package com.example.bankcards.service;

import com.example.bankcards.dto.OwnerRequestDTO;
import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.dto.OwnerUpdateDTO;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.mapper.OwnerMapper;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.security.OwnerDetails;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Stream;

@Service
public class OwnerService {
    private final OwnerRepository ownerRepository;
    private final OwnerMapper ownerMapper;
    private final PasswordEncoder passwordEncoder;

    public OwnerService(OwnerRepository ownerRepository, OwnerMapper ownerMapper, PasswordEncoder passwordEncoder) {
        this.ownerRepository = ownerRepository;
        this.ownerMapper = ownerMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OwnerResponseDTO savePerson(OwnerRequestDTO dto) {
        Owner owner = ownerMapper.toEntity(dto);
        ownerRepository.save(owner);
        return ownerMapper.toResponse(owner);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public OwnerResponseDTO getCurrentCustomerInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OwnerDetails ownerDetails = (OwnerDetails) authentication.getPrincipal();

        Owner owner = ownerRepository.findById(ownerDetails.getId())
                .orElseThrow(() -> new EntityNotFoundException("Customer with ID " + ownerDetails.getId() + " wasn't found!"));

        return ownerMapper.toResponse(owner);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public OwnerResponseDTO updateCurrentCustomerData(OwnerUpdateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OwnerDetails ownerDetails = (OwnerDetails) authentication.getPrincipal();

        Owner ownerToUpdate = ownerRepository.findById(ownerDetails.getId())
                .orElseThrow(() -> new EntityNotFoundException("Customer with ID " + ownerDetails.getId() + " wasn't found!"));

        boolean allFieldsEmpty = Stream.of(
                dto.getPhone(),
                dto.getPassword(),
                dto.getEmail()
        ).allMatch(value -> value == null || value.isBlank());

        if (allFieldsEmpty) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nothing to update");
        }

        if (StringUtils.hasText(dto.getPassword())) {
            ownerToUpdate.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getPhone() != null) ownerToUpdate.setPhone(dto.getPhone());
        if (StringUtils.hasText(dto.getEmail())) {
            if (ownerRepository.existsByEmailAndIdNot(dto.getEmail(), ownerToUpdate.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This email is already taken!");
            }
            ownerToUpdate.setEmail(dto.getEmail());
        }

        return ownerMapper.toResponse(ownerRepository.save(ownerToUpdate));
    }
}
