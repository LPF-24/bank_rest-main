package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;

public record CardAdminFilter(
        Long ownerId,
        String email,
        CardStatus status,
        String bin,
        String panLast4
) {}

