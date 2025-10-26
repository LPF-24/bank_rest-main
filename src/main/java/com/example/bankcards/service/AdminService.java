package com.example.bankcards.service;

import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import com.example.bankcards.mapper.OwnerMapper;
import com.example.bankcards.repository.OwnerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final OwnerRepository ownerRepository;
    private final OwnerMapper ownerMapper;

    public AdminService(OwnerRepository ownerRepository, OwnerMapper ownerMapper) {
        this.ownerRepository = ownerRepository;
        this.ownerMapper = ownerMapper;
    }

    @Transactional
    public void promotePerson(Long personId) {
        Owner owner = ownerRepository.findById(personId)
                .orElseThrow(() -> new EntityNotFoundException("Customer with this id " + personId + " can't be found"));

        owner.setRole(Role.ADMIN);
        ownerRepository.save(owner);
    }

    @Transactional(readOnly = true)
    public List<OwnerResponseDTO> findAllUsers() {
        return ownerRepository.findAll().stream()
                .filter(person -> Role.USER.equals(person.getRole()))
                .map(ownerMapper::toResponse)
                .toList();
    }
}
