package domain;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID accountId) {
        super("Insufficient funds for account: " + accountId);
    }
}