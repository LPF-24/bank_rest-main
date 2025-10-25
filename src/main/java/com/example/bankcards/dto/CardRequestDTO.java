package com.example.bankcards.dto;

import com.example.bankcards.entity.Currency;
import com.example.bankcards.util.annotation.ValidPan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CardRequestDTO {
    @NotBlank(message = "PAN is required")
    @ValidPan
    private String pan;

    @NotNull(message = "Currency is required")
    private Currency currency;

    public CardRequestDTO() {
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
