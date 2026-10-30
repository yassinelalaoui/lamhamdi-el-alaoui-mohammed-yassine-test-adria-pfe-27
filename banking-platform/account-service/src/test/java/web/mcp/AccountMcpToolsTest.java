package web.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.Account;
import domain.AccountStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.AccountService;

@ExtendWith(MockitoExtension.class)
class AccountMcpToolsTest {
    @Mock
    private AccountService accountService;

    @Test
    void accountToolDelegatesToAccountService() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account("customer-1", "EUR");
        when(accountService.getAccount(accountId)).thenReturn(account);
        AccountMcpTools tools = new AccountMcpTools(accountService);

        AccountMcpTools.AccountToolView result = tools.getAccount(accountId.toString());

        assertEquals("customer-1", result.customerReference());
        assertEquals(AccountStatus.ACTIVE, result.status());
        verify(accountService).getAccount(accountId);
    }

    @Test
    void balanceToolDelegatesToAccountService() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account("customer-1", "EUR");
        account.changeBalance(new BigDecimal("21.5000"));
        when(accountService.getBalance(accountId)).thenReturn(account);
        AccountMcpTools tools = new AccountMcpTools(accountService);

        AccountMcpTools.AccountBalanceToolView result = tools.getAccountBalance(accountId.toString());

        assertEquals(new BigDecimal("21.5000"), result.balance());
        verify(accountService).getBalance(accountId);
    }
}