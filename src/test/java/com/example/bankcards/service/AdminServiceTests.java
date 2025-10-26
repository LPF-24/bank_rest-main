package com.example.bankcards.service;

import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTests {
    private static final Long PERSON_ID = 1L;
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    private static final String EMAIL = "john@gmail.com";

    @Mock
    private OwnerRepository ownerRepository;

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
}
