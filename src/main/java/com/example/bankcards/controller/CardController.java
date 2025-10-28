package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.dto.DepositRequestDTO;
import com.example.bankcards.dto.TransferRequestDTO;
import com.example.bankcards.dto.TransferResponseDTO;
import com.example.bankcards.security.OwnerDetails;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cards")
public class CardController {
    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public Page<CardResponseDTO> getMyCards(
            @AuthenticationPrincipal OwnerDetails ownerDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return cardService.getMyCards(ownerDetails.getId(), pageable);
    }

    @GetMapping("/{id}")
    public CardResponseDTO getMyCard(
            @AuthenticationPrincipal OwnerDetails me,
            @PathVariable Long id) {
        return cardService.getMyCardById(me.getId(), id);
    }

    @PostMapping("/{id}/deposit")
    public CardResponseDTO depositMyCard(
            @AuthenticationPrincipal OwnerDetails me,
            @PathVariable Long id,
            @RequestBody @Valid DepositRequestDTO dto
    ) {
        return cardService.depositMyCard(me.getId(), id, dto.getAmount());
    }

    @PostMapping("/{id}/withdraw")
    public CardResponseDTO withdrawMyCard(
            @AuthenticationPrincipal OwnerDetails me,
            @PathVariable Long id,
            @RequestBody @Valid DepositRequestDTO dto
    ) {
        return cardService.withdrawMyCard(me.getId(), id, dto.getAmount());
    }

    @PostMapping("/transfer")
    public TransferResponseDTO transferBetweenMyCards(
            @AuthenticationPrincipal OwnerDetails me,
            @RequestBody @Valid TransferRequestDTO dto) {
        return cardService.transferBetweenMyCards(me.getId(), dto.getFromCardId(), dto.getToCardId(), dto.getAmount());
    }

}

