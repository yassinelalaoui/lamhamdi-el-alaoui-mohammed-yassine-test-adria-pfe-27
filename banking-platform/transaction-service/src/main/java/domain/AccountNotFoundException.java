package domain;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    private final UUID accountId;

    public AccountNotFoundException(UUID accountId) {
        super("Compte introuvable : " + accountId);
        this.accountId = accountId;
    }

    public UUID getAccountId() {
        return accountId;
    }
}
