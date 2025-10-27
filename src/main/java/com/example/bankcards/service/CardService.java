package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateRequestDTO;
import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Currency;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.util.PanGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
