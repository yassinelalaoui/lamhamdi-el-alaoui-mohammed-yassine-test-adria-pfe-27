package service;

import domain.BalanceOperationType;
import java.math.BigDecimal;
import java.util.UUID;

public record BalanceAdjustmentCommand(
        UUID operationReference, BalanceOperationType type, BigDecimal amount, String currency) {}