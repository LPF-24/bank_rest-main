package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.mapper.OwnerMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import com.example.bankcards.dto.OwnerAdminUpdateDTO;

import java.util.List;
import java.util.stream.Stream;

@Service
public class AdminService {
    private final OwnerRepository ownerRepository;
    private final OwnerMapper ownerMapper;
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    public AdminService(OwnerRepository ownerRepository, OwnerMapper ownerMapper, CardRepository cardRepository, CardMapper cardMapper) {
        this.ownerRepository = ownerRepository;
        this.ownerMapper = ownerMapper;
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
    }

    @Transactional
    public void promotePerson(Long personId) {
        Owner owner = ownerRepository.findById(personId)
                .orElseThrow(() -> new EntityNotFoundException("Customer with this id " + personId + " can't be found"));

        owner.setRole(Role.ADMIN);
        ownerRepository.save(owner);
    }

    @Transactional(readOnly = true)
    public List<OwnerResponseDTO> findAllUsers() {
        return ownerRepository.findAll().stream()
                .filter(person -> Role.USER.equals(person.getRole()))
                .map(ownerMapper::toResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public void blockCustomer(Long customerId) {
        Owner ownerToLock = ownerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer with ID " + customerId + " wasn't found!"));

        ownerToLock.setLocked(true);

        ownerRepository.save(ownerToLock);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public void unlockCustomer(Long ownerId) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner with id " + ownerId + " not found"));
        if (owner.isLocked()) {
            owner.setLocked(false);
            ownerRepository.save(owner);
        }
        // идемпотентно: если уже разблокирован — просто ничего не делаем
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public OwnerResponseDTO updateCustomerDataByAdmin(Long ownerId, OwnerAdminUpdateDTO dto) {
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Owner with ID " + ownerId + " wasn't found!"));

        boolean allFieldsEmpty = Stream.of(
                dto.getFirstName(),
                dto.getLastName(),
                dto.getDateOfBirth()
        ).allMatch(value -> value == null || (value instanceof String && ((String) value).isBlank()));

        if (allFieldsEmpty) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing to update");
        }

        if (StringUtils.hasText(dto.getFirstName())) {
            owner.setFirstName(dto.getFirstName());
        }
        if (StringUtils.hasText(dto.getLastName())) {
            owner.setLastName(dto.getLastName());
        }
        if (dto.getDateOfBirth() != null) {
            owner.setDateOfBirth(dto.getDateOfBirth());
        }

        Owner updated = ownerRepository.save(owner);
        return ownerMapper.toResponse(updated);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public CardResponseDTO adminBlockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));
        if (card.getStatus() != CardStatus.BLOCKED) {
            card.setStatus(CardStatus.BLOCKED);
            card = cardRepository.save(card);
        }
        return cardMapper.toResponse(card);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public CardResponseDTO adminUnblockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found"));
        if (card.getStatus() == CardStatus.BLOCKED) {
            card.setStatus(CardStatus.ACTIVE);
            card = cardRepository.save(card);
        }
        return cardMapper.toResponse(card);
    }
}
