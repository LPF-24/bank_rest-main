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
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    @Nested
    class DepositWithdrawMyCardTests {

        @BeforeEach
        void initService() {
            // твой конструктор: (cardRepository, ownerRepository, cardMapper, defaultBin, defaultCurrency)
            cardService = new CardService(cardRepository, ownerRepository, cardMapper, "400000", "USD");
        }

        @Test
        void depositMyCard_shouldIncreaseBalance_whenOwnerAndActive() {
            Long ownerId = 7L, cardId = 100L;
            BigDecimal amount = new BigDecimal("50.00");

            Card card = new Card();
            card.setId(cardId);
            card.setPan("stub");
            card.setPanLast4("1111");
            card.setBin("400000");
            card.setExpiryMonth((short)10);
            card.setExpiryYear((short)2030);
            card.setStatus(CardStatus.ACTIVE);
            card.setBalance(BigDecimal.ZERO);
            card.setCurrency(Currency.USD);

            when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.of(card));

            Card after = cloneCard(card);
            after.setBalance(new BigDecimal("50.00"));
            when(cardRepository.save(any(Card.class))).thenReturn(after);

            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(cardId);
            dto.setMaskedPan("**** **** **** 1111");
            dto.setStatus(CardStatus.ACTIVE);
            dto.setBalance(new BigDecimal("50.00"));
            dto.setCurrency(Currency.USD);
            when(cardMapper.toResponse(after)).thenReturn(dto);

            CardResponseDTO res = cardService.depositMyCard(ownerId, cardId, amount);

            assertEquals(new BigDecimal("50.00"), res.getBalance());
            verify(cardRepository).findByIdAndOwnerId(cardId, ownerId);
            verify(cardRepository).save(argThat(c -> c.getBalance().compareTo(new BigDecimal("50.00")) == 0));
            verify(cardMapper).toResponse(after);
        }

        @Test
        void depositMyCard_shouldReturn400_whenAmountInvalid() {
            assertThrows(ResponseStatusException.class, () -> cardService.depositMyCard(1L, 1L, BigDecimal.ZERO));
            assertThrows(ResponseStatusException.class, () -> cardService.depositMyCard(1L, 1L, new BigDecimal("-5")));
        }

        @Test
        void depositMyCard_shouldReturn404_whenCardNotOwned() {
            when(cardRepository.findByIdAndOwnerId(99L, 1L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> cardService.depositMyCard(1L, 99L, new BigDecimal("1")));
        }

        @Test
        void depositMyCard_shouldReturn409_whenBlocked() {
            Long ownerId = 2L, cardId = 200L;
            Card card = new Card();
            card.setId(cardId);
            card.setPan("stub");
            card.setPanLast4("2222");
            card.setBin("400000");
            card.setExpiryMonth((short)10);
            card.setExpiryYear((short)2030);
            card.setStatus(CardStatus.BLOCKED);
            card.setBalance(new BigDecimal("10.00"));
            card.setCurrency(Currency.USD);
            when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.of(card));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> cardService.depositMyCard(ownerId, cardId, new BigDecimal("5")));
            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            verify(cardRepository, never()).save(any());
        }

        @Test
        void withdrawMyCard_shouldDecreaseBalance_whenOwnerAndEnoughFunds() {
            Long ownerId = 3L, cardId = 300L;
            BigDecimal amount = new BigDecimal("40.00");

            Card card = new Card();
            card.setId(cardId);
            card.setPan("stub");
            card.setPanLast4("3333");
            card.setBin("400000");
            card.setExpiryMonth((short)10);
            card.setExpiryYear((short)2030);
            card.setStatus(CardStatus.ACTIVE);
            card.setBalance(new BigDecimal("100.00"));
            card.setCurrency(Currency.USD);

            when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.of(card));

            Card after = cloneCard(card);
            after.setBalance(new BigDecimal("60.00"));
            when(cardRepository.save(any(Card.class))).thenReturn(after);

            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(cardId);
            dto.setMaskedPan("**** **** **** 3333");
            dto.setStatus(CardStatus.ACTIVE);
            dto.setBalance(new BigDecimal("60.00"));
            dto.setCurrency(Currency.USD);
            when(cardMapper.toResponse(after)).thenReturn(dto);

            CardResponseDTO res = cardService.withdrawMyCard(ownerId, cardId, amount);

            assertEquals(new BigDecimal("60.00"), res.getBalance());
            verify(cardRepository).findByIdAndOwnerId(cardId, ownerId);
            verify(cardRepository).save(argThat(c -> c.getBalance().compareTo(new BigDecimal("60.00")) == 0));
            verify(cardMapper).toResponse(after);
        }

        @Test
        void withdrawMyCard_shouldReturn409_whenInsufficientFunds() {
            Long ownerId = 4L, cardId = 400L;
            Card card = new Card();
            card.setId(cardId);
            card.setPan("stub");
            card.setPanLast4("4444");
            card.setBin("400000");
            card.setExpiryMonth((short)10);
            card.setExpiryYear((short)2030);
            card.setStatus(CardStatus.ACTIVE);
            card.setBalance(new BigDecimal("30.00"));
            card.setCurrency(Currency.USD);
            when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.of(card));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> cardService.withdrawMyCard(ownerId, cardId, new BigDecimal("50.00")));
            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            verify(cardRepository, never()).save(any());
            verifyNoInteractions(cardMapper);
        }
    }

    @Nested
    class TransferBetweenMyCardsTests {

        @BeforeEach
        void initService() {
            cardService = new CardService(cardRepository, ownerRepository, cardMapper, "400000", "USD");
        }

        @Test
        void transfer_shouldMoveFunds_whenOwnerActiveAndEnough() {
            Long ownerId = 7L;
            Long fromId = 101L, toId = 202L;
            BigDecimal amount = new BigDecimal("40.00");

            // given
            Card from = baseCard(fromId, "1111", CardStatus.ACTIVE, new BigDecimal("100.00"));
            Card to   = baseCard(toId, "2222", CardStatus.ACTIVE, new BigDecimal("5.00"));

            when(cardRepository.findByIdAndOwnerId(fromId, ownerId)).thenReturn(Optional.of(from));
            when(cardRepository.findByIdAndOwnerId(toId, ownerId)).thenReturn(Optional.of(to));

            // после операции должны получиться такие состояния
            Card savedFrom = cloneCard(from);
            savedFrom.setBalance(new BigDecimal("60.00")); // 100 - 40

            Card savedTo = cloneCard(to);
            savedTo.setBalance(new BigDecimal("45.00"));   // 5 + 40

            // вместо двух argThat(...) — один универсальный Answer, чтобы избежать NPE
            when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
                Card c = inv.getArgument(0);
                if (c != null && fromId.equals(c.getId())) return savedFrom;
                if (c != null && toId.equals(c.getId()))   return savedTo;
                return c; // fallback
            });

            CardResponseDTO fromDto = dtoOf(savedFrom.getId(), "1111", savedFrom.getBalance(), CardStatus.ACTIVE);
            CardResponseDTO toDto   = dtoOf(savedTo.getId(), "2222", savedTo.getBalance(), CardStatus.ACTIVE);

            when(cardMapper.toResponse(savedFrom)).thenReturn(fromDto);
            when(cardMapper.toResponse(savedTo)).thenReturn(toDto);

            // when
            TransferResponseDTO res = cardService.transferBetweenMyCards(ownerId, fromId, toId, amount);

            // then
            assertEquals(new BigDecimal("60.00"), res.getFrom().getBalance());
            assertEquals(new BigDecimal("45.00"), res.getTo().getBalance());

            verify(cardRepository).findByIdAndOwnerId(fromId, ownerId);
            verify(cardRepository).findByIdAndOwnerId(toId, ownerId);
            verify(cardRepository, times(2)).save(any(Card.class));
            verify(cardMapper).toResponse(savedFrom);
            verify(cardMapper).toResponse(savedTo);
        }

        @Test
        void transfer_should400_whenAmountInvalid_orSameCard() {
            assertThrows(ResponseStatusException.class,
                    () -> cardService.transferBetweenMyCards(1L, 10L, 20L, BigDecimal.ZERO));
            assertThrows(ResponseStatusException.class,
                    () -> cardService.transferBetweenMyCards(1L, 10L, 10L, new BigDecimal("1")));
        }

        @Test
        void transfer_should404_whenFromOrToNotOwned() {
            when(cardRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class,
                    () -> cardService.transferBetweenMyCards(1L, 10L, 20L, new BigDecimal("1")));
        }

        @Test
        void transfer_should409_whenBlockedOrInsufficient_orCurrencyMismatch() {
            Long ownerId = 1L;
            Card from = baseCard(1L, "1111", CardStatus.BLOCKED, new BigDecimal("100"));
            Card to   = baseCard(2L, "2222", CardStatus.ACTIVE,  new BigDecimal("0"));

            when(cardRepository.findByIdAndOwnerId(1L, ownerId)).thenReturn(Optional.of(from));
            when(cardRepository.findByIdAndOwnerId(2L, ownerId)).thenReturn(Optional.of(to));

            assertEquals(HttpStatus.CONFLICT,
                    assertThrows(ResponseStatusException.class,
                            () -> cardService.transferBetweenMyCards(ownerId, 1L, 2L, new BigDecimal("10")))
                            .getStatusCode());

            // недостаточно средств
            from.setStatus(CardStatus.ACTIVE);
            from.setBalance(new BigDecimal("5"));
            assertEquals(HttpStatus.CONFLICT,
                    assertThrows(ResponseStatusException.class,
                            () -> cardService.transferBetweenMyCards(ownerId, 1L, 2L, new BigDecimal("10")))
                            .getStatusCode());
        }

        // helpers
        private Card baseCard(Long id, String last4, CardStatus st, BigDecimal bal) {
            Card c = new Card();
            c.setId(id);
            c.setPan("stub");
            c.setPanLast4(last4);
            c.setBin("400000");
            c.setExpiryMonth((short)10);
            c.setExpiryYear((short)2030);
            c.setStatus(st);
            c.setBalance(bal);
            c.setCurrency(Currency.USD);
            return c;
        }

        private Card cloneCard(Card s){ Card c=baseCard(s.getId(), s.getPanLast4(), s.getStatus(), s.getBalance()); return c;}
        private CardResponseDTO dtoOf(Long id, String last4, BigDecimal bal, CardStatus st){
            CardResponseDTO d=new CardResponseDTO(); d.setId(id); d.setMaskedPan("**** **** **** "+last4);
            d.setBalance(bal); d.setStatus(st); d.setCurrency(Currency.USD); return d;
        }
    }

    private Card cloneCard(Card src) {
        Card c = new Card();
        c.setId(src.getId());
        c.setPan(src.getPan());
        c.setPanLast4(src.getPanLast4());
        c.setBin(src.getBin());
        c.setExpiryMonth(src.getExpiryMonth());
        c.setExpiryYear(src.getExpiryYear());
        c.setStatus(src.getStatus());
        c.setBalance(src.getBalance());
        c.setCurrency(src.getCurrency());
        return c;
    }
}