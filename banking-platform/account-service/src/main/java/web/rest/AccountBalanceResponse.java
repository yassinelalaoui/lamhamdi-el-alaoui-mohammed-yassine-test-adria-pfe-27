package web.rest;

import domain.Account;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceResponse(UUID accountId, BigDecimal balance, String currency) {
    public static AccountBalanceResponse from(Account account) {
        return new AccountBalanceResponse(account.getId(), account.getBalance(), account.getCurrency());
    }
}