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
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    @Nested
    class GetMyCardsTests {
        @BeforeEach
        void setUp() {
            // ВАЖНО: передать все 5 аргументов конструктора
            cardService = new CardService(
                    cardRepository,
                    ownerRepository,
                    cardMapper,
                    "400000",   // defaultBin
                    "USD"       // defaultCurrency (строкой, enum внутри разберётся)
            );
        }

        @Test
        void getMyCards_shouldReturnPagedDtos() {
            Long ownerId = 42L;
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

            // given: сущности из БД
            Card card1 = new Card();
            card1.setId(1L);
            card1.setPanLast4("1234");
            card1.setExpiryMonth((short) 10);
            card1.setExpiryYear((short) 2030);
            card1.setStatus(CardStatus.ACTIVE);
            card1.setBalance(BigDecimal.ZERO);
            card1.setCurrency(Currency.USD);
            card1.setCreatedAt(LocalDateTime.now()); // LocalDateTime, не Instant

            Card card2 = new Card();
            card2.setId(2L);
            card2.setPanLast4("5678");
            card2.setExpiryMonth((short) 11);
            card2.setExpiryYear((short) 2031);
            card2.setStatus(CardStatus.BLOCKED);
            card2.setBalance(new BigDecimal("100.00"));
            card2.setCurrency(Currency.USD);
            card2.setCreatedAt(LocalDateTime.now()); // LocalDateTime, не Instant

            Page<Card> page = new PageImpl<>(List.of(card1, card2), pageable, 2);
            when(cardRepository.findAllByOwnerId(ownerId, pageable)).thenReturn(page);

            // and: маппинг в DTO
            CardResponseDTO dto1 = new CardResponseDTO();
            dto1.setId(1L);
            dto1.setMaskedPan("**** **** **** 1234");
            dto1.setExpiryMonth((short) 10);
            dto1.setExpiryYear((short) 2030);
            dto1.setStatus(CardStatus.ACTIVE);
            dto1.setBalance(BigDecimal.ZERO);
            dto1.setCurrency(Currency.USD);

            CardResponseDTO dto2 = new CardResponseDTO();
            dto2.setId(2L);
            dto2.setMaskedPan("**** **** **** 5678");
            dto2.setExpiryMonth((short) 11);
            dto2.setExpiryYear((short) 2031);
            dto2.setStatus(CardStatus.BLOCKED);
            dto2.setBalance(new BigDecimal("100.00"));
            dto2.setCurrency(Currency.USD);

            when(cardMapper.toResponse(card1)).thenReturn(dto1);
            when(cardMapper.toResponse(card2)).thenReturn(dto2);

            // when
            Page<CardResponseDTO> result = cardService.getMyCards(ownerId, pageable);

            // then
            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            assertEquals(2, result.getContent().size());
            assertEquals(1L, result.getContent().get(0).getId());
            assertEquals("**** **** **** 1234", result.getContent().get(0).getMaskedPan());
            assertEquals(CardStatus.BLOCKED, result.getContent().get(1).getStatus());
            assertEquals(new BigDecimal("100.00"), result.getContent().get(1).getBalance());

            verify(cardRepository).findAllByOwnerId(ownerId, pageable);
            verify(cardMapper, times(1)).toResponse(card1);
            verify(cardMapper, times(1)).toResponse(card2);
            verifyNoMoreInteractions(cardRepository, cardMapper);
        }
    }

    @Nested
    class GetMyCardByIdTests {

        @Test
        void getMyCardById_shouldReturnDto_whenOwned() {
            Long ownerId = 10L;
            Long cardId  = 111L;

            Card card = new Card();
            card.setId(cardId);
            card.setPan("stub");         // pan_encrypted через конвертер, поле не null
            card.setPanLast4("1234");
            card.setBin("400000");       // bin обязателен по entity
            card.setExpiryMonth((short) 10);
            card.setExpiryYear((short) 2030);
            card.setStatus(CardStatus.ACTIVE);
            card.setBalance(BigDecimal.ZERO);
            card.setCurrency(Currency.USD);

            when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.of(card));

            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(cardId);
            dto.setMaskedPan("**** **** **** 1234");
            dto.setExpiryMonth((short) 10);
            dto.setExpiryYear((short) 2030);
            dto.setStatus(CardStatus.ACTIVE);
            dto.setBalance(BigDecimal.ZERO);
            dto.setCurrency(Currency.USD);

            when(cardMapper.toResponse(card)).thenReturn(dto);

            CardResponseDTO result = cardService.getMyCardById(ownerId, cardId);

            assertNotNull(result);
            assertEquals(cardId, result.getId());
            assertEquals("**** **** **** 1234", result.getMaskedPan());
            assertEquals(CardStatus.ACTIVE, result.getStatus());

            verify(cardRepository).findByIdAndOwnerId(cardId, ownerId);
            verify(cardMapper).toResponse(card);
            verifyNoMoreInteractions(cardRepository, cardMapper);
        }

        @Test
        void getMyCardById_shouldThrowEntityNotFound_whenNotOwnedOrMissing() {
            Long ownerId = 10L;
            Long cardId  = 222L;

            when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(
                    EntityNotFoundException.class,
                    () -> cardService.getMyCardById(ownerId, cardId)
            );
            assertTrue(ex.getMessage().contains("Card not found"));

            verify(cardRepository).findByIdAndOwnerId(cardId, ownerId);
            verifyNoInteractions(cardMapper);
        }
    }
}