package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.dto.OwnerAdminUpdateDTO;
import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.entity.*;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.mapper.OwnerMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTests {
    private static final Long PERSON_ID = 1L;
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    private static final String EMAIL = "john@gmail.com";

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private OwnerMapper ownerMapper;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private AdminService adminService;

    @Nested
    class PromotePersonTests {

        @Test
        void shouldPromotePersonToAdmin() {
            Owner owner = createSampleOwner();

            when(ownerRepository.findById(PERSON_ID)).thenReturn(Optional.of(owner));

            adminService.promotePerson(PERSON_ID);

            verify(ownerRepository).save(owner);
            assertEquals(Role.ADMIN, owner.getRole());
        }

        @Test
        void shouldThrowException_whenUserNotFound() {
            Long userId = 99L;

            when(ownerRepository.findById(userId)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> adminService.promotePerson(userId));
        }
    }

    @Nested
    class FindAllUsersTests {
        @BeforeEach
        void setUp() {
            adminService = new AdminService(ownerRepository, ownerMapper, cardRepository, cardMapper);
        }

        @Test
        void shouldGetAllUsersInfo() {
            Owner owner = createSampleOwner();
            OwnerResponseDTO responseDTO = createSampleResponseDTO();

            when(ownerRepository.findAll()).thenReturn(List.of(owner));
            when(ownerMapper.toResponse(owner)).thenReturn(responseDTO);

            List<OwnerResponseDTO> result = adminService.findAllUsers();

            assertEquals(responseDTO.getEmail(), result.getFirst().getEmail());
            assertEquals(responseDTO.getId(), result.getFirst().getId());
            assertEquals(EMAIL, result.getFirst().getEmail());
            assertEquals(Role.USER, owner.getRole());
            assertEquals(PERSON_ID, result.getFirst().getId());
            assertEquals(1, result.size());
            assertEquals(FIRST_NAME, result.getFirst().getFirstName());

            verify(ownerRepository).findAll();
        }

        @Test
        void shouldReturnEmptyList_whenNoUsers() {
            when(ownerRepository.findAll()).thenReturn(List.of());

            List<OwnerResponseDTO> result = adminService.findAllUsers();

            assertEquals(0, result.size());

            verify(ownerRepository).findAll();
        }
    }

    @Nested
    class BlockCustomerTests {
        @Test
        void blockCustomer_shouldSetLockedTrue_andSave() {
            Owner o = new Owner();
            o.setId(1L);
            o.setLocked(false);

            when(ownerRepository.findById(1L)).thenReturn(Optional.of(o));
            when(ownerRepository.save(any(Owner.class))).thenAnswer(inv -> inv.getArgument(0));

            adminService.blockCustomer(1L);

            assertTrue(o.isLocked());
            verify(ownerRepository).save(o);
        }

        @Test
        void blockCustomer_shouldThrow_ifOwnerNotFound() {
            when(ownerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> adminService.blockCustomer(99L));
            verify(ownerRepository, never()).save(any());
        }
    }

    @Nested
    class UnlockCustomerTests {
        @Test
        void unlockCustomer_shouldSetLockedFalse_andSave_whenLocked() {
            Owner o = new Owner();
            o.setId(1L);
            o.setLocked(true);

            when(ownerRepository.findById(1L)).thenReturn(Optional.of(o));
            when(ownerRepository.save(any(Owner.class))).thenAnswer(inv -> inv.getArgument(0));

            adminService.unlockCustomer(1L);

            assertFalse(o.isLocked());
            verify(ownerRepository).save(o);
        }

        @Test
        void unlockCustomer_shouldDoNothing_whenAlreadyUnlocked() {
            Owner o = new Owner();
            o.setId(1L);
            o.setLocked(false);

            when(ownerRepository.findById(1L)).thenReturn(Optional.of(o));

            adminService.unlockCustomer(1L);

            assertFalse(o.isLocked());
            verify(ownerRepository, never()).save(any());
        }

        @Test
        void unlockCustomer_shouldThrow_whenOwnerNotFound() {
            when(ownerRepository.findById(42L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> adminService.unlockCustomer(42L));
            verify(ownerRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateCustomerDataByAdminTests {

        @Test
        void shouldUpdateCustomerFields() {
            Owner owner = new Owner();
            owner.setId(1L);
            owner.setFirstName("Old");
            owner.setLastName("Name");
            owner.setDateOfBirth(LocalDate.of(1990, 1, 1));

            OwnerResponseDTO mappedResponse = new OwnerResponseDTO();
            mappedResponse.setId(1L);
            mappedResponse.setFirstName("NewName");
            mappedResponse.setLastName("UpdatedLast");
            mappedResponse.setDateOfBirth(LocalDate.of(1995, 2, 15));

            when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(ownerRepository.save(any())).thenReturn(owner);
            when(ownerMapper.toResponse(owner)).thenReturn(mappedResponse);

            OwnerAdminUpdateDTO dto = new OwnerAdminUpdateDTO();
            dto.setFirstName("NewName");
            dto.setLastName("UpdatedLast");
            dto.setDateOfBirth(LocalDate.of(1995, 2, 15));

            OwnerResponseDTO response = adminService.updateCustomerDataByAdmin(1L, dto);

            assertNotNull(response);
            assertEquals("NewName", response.getFirstName());
            assertEquals("UpdatedLast", response.getLastName());
            assertEquals(LocalDate.of(1995, 2, 15), response.getDateOfBirth());

            verify(ownerRepository).save(owner);
        }

        @Test
        void shouldThrowException_whenCustomerNotFound() {
            when(ownerRepository.findById(99L)).thenReturn(Optional.empty());
            OwnerAdminUpdateDTO dto = new OwnerAdminUpdateDTO();
            dto.setFirstName("Name");

            assertThrows(EntityNotFoundException.class,
                    () -> adminService.updateCustomerDataByAdmin(99L, dto));
        }

        @Test
        void shouldThrowBadRequest_whenNothingToUpdate() {
            Owner owner = new Owner();
            owner.setId(1L);
            when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));

            OwnerAdminUpdateDTO dto = new OwnerAdminUpdateDTO(); // все поля null

            assertThrows(ResponseStatusException.class,
                    () -> adminService.updateCustomerDataByAdmin(1L, dto));
            verify(ownerRepository, never()).save(any());
        }
    }

    @Nested
    class AdminBlockUnblockTests {

        @Test
        void adminBlockCard_shouldSetBlocked_whenActive() {
            Long cardId = 100L;

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

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

            Card blocked = cloneCard(card);
            blocked.setStatus(CardStatus.BLOCKED);
            when(cardRepository.save(any(Card.class))).thenReturn(blocked);

            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(cardId);
            dto.setMaskedPan("**** **** **** 1111");
            dto.setStatus(CardStatus.BLOCKED);
            when(cardMapper.toResponse(blocked)).thenReturn(dto);

            CardResponseDTO result = adminService.adminBlockCard(cardId);

            assertEquals(CardStatus.BLOCKED, result.getStatus());
            verify(cardRepository).findById(cardId);
            verify(cardRepository).save(argThat(c -> c.getStatus() == CardStatus.BLOCKED));
            verify(cardMapper).toResponse(blocked);
        }

        @Test
        void adminBlockCard_shouldBeIdempotent_whenAlreadyBlocked() {
            Long cardId = 101L;

            Card card = new Card();
            card.setId(cardId);
            card.setPan("stub");
            card.setPanLast4("2222");
            card.setBin("400000");
            card.setExpiryMonth((short)10);
            card.setExpiryYear((short)2030);
            card.setStatus(CardStatus.BLOCKED);
            card.setBalance(BigDecimal.ZERO);
            card.setCurrency(Currency.USD);

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(cardId);
            dto.setMaskedPan("**** **** **** 2222");
            dto.setStatus(CardStatus.BLOCKED);
            when(cardMapper.toResponse(card)).thenReturn(dto);

            CardResponseDTO result = adminService.adminBlockCard(cardId);

            assertEquals(CardStatus.BLOCKED, result.getStatus());
            verify(cardRepository).findById(cardId);
            verify(cardRepository, never()).save(any());
            verify(cardMapper).toResponse(card);
        }

        @Test
        void adminUnblockCard_shouldSetActive_whenBlocked() {
            Long cardId = 102L;

            Card card = new Card();
            card.setId(cardId);
            card.setPan("stub");
            card.setPanLast4("3333");
            card.setBin("400000");
            card.setExpiryMonth((short)10);
            card.setExpiryYear((short)2030);
            card.setStatus(CardStatus.BLOCKED);
            card.setBalance(BigDecimal.ZERO);
            card.setCurrency(Currency.USD);

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

            Card active = cloneCard(card);
            active.setStatus(CardStatus.ACTIVE);
            when(cardRepository.save(any(Card.class))).thenReturn(active);

            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(cardId);
            dto.setMaskedPan("**** **** **** 3333");
            dto.setStatus(CardStatus.ACTIVE);
            when(cardMapper.toResponse(active)).thenReturn(dto);

            CardResponseDTO result = adminService.adminUnblockCard(cardId);

            assertEquals(CardStatus.ACTIVE, result.getStatus());
            verify(cardRepository).findById(cardId);
            verify(cardRepository).save(argThat(c -> c.getStatus() == CardStatus.ACTIVE));
            verify(cardMapper).toResponse(active);
        }

        @Test
        void adminUnblockCard_shouldBeIdempotent_whenAlreadyActive() {
            Long cardId = 103L;

            Card card = new Card();
            card.setId(cardId);
            card.setPan("stub");
            card.setPanLast4("4444");
            card.setBin("400000");
            card.setExpiryMonth((short)10);
            card.setExpiryYear((short)2030);
            card.setStatus(CardStatus.ACTIVE);
            card.setBalance(BigDecimal.ZERO);
            card.setCurrency(Currency.USD);

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(cardId);
            dto.setMaskedPan("**** **** **** 4444");
            dto.setStatus(CardStatus.ACTIVE);
            when(cardMapper.toResponse(card)).thenReturn(dto);

            CardResponseDTO result = adminService.adminUnblockCard(cardId);

            assertEquals(CardStatus.ACTIVE, result.getStatus());
            verify(cardRepository).findById(cardId);
            verify(cardRepository, never()).save(any());
            verify(cardMapper).toResponse(card);
        }

        @Test
        void adminBlockCard_shouldThrow404_whenNotFound() {
            when(cardRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> adminService.adminBlockCard(999L));
        }

        @Test
        void adminUnblockCard_shouldThrow404_whenNotFound() {
            when(cardRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> adminService.adminUnblockCard(999L));
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

    private static Owner createSampleOwner() {
        Owner owner = new Owner();
        owner.setId(PERSON_ID);
        owner.setFirstName(FIRST_NAME);
        owner.setLastName(LAST_NAME);
        owner.setDateOfBirth(LocalDate.of(2002, 3, 14));
        owner.setEmail(EMAIL);
        owner.setPassword("secret");
        owner.setPhone("+3-345-12-12");
        return owner;
    }

    private static OwnerResponseDTO createSampleResponseDTO() {
        OwnerResponseDTO response = new OwnerResponseDTO();
        response.setId(PERSON_ID);
        response.setFirstName(FIRST_NAME);
        response.setLastName(LAST_NAME);
        response.setEmail(EMAIL);
        response.setRole(Role.USER);
        return response;
    }
}
