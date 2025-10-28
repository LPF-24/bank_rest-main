package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Запрос на перевод средств между двумя картами одного владельца.
 * Используется в endpoint `/cards/transfer`.
 */
@Schema(description = "Запрос на перевод средств между двумя картами одного клиента")
public class TransferRequestDTO {

    @Schema(
            description = "Сумма перевода. Должна быть положительной (больше 0).",
            example = "150.00",
            minimum = "0.01",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    @Schema(
            description = "ID карты, с которой будут списаны средства",
            example = "7",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "fromCardId is required")
    private Long fromCardId;

    @Schema(
            description = "ID карты, на которую будут зачислены средства",
            example = "9",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "toCardId is required")
    private Long toCardId;

    public TransferRequestDTO() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getFromCardId() {
        return fromCardId;
    }

    public void setFromCardId(Long fromCardId) {
        this.fromCardId = fromCardId;
    }

    public Long getToCardId() {
        return toCardId;
    }

    public void setToCardId(Long toCardId) {
        this.toCardId = toCardId;
    }
}