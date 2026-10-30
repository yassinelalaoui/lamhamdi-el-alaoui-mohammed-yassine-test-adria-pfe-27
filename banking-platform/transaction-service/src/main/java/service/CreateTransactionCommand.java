package service;

import java.math.BigDecimal;
import java.util.UUID;

/** Command for creating a new debit/credit transaction on a single account. */
public record CreateTransactionCommand(
        UUID accountId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country) {}
