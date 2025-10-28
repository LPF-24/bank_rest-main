package com.example.bankcards.controller;

import com.example.bankcards.dto.CardAdminFilter;
import com.example.bankcards.dto.CardCreateRequestDTO;
import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.AdminService;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/cards")
public class AdminCardController {

    private final CardService cardService;
    private final AdminService adminService;

    public AdminCardController(CardService cardService, AdminService adminService) {
        this.cardService = cardService;
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<CardResponseDTO> create(@RequestBody @Valid CardCreateRequestDTO dto) {
        CardResponseDTO response = cardService.createCard(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/block")
    public CardResponseDTO blockCardByAdmin(@PathVariable Long id) {
        return adminService.adminBlockCard(id);
    }

    @PatchMapping("/{id}/unblock")
    public CardResponseDTO unblockCardByAdmin(@PathVariable Long id) {
        return adminService.adminUnblockCard(id);
    }

    @GetMapping
    public Page<CardResponseDTO> findAll(
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String bin,
            @RequestParam(required = false) String panLast4,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        CardAdminFilter filter = new CardAdminFilter(ownerId, email, status, bin, panLast4);
        return adminService.findCards(filter, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        adminService.adminDeleteCard(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
