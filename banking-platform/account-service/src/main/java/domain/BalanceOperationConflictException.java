package domain;

import java.util.UUID;

public class BalanceOperationConflictException extends RuntimeException {
    public BalanceOperationConflictException(UUID operationReference) {
        super("Operation reference was already used with a different request: " + operationReference);
    }
}