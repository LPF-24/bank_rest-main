package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.exception.ValidationException;
import com.example.bankcards.security.JWTUtil;
import com.example.bankcards.security.OwnerDetails;
import com.example.bankcards.security.OwnerDetailsService;
import com.example.bankcards.service.OwnerService;
import com.example.bankcards.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/login")
    public ResponseEntity<?> performAuthentication(@RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {
        try {
            logger.debug(">>> Received login request: {}", loginRequest.getEmail());
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            OwnerDetails ownerDetails = (OwnerDetails) authentication.getPrincipal();
            String role = ownerDetails.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");

            String accessToken = jwtUtil.generateAccessToken(ownerDetails.getId(), ownerDetails.getUsername(), role);
            String refreshToken = jwtUtil.generateRefreshToken(ownerDetails.getUsername());

            refreshTokenService.saveRefreshToken(ownerDetails.getUsername(), refreshToken);

            return ResponseEntity.ok(new JWTResponse(accessToken, refreshToken, ownerDetails.getId(), ownerDetails.getUsername()));
        } catch (BadCredentialsException e) {
            ErrorResponseDTO error = new ErrorResponseDTO();
            error.setStatus(401);
            error.setMessage("Invalid email or password");
            error.setPath("/owner/login");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/personal-account")
    public ResponseEntity<OwnerResponseDTO> getProfileInfo() {
        return ResponseEntity.ok(ownerService.getCurrentCustomerInfo());
    }
}
