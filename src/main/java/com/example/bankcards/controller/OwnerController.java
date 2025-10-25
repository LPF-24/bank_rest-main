package com.example.bankcards.controller;

import com.example.bankcards.dto.OwnerRequestDTO;
import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.exception.ValidationException;
import com.example.bankcards.security.JWTUtil;
import com.example.bankcards.security.OwnerDetailsService;
import com.example.bankcards.service.OwnerService;
import com.example.bankcards.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/owner")
public class OwnerController {
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final OwnerDetailsService ownerDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final Logger logger = LoggerFactory.getLogger(OwnerController.class);
    private final OwnerService ownerService;

    public OwnerController(JWTUtil jwtUtil, AuthenticationManager authenticationManager, OwnerDetailsService ownerDetailsService, RefreshTokenService refreshTokenService, OwnerService ownerService) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.ownerDetailsService = ownerDetailsService;
        this.refreshTokenService = refreshTokenService;
        this.ownerService = ownerService;
    }

    @PostMapping("/registration")
    public ResponseEntity<OwnerResponseDTO> register(@RequestBody @Valid OwnerRequestDTO dto, BindingResult bindingResult) {
        // TODO (возможно): ownerValidator
        if (bindingResult.hasErrors()) {
            logger.error("Binding result has errors: ");
            bindingResult.getFieldErrors().forEach(fieldError ->
                    logger.error(fieldError.getDefaultMessage()));
            throw new ValidationException(bindingResult);
        }

        logger.info("Middle of the method");
        OwnerResponseDTO response = ownerService.savePerson(dto);
        logger.info("Customer {} {} successfully created", dto.getFirstName(), dto.getLastName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
