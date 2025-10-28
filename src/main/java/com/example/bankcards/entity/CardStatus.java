package com.example.bankcards.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Статус банковской карты.
 * Используется для определения доступности операций с картой.
 */
@Schema(description = "Статус банковской карты")
public enum CardStatus {

    @Schema(description = "Карта активна и доступна для всех операций (пополнение, снятие, переводы)")
    ACTIVE,

    @Schema(description = "Карта заблокирована администратором. Операции недоступны (пополнение, снятие, переводы запрещены)")
    BLOCKED,

    @Schema(description = "Срок действия карты истёк. Использование карты ограничено согласно бизнес-правилам")
    EXPIRED
}
