package web.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import service.CreateTransactionCommand;

public record CreateTransactionRequest(
        @NotNull UUID accountId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String merchantCategory,
        @NotBlank String country) {

    public CreateTransactionCommand toCommand() {
        return new CreateTransactionCommand(accountId, amount, currency, merchantCategory, country);
    }
}
