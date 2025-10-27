package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateRequestDTO;
import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OwnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTests {

    @Mock private CardRepository cardRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private CardMapper cardMapper;

    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardService = new CardService(
                cardRepository,
                ownerRepository,
                cardMapper,
                "400000",
                "USD"
        );
    }

    @Test
    void createCard_shouldGeneratePanAndPersist() {
        Owner owner = new Owner();
        owner.setId(10L);

        when(ownerRepository.findById(10L)).thenReturn(Optional.of(owner));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cardMapper.toResponse(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(c.getId());
            dto.setOwnerId(c.getOwner().getId());
            dto.setMaskedPan("**** **** **** " + c.getPanLast4());
            dto.setPanLast4(c.getPanLast4());
            dto.setExpiryMonth(c.getExpiryMonth());
            dto.setExpiryYear(c.getExpiryYear());
            dto.setStatus(c.getStatus());
            dto.setBalance(c.getBalance());
            dto.setCurrency(c.getCurrency());
            return dto;
        });

        CardCreateRequestDTO dto = new CardCreateRequestDTO();
        dto.setOwnerId(10L);

        CardResponseDTO resp = cardService.createCard(dto);

        assertNotNull(resp);
        assertEquals(10L, resp.getOwnerId());
        assertEquals(CardStatus.ACTIVE, resp.getStatus());
        assertEquals(BigDecimal.ZERO, resp.getBalance());

        LocalDate now = LocalDate.now();
        assertEquals((short) now.getMonthValue(), resp.getExpiryMonth());
        assertEquals((short) (now.getYear() + 4), resp.getExpiryYear());

        verify(cardRepository).save(any(Card.class));
    }
}