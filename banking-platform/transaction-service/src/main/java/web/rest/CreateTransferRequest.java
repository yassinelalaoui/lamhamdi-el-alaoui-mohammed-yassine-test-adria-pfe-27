package web.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import service.CreateTransferCommand;

public record CreateTransferRequest(
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationAccountId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency) {

    public CreateTransferCommand toCommand() {
        return new CreateTransferCommand(sourceAccountId, destinationAccountId, amount, currency);
    }
}
