package service;

import domain.BalanceOperationType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BalanceAdjustmentResult(
        UUID operationReference,
        UUID accountId,
        BalanceOperationType type,
        BigDecimal amount,
        String currency,
        BigDecimal resultingBalance,
        Instant createdAt,
        boolean replayed) {}