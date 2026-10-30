package web.rest;

import domain.Account;
import domain.AccountStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id, String customerReference, AccountStatus status, String currency, BigDecimal balance, long version) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerReference(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance(),
                account.getVersion());
    }
}