package com.example.bankcards.service;

import com.example.bankcards.dto.OwnerRequestDTO;
import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.dto.OwnerUpdateDTO;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import com.example.bankcards.mapper.OwnerMapper;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.security.OwnerDetails;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OwnerServiceTests {
    @Mock
    private OwnerRepository ownerRepository;

    private OwnerMapper ownerMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OwnerService ownerService;

    Owner owner;
    OwnerDetails ownerDetails;

    @BeforeEach
    void setUp() {
        ModelMapper modelMapper = new ModelMapper();
        this.ownerMapper = new OwnerMapper(modelMapper, passwordEncoder);
        this.ownerService = new OwnerService(ownerRepository, ownerMapper, passwordEncoder);
    }

    @Test
    void saveOwner_shouldSaveAndReturnDTO() {
        // Arrange
        OwnerRequestDTO dto = createSampleDTO();

        // Мокаем поведение passwordEncoder
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        // Гарантируем, что объект, переданный в save(...), получит id = 1L
        when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> {
            Owner p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // Act
        OwnerResponseDTO result = ownerService.savePerson(dto);

        System.out.println("Result DTO ID: " + result.getId());

        // Assert
        assertEquals(1L, result.getId());
        assertEquals("John", result.getFirstName());
        assertEquals(LocalDate.of(2002, 3, 14), result.getDateOfBirth());
        assertEquals("john@gmail.com", result.getEmail());

        verify(passwordEncoder).encode("secret");
        verify(ownerRepository).save(any(Owner.class));
    }

    @Test
    void modelMapper_shouldMapId() {
        ModelMapper modelMapper = new ModelMapper();
        ownerMapper = new OwnerMapper(modelMapper, passwordEncoder);

        owner = createSampleOwner();

        OwnerResponseDTO dto = ownerMapper.toResponse(owner);

        assertEquals(1L, dto.getId());
        assertEquals("John", dto.getFirstName());
        assertEquals("john@gmail.com", dto.getEmail());
    }

    @Test
    void saveOwner_shouldEncodePassword() {
        OwnerRequestDTO dto = createSampleDTO();
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        ownerService.savePerson(dto);
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void saveOwner_shouldAssignUserRoleByDefault() {
        OwnerRequestDTO dto = createSampleDTO();
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        OwnerResponseDTO result = ownerService.savePerson(dto);

        assertEquals(Role.USER, result.getRole());
    }

    @Test
    void getCurrentCustomerInfo_shouldReturnCorrectDTO() {
        // given
        Owner owner = createSampleOwner();
        OwnerDetails ownerDetails = new OwnerDetails(owner);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(ownerDetails);

        when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));

        // when
        OwnerResponseDTO result = ownerService.getCurrentCustomerInfo();

        // than
        assertEquals("John", result.getFirstName());
        verify(ownerRepository).findById(1L);
    }

    @Test
    void getCurrentCustomerInfo_shouldThrowException_whenCustomerNotFound() {
        Owner owner = createSampleOwner();
        OwnerDetails ownerDetails = new OwnerDetails(owner);
        SecurityContextHolder.setContext(securityContext);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(ownerDetails);

        when(ownerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ownerService.getCurrentCustomerInfo());
        verify(ownerRepository).findById(1L);
    }

    @Nested
    class UpdateCurrentCustomerDataTests {
        @BeforeEach
        void setUp() {
            SecurityContextHolder.setContext(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            owner = createSampleOwner();
            ownerDetails = new OwnerDetails(owner);
            when(authentication.getPrincipal()).thenReturn(ownerDetails);
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        void updateCurrentCustomerData_shouldReturnCorrectDTO() {
            // given
            owner.setPassword("encoded-secret");

            OwnerUpdateDTO updateDTO = new OwnerUpdateDTO();
            updateDTO.setPhone("+3-345-12-12");
            updateDTO.setEmail("john@gmail.com");
            updateDTO.setPassword("secret");

            when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

            // when
            when(ownerRepository.save(owner)).thenReturn(owner);

            OwnerResponseDTO result = ownerService.updateCurrentCustomerData(updateDTO);

            // then
            assertEquals("+3-345-12-12", result.getPhone());
            assertEquals("john@gmail.com", result.getEmail());
            verify(ownerRepository).findById(1L);
            verify(ownerRepository).save(owner);
        }

        @Test
        void updateCurrentCustomerData_shouldThrowBadRequestException_whenAllFieldsAreNull() {
            // given
            Owner owner = new Owner();
            owner.setId(1L);

            OwnerUpdateDTO dto = new OwnerUpdateDTO();

            // when
            when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));

            // than
            assertThrows(ResponseStatusException.class, () -> ownerService.updateCurrentCustomerData(dto));
        }

        @Test
        void updateCurrentCustomerData_shouldThrowResponseStatusException_whenAllFieldsAreEmpty() {
            Owner owner = createSampleOwner();
            OwnerDetails ownerDetails = new OwnerDetails(owner);

            // given
            OwnerUpdateDTO dto = new OwnerUpdateDTO();
            dto.setEmail("");
            dto.setPassword("");
            dto.setPhone("");

            // when
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(ownerDetails);
            when(ownerRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            // then
            assertThrows(ResponseStatusException.class, () -> ownerService.updateCurrentCustomerData(dto));
            verify(ownerRepository, never()).save(any());
        }

        @Test
        void updateCurrentCustomerData_shouldNotChangePassword_ifPasswordIsNull() {
            Owner owner = new Owner();
            owner.setId(1L);
            owner.setEmail("old@gmail.com");
            owner.setPhone("+100000000");
            owner.setPassword("original");

            OwnerDetails principal = new OwnerDetails(owner);

            when(authentication.getPrincipal()).thenReturn(principal);
            when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OwnerUpdateDTO dto = new OwnerUpdateDTO();
            dto.setEmail("new@gmail.com");
            dto.setPhone("+2000000");
            dto.setPassword(null);

            ownerService.updateCurrentCustomerData(dto);

            ArgumentCaptor<Owner> captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            Owner saved = captor.getValue();

            assertEquals("original", saved.getPassword());
            verify(passwordEncoder, never()).encode(anyString());

            assertEquals("new@gmail.com", saved.getEmail());
            assertEquals("+2000000", saved.getPhone());
        }

        @Test
        void updateCurrentCustomerData_shouldEncodePassword_whenPasswordProvided() {
            // given
            Owner owner = new Owner();
            owner.setId(1L);
            owner.setPassword("old-hash");

            OwnerDetails principal = new OwnerDetails(owner);

            when(authentication.getPrincipal()).thenReturn(principal);

            when(ownerRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(ownerRepository.save(any(Owner.class))).thenAnswer(invocation -> invocation.getArgument(0));

            when(passwordEncoder.encode("NewPass1!")).thenReturn("new-hash");

            OwnerUpdateDTO dto = new OwnerUpdateDTO();
            dto.setPassword("NewPass1!");

            // when
            ownerService.updateCurrentCustomerData(dto);

            // then
            ArgumentCaptor<Owner> captor = ArgumentCaptor.forClass(Owner.class);
            verify(ownerRepository).save(captor.capture());
            Owner saved = captor.getValue();

            assertEquals("new-hash", saved.getPassword());
            verify(passwordEncoder).encode("NewPass1!");
        }
    }

    private static OwnerRequestDTO createSampleDTO() {
        OwnerRequestDTO dto = new OwnerRequestDTO();
        dto.setFirstName("John");
        dto.setLastName("Smith");
        dto.setDateOfBirth(LocalDate.of(2002, 3, 14));
        dto.setEmail("john@gmail.com");
        dto.setPassword("secret");
        dto.setPhone("+3-345-12-12");
        return dto;
    }

    private static Owner createSampleOwner() {
        Owner owner = new Owner();
        owner.setId(1L);
        owner.setFirstName("John");
        owner.setLastName("Smith");
        owner.setDateOfBirth(LocalDate.of(2002, 3, 14));
        owner.setEmail("john@gmail.com");
        owner.setPassword("secret");
        owner.setPhone("+3-345-12-12");
        return owner;
    }
}
