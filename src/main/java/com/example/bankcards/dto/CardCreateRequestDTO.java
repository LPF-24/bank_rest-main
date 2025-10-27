package com.example.bankcards.dto;

import jakarta.validation.constraints.NotNull;

public class CardCreateRequestDTO {
    @NotNull(message = "Owner ID is required")
    private Long ownerId;

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
}

