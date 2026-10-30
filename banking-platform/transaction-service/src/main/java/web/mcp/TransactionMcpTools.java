package web.mcp;

import domain.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import service.CreateTransactionCommand;
import service.CreateTransferCommand;
import service.TransactionService;

/**
 * MCP tools for the transaction-service, callable by the AI assistant.
 *
 * <p>All methods delegate to the same {@link TransactionService} used by REST and GraphQL.
 */
@Component
public class TransactionMcpTools {
    private final TransactionService transactionService;

    public TransactionMcpTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @McpTool(name = "get_transaction", description = "Retrieve a transaction by its UUID.")
    public TransactionView getTransaction(
            @McpToolParam(description = "Transaction UUID", required = true) String transactionId) {
        return TransactionView.from(transactionService.getTransaction(UUID.fromString(transactionId)));
    }

    @McpTool(name = "list_account_transactions",
             description = "List all transactions for a given account, newest first.")
    public List<TransactionView> listTransactionsByAccount(
            @McpToolParam(description = "Account UUID", required = true) String accountId) {
        return transactionService.getTransactionsByAccount(UUID.fromString(accountId)).stream()
                .map(TransactionView::from)
                .toList();
    }

    @McpTool(name = "create_transaction",
             description = "Record a new debit transaction and update the account balance.")
    public TransactionView createTransaction(
            @McpToolParam(description = "Account UUID", required = true) String accountId,
            @McpToolParam(description = "Amount (positive)", required = true) String amount,
            @McpToolParam(description = "ISO 4217 currency code", required = true) String currency,
            @McpToolParam(description = "Merchant category", required = true) String merchantCategory,
            @McpToolParam(description = "Country", required = true) String country) {
        Transaction transaction = transactionService.createTransaction(new CreateTransactionCommand(
                UUID.fromString(accountId),
                new BigDecimal(amount),
                currency,
                merchantCategory,
                country));
        return TransactionView.from(transaction);
    }

    @McpTool(name = "create_transfer",
             description = "Transfer funds from one account to another.")
    public List<TransactionView> createTransfer(
            @McpToolParam(description = "Source account UUID", required = true) String sourceAccountId,
            @McpToolParam(description = "Destination account UUID", required = true) String destinationAccountId,
            @McpToolParam(description = "Amount to transfer (positive)", required = true) String amount,
            @McpToolParam(description = "ISO 4217 currency code", required = true) String currency) {
        return transactionService.createTransfer(new CreateTransferCommand(
                UUID.fromString(sourceAccountId),
                UUID.fromString(destinationAccountId),
                new BigDecimal(amount),
                currency)).stream()
                .map(TransactionView::from)
                .toList();
    }

    public record TransactionView(
            UUID id,
            UUID accountId,
            BigDecimal amount,
            String currency,
            String merchantCategory,
            String country,
            Instant createdAt) {
        public static TransactionView from(Transaction transaction) {
            return new TransactionView(
                    transaction.getId(),
                    transaction.getAccountId(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    transaction.getMerchantCategory(),
                    transaction.getCountry(),
                    transaction.getCreatedAt());
        }
    }
}
