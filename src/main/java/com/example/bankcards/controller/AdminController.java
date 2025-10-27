package com.example.bankcards.controller;

import com.example.bankcards.dto.CodeRequestDTO;
import com.example.bankcards.dto.OwnerAdminUpdateDTO;
import com.example.bankcards.dto.OwnerResponseDTO;
import com.example.bankcards.exception.ValidationException;
import com.example.bankcards.security.OwnerDetails;
import com.example.bankcards.service.AdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/all-customers")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<OwnerResponseDTO>> getAllCustomers() {
        return ResponseEntity.ok(adminService.findAllUsers());
    }

    @PatchMapping("/block-customer/{id}")
    public ResponseEntity<String> blockCustomer(@PathVariable("id") Long ownerId) {
        adminService.blockCustomer(ownerId);
        return ResponseEntity.ok("Customer's account with id " + ownerId + " is locked.");
    }

    @PatchMapping("/unblock-customer/{id}")
    public ResponseEntity<String> unblockCustomer(@PathVariable("id") Long ownerId) {
        adminService.unlockCustomer(ownerId);
        return ResponseEntity.ok("Customer's account with id " + ownerId + " is unlocked.");
    }

    @PatchMapping("/update-customer/{id}")
    public ResponseEntity<OwnerResponseDTO> updateCustomerDataAsAdmin(
            @PathVariable("id") Long ownerId,
            @RequestBody @Valid OwnerAdminUpdateDTO dto,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            throw new ValidationException(bindingResult);
        }

        OwnerResponseDTO response = adminService.updateCustomerDataByAdmin(ownerId, dto);
        return ResponseEntity.ok(response);
    }
}
