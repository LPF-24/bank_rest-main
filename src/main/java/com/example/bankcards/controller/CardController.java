package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponseDTO;
import com.example.bankcards.dto.DepositRequestDTO;
import com.example.bankcards.dto.TransferRequestDTO;
import com.example.bankcards.dto.TransferResponseDTO;
import com.example.bankcards.dto.ErrorResponseDTO;
import com.example.bankcards.security.OwnerDetails;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cards", description = "Операции с картами текущего пользователя")
@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardService cardService;
    public CardController(CardService cardService) { this.cardService = cardService; }

    @Operation(
            summary = "Список моих карт",
            description = "Возвращает страницу карт текущего пользователя. По умолчанию сортировка: createdAt DESC.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Страница карт возвращена"),
                    @ApiResponse(responseCode = "401", description = "Неавторизован",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class),
                                    examples = @ExampleObject(name="Unauthorized",
                                            value = "{\"status\":401,\"message\":\"Unauthorized: missing or invalid token\",\"path\":\"/cards\"}"))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping
    public Page<CardResponseDTO> getMyCards(
            @AuthenticationPrincipal @Parameter(hidden = true) OwnerDetails ownerDetails,
            @Parameter(description = "Параметры страницы (page, size, sort)")
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return cardService.getMyCards(ownerDetails.getId(), pageable);
    }

    @Operation(
            summary = "Получить мою карту по ID",
            description = "Возвращает карту по идентификатору, если она принадлежит текущему пользователю.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Карта найдена",
                            content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Неверный формат ID",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class),
                                    examples = @ExampleObject(name="BadPathParam",
                                            value = "{\"status\":400,\"message\":\"Invalid value 'abc' for parameter 'id'\",\"path\":\"/cards/abc\"}"))),
                    @ApiResponse(responseCode = "401", description = "Неавторизован",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена или не принадлежит пользователю",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @GetMapping("/{id}")
    public CardResponseDTO getMyCard(
            @AuthenticationPrincipal @Parameter(hidden = true) OwnerDetails me,
            @Parameter(description = "ID карты", example = "123") @PathVariable Long id) {
        return cardService.getMyCardById(me.getId(), id);
    }

    @Operation(
            summary = "Пополнить мою карту",
            description = "Пополняет баланс карты, если карта принадлежит текущему пользователю и не заблокирована.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = DepositRequestDTO.class),
                            examples = @ExampleObject(value = "{\"amount\":150.25}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Баланс обновлён",
                            content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Невалидная сумма (<= 0) или ошибки валидации",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class),
                                    examples = @ExampleObject(value = "{\"status\":400,\"message\":\"amount: must be greater than 0\",\"path\":\"/cards/7/deposit\"}"))),
                    @ApiResponse(responseCode = "401", description = "Неавторизован",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена или не принадлежит пользователю",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "409", description = "Карта заблокирована",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/{id}/deposit")
    public CardResponseDTO depositMyCard(
            @AuthenticationPrincipal @Parameter(hidden = true) OwnerDetails me,
            @Parameter(description = "ID карты", example = "7") @PathVariable Long id,
            @RequestBody @Valid DepositRequestDTO dto
    ) {
        return cardService.depositMyCard(me.getId(), id, dto.getAmount());
    }

    @Operation(
            summary = "Снять с моей карты",
            description = "Снимает средства с карты, если карта принадлежит текущему пользователю и не заблокирована. Баланс не может стать отрицательным.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = DepositRequestDTO.class),
                            examples = @ExampleObject(value = "{\"amount\":40}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Баланс обновлён",
                            content = @Content(schema = @Schema(implementation = CardResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Невалидная сумма (<= 0) или ошибки валидации",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизован",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Карта не найдена или не принадлежит пользователю",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "409", description = "Недостаточно средств или карта заблокирована",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/{id}/withdraw")
    public CardResponseDTO withdrawMyCard(
            @AuthenticationPrincipal @Parameter(hidden = true) OwnerDetails me,
            @Parameter(description = "ID карты", example = "7") @PathVariable Long id,
            @RequestBody @Valid DepositRequestDTO dto
    ) {
        return cardService.withdrawMyCard(me.getId(), id, dto.getAmount());
    }

    @Operation(
            summary = "Перевод между моими картами",
            description = "Переводит сумму между двумя картами текущего пользователя. Карты должны отличаться, иметь одинаковую валюту, на исходной карте достаточно средств, обе карты не заблокированы.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = TransferRequestDTO.class),
                            examples = @ExampleObject(value = "{\"fromCardId\":7,\"toCardId\":9,\"amount\":40}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Перевод выполнен",
                            content = @Content(schema = @Schema(implementation = TransferResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Невалидные данные (amount <= 0 или одинаковые карты)",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизован",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Одна из карт не найдена или не принадлежит пользователю",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                    @ApiResponse(responseCode = "409", description = "Недостаточно средств / карта заблокирована / валюты не совпадают",
                            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
            }
    )
    @PostMapping("/transfer")
    public TransferResponseDTO transferBetweenMyCards(
            @AuthenticationPrincipal @Parameter(hidden = true) OwnerDetails me,
            @RequestBody @Valid TransferRequestDTO dto) {
        return cardService.transferBetweenMyCards(me.getId(), dto.getFromCardId(), dto.getToCardId(), dto.getAmount());
    }
}
