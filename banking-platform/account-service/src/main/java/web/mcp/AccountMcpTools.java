package web.mcp;

import domain.Account;
import domain.AccountStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import service.AccountService;

@Component
public class AccountMcpTools {
    private final AccountService accountService;

    public AccountMcpTools(AccountService accountService) {
        this.accountService = accountService;
    }

    @McpTool(name = "get_account", description = "Read an account by ID when the caller is authorized.")
    public AccountToolView getAccount(
            @McpToolParam(description = "Account UUID", required = true) String accountId) {
        return AccountToolView.from(accountService.getAccount(UUID.fromString(accountId)));
    }

    @McpTool(name = "check_account_status", description = "Check an authorized account's current status.")
    public AccountStatus checkAccountStatus(
            @McpToolParam(description = "Account UUID", required = true) String accountId) {
        return accountService.getAccount(UUID.fromString(accountId)).getStatus();
    }

    @McpTool(name = "get_account_balance", description = "Read an authorized account's balance and currency.")
    public AccountBalanceToolView getAccountBalance(
            @McpToolParam(description = "Account UUID", required = true) String accountId) {
        return AccountBalanceToolView.from(accountService.getBalance(UUID.fromString(accountId)));
    }

    public record AccountToolView(
            UUID id, String customerReference, AccountStatus status, String currency, BigDecimal balance) {
        public static AccountToolView from(Account account) {
            return new AccountToolView(
                    account.getId(),
                    account.getCustomerReference(),
                    account.getStatus(),
                    account.getCurrency(),
                    account.getBalance());
        }
    }

    public record AccountBalanceToolView(UUID accountId, BigDecimal balance, String currency) {
        public static AccountBalanceToolView from(Account account) {
            return new AccountBalanceToolView(account.getId(), account.getBalance(), account.getCurrency());
        }
    }
}