package com.example.bankcards.mapper;

import com.example.bankcards.dto.OwnerRequestDTO;
import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.entity.Role;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class OwnerMapper {
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public OwnerMapper(ModelMapper modelMapper, PasswordEncoder passwordEncoder) {
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Owner toEntity(OwnerRequestDTO dto) {
        Owner owner = new Owner();
        owner.setFirstName(dto.getFirstName());
        owner.setLastName(dto.getLastName());
        owner.setDateOfBirth(dto.getDateOfBirth());
        owner.setEmail(dto.getEmail());
        owner.setPassword(passwordEncoder.encode(dto.getPassword()));
        owner.setPhone(dto.getPhone());
        owner.setRole(Role.USER);
        return owner;
    }

    public OwnerResponseDTO toResponse(Owner owner) {
        return modelMapper.map(owner, OwnerResponseDTO.class);
    }
}
