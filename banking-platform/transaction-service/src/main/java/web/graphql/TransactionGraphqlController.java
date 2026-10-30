package web.graphql;

import domain.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import service.CreateTransactionCommand;
import service.CreateTransferCommand;
import service.TransactionService;

@Controller
public class TransactionGraphqlController {
    private final TransactionService transactionService;

    public TransactionGraphqlController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public Transaction transaction(@Argument("id") String id) {
        return transactionService.getTransaction(UUID.fromString(id));
    }

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.getAllTransactions();
    }

    @QueryMapping
    public List<Transaction> transactionsByAccount(@Argument("accountId") String accountId) {
        return transactionService.getTransactionsByAccount(UUID.fromString(accountId));
    }

    @MutationMapping
    public Transaction createTransaction(@Argument("input") CreateTransactionInput input) {
        return transactionService.createTransaction(new CreateTransactionCommand(
                UUID.fromString(input.accountId()),
                new BigDecimal(input.amount()),
                input.currency(),
                input.merchantCategory(),
                input.country()));
    }

    @MutationMapping
    public List<Transaction> createTransfer(@Argument("input") CreateTransferInput input) {
        return transactionService.createTransfer(new CreateTransferCommand(
                UUID.fromString(input.sourceAccountId()),
                UUID.fromString(input.destinationAccountId()),
                new BigDecimal(input.amount()),
                input.currency()));
    }

    public record CreateTransactionInput(
            String accountId,
            String amount,
            String currency,
            String merchantCategory,
            String country) {}

    public record CreateTransferInput(
            String sourceAccountId,
            String destinationAccountId,
            String amount,
            String currency) {}
}
