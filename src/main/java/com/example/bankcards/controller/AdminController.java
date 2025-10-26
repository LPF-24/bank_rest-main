package com.example.bankcards.controller;

import com.example.bankcards.dto.CodeRequestDTO;
import com.example.bankcards.exception.ValidationException;
import com.example.bankcards.security.OwnerDetails;
import com.example.bankcards.service.AdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;
    private final Logger logger = LoggerFactory.getLogger(AdminController.class);

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PatchMapping("/promote")
    public ResponseEntity<?> promote(@RequestBody @Valid CodeRequestDTO code, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.error("BindingResult has errors: ");
            bindingResult.getFieldErrors().forEach(fieldError -> {
                logger.error(fieldError.getDefaultMessage());
            });
            throw new ValidationException(bindingResult);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OwnerDetails personDetails = (OwnerDetails) authentication.getPrincipal();

        logger.debug("Middle of the method");
        adminService.promotePerson(personDetails.getId());
        logger.info("Promotion was successful");
        return ResponseEntity.ok("You have been successfully promoted to administrator. Please log in again, colleague.");
    }
}
