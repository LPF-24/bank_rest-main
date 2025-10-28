package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateRequestDTO;
import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.dto.TransferResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Currency;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.util.PanGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final OwnerRepository ownerRepository;
    private final CardMapper cardMapper;
    private final Currency defaultCurrency;

    private final String defaultBin;

    public CardService(
            CardRepository cardRepository,
            OwnerRepository ownerRepository,
            CardMapper cardMapper,
            @Value("${card.bin:400000}") String defaultBin,
            @Value("${card.currency:USD}") String defaultCurrency) {
        this.cardRepository = cardRepository;
        this.ownerRepository = ownerRepository;
        this.cardMapper = cardMapper;
        this.defaultBin = defaultBin;
        this.defaultCurrency = Currency.valueOf(defaultCurrency); // если enum
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public CardResponseDTO createCard(CardCreateRequestDTO dto) {
        Owner owner = ownerRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Owner with id " + dto.getOwnerId() + " not found"));

        // Генерируем PAN и derived поля
        String pan = PanGenerator.generatePan(defaultBin);
        String panLast4 = pan.substring(pan.length() - 4);
        String bin = pan.substring(0, 6);

        // Срок действия: текущий месяц + 4 года
        LocalDate now = LocalDate.now();
        short expiryMonth = (short) now.getMonthValue();
        short expiryYear = (short) (now.getYear() + 4);

        Card card = new Card();
        card.setOwner(owner);
        card.setPan(pan);
        card.setPanLast4(panLast4);
        card.setBin(bin);
        card.setExpiryMonth(expiryMonth);
        card.setExpiryYear(expiryYear);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(BigDecimal.ZERO);
        card.setCurrency(defaultCurrency);

        Card saved = cardRepository.save(card);
        return cardMapper.toResponse(saved);
    }

    @PreAuthorize("isAuthenticated()")
    public Page<CardResponseDTO> getMyCards(Long ownerId, Pageable pageable) {
        return cardRepository.findAllByOwnerId(ownerId, pageable)
                .map(cardMapper::toResponse);
    }

    @PreAuthorize("isAuthenticated()")
    public CardResponseDTO getMyCardById(Long ownerId, Long cardId) {
        Card card = cardRepository.findByIdAndOwnerId(cardId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));
        return cardMapper.toResponse(card);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public CardResponseDTO depositMyCard(Long ownerId, Long cardId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        Card card = cardRepository.findByIdAndOwnerId(cardId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Card is blocked");
        }
        card.setBalance(card.getBalance().add(amount));
        Card saved = cardRepository.save(card);
        return cardMapper.toResponse(saved);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public CardResponseDTO withdrawMyCard(Long ownerId, Long cardId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        Card card = cardRepository.findByIdAndOwnerId(cardId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Card is blocked");
        }
        if (card.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds");
        }
        card.setBalance(card.getBalance().subtract(amount));
        Card saved = cardRepository.save(card);
        return cardMapper.toResponse(saved);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public TransferResponseDTO transferBetweenMyCards(Long ownerId, Long fromId, Long toId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        if (Objects.equals(fromId, toId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and destination cards must differ");
        }

        Card from = cardRepository.findByIdAndOwnerId(fromId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Source card not found"));
        Card to = cardRepository.findByIdAndOwnerId(toId, ownerId
        ).orElseThrow(() -> new EntityNotFoundException("Destination card not found"));

        // Валидируем статусы
        if (from.getStatus() == CardStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Source card is blocked");
        }
        if (to.getStatus() == CardStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destination card is blocked");
        }

        // (опционально) проверка валют
        if (from.getCurrency() != to.getCurrency()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Currency mismatch");
        }

        // Достаточно ли средств
        if (from.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds");
        }

        // Денежные операции
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        // Сохраняем обе — в одной транзакции
        Card savedFrom = cardRepository.save(from);
        Card savedTo   = cardRepository.save(to);

        TransferResponseDTO resp = new TransferResponseDTO();
        resp.setFrom(cardMapper.toResponse(savedFrom));
        resp.setTo(cardMapper.toResponse(savedTo));
        return resp;
    }
}
