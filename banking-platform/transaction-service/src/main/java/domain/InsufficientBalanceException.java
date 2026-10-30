package domain;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(UUID accountId, BigDecimal balance, BigDecimal requestedAmount) {
        super(String.format("Solde insuffisant sur le compte %s : solde actuel = %s, montant demandé = %s.",
                accountId, balance, requestedAmount));
    }
}

