package web.rest;

import domain.BalanceOperationType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import service.BalanceAdjustmentResult;

public record BalanceAdjustmentResponse(
        UUID operationReference,
        UUID accountId,
        BalanceOperationType type,
        BigDecimal amount,
        String currency,
        BigDecimal resultingBalance,
        Instant createdAt,
        boolean replayed) {
    public static BalanceAdjustmentResponse from(BalanceAdjustmentResult result) {
        return new BalanceAdjustmentResponse(
                result.operationReference(),
                result.accountId(),
                result.type(),
                result.amount(),
                result.currency(),
                result.resultingBalance(),
                result.createdAt(),
                result.replayed());
    }
}