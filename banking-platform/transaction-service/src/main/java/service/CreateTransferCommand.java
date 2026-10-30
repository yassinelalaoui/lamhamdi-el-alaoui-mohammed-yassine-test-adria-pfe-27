package service;

import java.math.BigDecimal;
import java.util.UUID;

/** Command for initiating a transfer between two accounts. */
public record CreateTransferCommand(
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        String currency) {}
