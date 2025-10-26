package com.example.bankcards.service;

import com.example.bankcards.dto.OwnerRequestDTO;
import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.mapper.OwnerMapper;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.security.OwnerDetails;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerService {
    private final OwnerRepository ownerRepository;
    private final OwnerMapper ownerMapper;

    public OwnerService(OwnerRepository ownerRepository, OwnerMapper ownerMapper) {
        this.ownerRepository = ownerRepository;
        this.ownerMapper = ownerMapper;
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
}
