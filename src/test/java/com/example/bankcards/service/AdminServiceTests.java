package com.example.bankcards.service;

import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import com.example.bankcards.mapper.OwnerMapper;
import com.example.bankcards.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
            adminService = new AdminService(ownerRepository, ownerMapper);
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
