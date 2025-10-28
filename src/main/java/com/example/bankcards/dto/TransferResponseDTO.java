package com.example.bankcards.dto;

public class TransferResponseDTO {
    private CardResponseDTO from;
    private CardResponseDTO to;

    public TransferResponseDTO() {
    }

    public CardResponseDTO getFrom() {
        return from;
    }

    public void setFrom(CardResponseDTO from) {
        this.from = from;
    }

    public CardResponseDTO getTo() {
        return to;
    }

    public void setTo(CardResponseDTO to) {
        this.to = to;
    }
}

