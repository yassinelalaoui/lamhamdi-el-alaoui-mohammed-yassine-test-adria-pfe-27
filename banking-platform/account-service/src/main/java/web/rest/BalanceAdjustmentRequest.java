package web.rest;

import domain.BalanceOperationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;
import service.BalanceAdjustmentCommand;

public record BalanceAdjustmentRequest(
        @NotNull UUID operationReference,
        @NotNull BalanceOperationType type,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotNull @Pattern(regexp = "(?i)^[A-Z]{3}$") String currency) {
    public BalanceAdjustmentCommand toCommand() {
        return new BalanceAdjustmentCommand(operationReference, type, amount, currency);
    }
}