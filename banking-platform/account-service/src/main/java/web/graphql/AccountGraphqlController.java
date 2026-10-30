package web.graphql;

import domain.Account;
import domain.AccountStatus;
import domain.BalanceOperationType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import service.AccountService;
import service.BalanceAdjustmentCommand;
import service.BalanceAdjustmentResult;
import service.CreateAccountCommand;
import service.UpdateAccountCommand;

@Controller
public class AccountGraphqlController {
    private final AccountService accountService;

    public AccountGraphqlController(AccountService accountService) {
        this.accountService = accountService;
    }

    @QueryMapping
    public Account account(@Argument("id") String id) {
        return accountService.getAccount(UUID.fromString(id));
    }

    @QueryMapping
    public List<Account> accounts(@Argument("customerReference") String customerReference) {
        return accountService.getAccountsByCustomer(customerReference);
    }

    @QueryMapping
    public Account accountBalance(@Argument("id") String id) {
        return accountService.getBalance(UUID.fromString(id));
    }

    @MutationMapping
    public Account createAccount(@Argument("input") CreateAccountInput input) {
        return accountService.createAccount(new CreateAccountCommand(input.customerReference(), input.currency()));
    }

    @MutationMapping
        public Account updateAccount(
            @Argument("id") String id, @Argument("input") UpdateAccountInput input) {
        return accountService.updateCustomerReference(
                UUID.fromString(id), new UpdateAccountCommand(input.customerReference()));
    }

    @MutationMapping
        public Account updateAccountStatus(
            @Argument("id") String id, @Argument("status") AccountStatus status) {
        return accountService.updateStatus(UUID.fromString(id), status);
    }

    @MutationMapping
        public BalanceAdjustmentResult adjustAccountBalance(
            @Argument("id") String id, @Argument("input") BalanceAdjustmentInput input) {
        return accountService.adjustBalance(
                UUID.fromString(id),
                new BalanceAdjustmentCommand(
                        UUID.fromString(input.operationReference()),
                        input.type(),
                        new BigDecimal(input.amount()),
                        input.currency()));
    }

    public record CreateAccountInput(String customerReference, String currency) {}

    public record UpdateAccountInput(String customerReference) {}

    public record BalanceAdjustmentInput(
            String operationReference, BalanceOperationType type, String amount, String currency) {}
}