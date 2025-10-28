package com.example.bankcards.mapper;

import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.entity.Card;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

    public CardResponseDTO toResponse(Card card) {
        CardResponseDTO dto = new CardResponseDTO();
        dto.setId(card.getId());
        dto.setMaskedPan("**** **** **** " + card.getPanLast4());
        dto.setExpiryMonth(card.getExpiryMonth());
        dto.setExpiryYear(card.getExpiryYear());
        dto.setStatus(card.getStatus());
        dto.setBalance(card.getBalance());
        dto.setCurrency(card.getCurrency());
        return dto;
    }
}
