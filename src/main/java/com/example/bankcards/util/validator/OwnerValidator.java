package com.example.bankcards.util.validator;

import com.example.bankcards.dto.OwnerRequestDTO;
import com.example.bankcards.dto.OwnerUpdateDTO;
import com.example.bankcards.entity.Owner;
import com.example.bankcards.repository.OwnerRepository;
import com.example.bankcards.security.OwnerDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
public class OwnerValidator implements Validator {
    private final OwnerRepository ownerRepository;
    private final Logger logger = LoggerFactory.getLogger(OwnerValidator.class);

    public OwnerValidator(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return OwnerRequestDTO.class.equals(clazz) || OwnerUpdateDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        logger.info("Method validate of OwnerValidator started");
        if (target instanceof OwnerRequestDTO targetDTO) {
            validateEmail(targetDTO.getEmail(), null, errors);
        } else if (target instanceof OwnerUpdateDTO dto) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            OwnerDetails ownerDetails = (OwnerDetails) authentication.getPrincipal();
            validateEmail(dto.getEmail(), ownerDetails.getId(), errors);
        }
    }

    public void validateEmail(String email, Long id, Errors errors) {
        Optional<Owner> peopleWithSameEmail = ownerRepository.findByEmail(email);

        if (peopleWithSameEmail.isPresent()) {
            boolean sameIdExists = peopleWithSameEmail.stream()
                    .anyMatch(person -> person.getId().equals(id));

            if (!sameIdExists) {
                errors.rejectValue("email", "email.taken", "This email is already taken!");
            }
        }
    }
}
