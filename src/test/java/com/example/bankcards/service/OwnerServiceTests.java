package com.example.bankcards.service;

import com.example.bankcards.dto.OwnerRequestDTO;
import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import com.example.bankcards.mapper.OwnerMapper;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.security.OwnerDetails;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setUp() {
        ModelMapper modelMapper = new ModelMapper();
        this.ownerMapper = new OwnerMapper(modelMapper, passwordEncoder);
        this.ownerService = new OwnerService(ownerRepository, ownerMapper);
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

    // Возможно, на будущее
    private static OwnerResponseDTO createSampleResponseDTO() {
        OwnerResponseDTO response = new OwnerResponseDTO();
        response.setId(1L);
        response.setFirstName("John");
        response.setLastName("Smith");
        response.setDateOfBirth(LocalDate.of(2002, 3, 14));
        response.setEmail("john@gmail.com");
        response.setRole(Role.USER);
        response.setPhone("+3-345-12-12");
        return response;
    }
}
