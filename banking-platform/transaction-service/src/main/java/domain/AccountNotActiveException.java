package domain;

import java.util.UUID;

public class AccountNotActiveException extends RuntimeException {
    private final UUID accountId;
    private final String status;

    public AccountNotActiveException(UUID accountId, String status) {
        super(String.format("Le compte %s n'est pas actif (statut actuel : %s).", accountId, status));
        this.accountId = accountId;
        this.status = status;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getStatus() {
        return status;
    }
}

