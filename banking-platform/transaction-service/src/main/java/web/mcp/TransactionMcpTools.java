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
 * Serveur d'outils MCP pour Transaction-Service.
 *
 * <p>Permet à l'agent IA d'automatiser la création et la consultation des transactions.
 * Utilise rigoureusement la même couche service ({@link TransactionService}) que REST et GraphQL.
 */
@Component
public class TransactionMcpTools {
    private final TransactionService transactionService;

    public TransactionMcpTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @McpTool(name = "get_transaction", description = "Consulter une transaction par son identifiant UUID.")
    public TransactionView getTransaction(
            @McpToolParam(description = "Identifiant UUID de la transaction", required = true) String transactionId) {
        return TransactionView.from(transactionService.getTransaction(UUID.fromString(transactionId)));
    }

    @McpTool(name = "list_transactions", description = "Consulter la liste de toutes les transactions bancaires enregistrées.")
    public List<TransactionView> listAllTransactions() {
        return transactionService.getAllTransactions().stream()
                .map(TransactionView::from)
                .toList();
    }

    @McpTool(name = "list_account_transactions",
             description = "Consulter l'historique complet des transactions d'un compte donné.")
    public List<TransactionView> listTransactionsByAccount(
            @McpToolParam(description = "Identifiant UUID du compte bancaire", required = true) String accountId) {
        return transactionService.getTransactionsByAccount(UUID.fromString(accountId)).stream()
                .map(TransactionView::from)
                .toList();
    }

    @McpTool(name = "create_transaction",
             description = "Créer et persister une transaction bancaire débitrice après validation du solde.")
    public TransactionView createTransaction(
            @McpToolParam(description = "Identifiant UUID du compte bancaire", required = true) String accountId,
            @McpToolParam(description = "Montant strictement positif (max 1000.00)", required = true) String amount,
            @McpToolParam(description = "Code devise ISO à 3 lettres (ex. EUR)", required = true) String currency,
            @McpToolParam(description = "Catégorie du marchand (ex. GROCERY, RETAIL)", required = true) String merchantCategory,
            @McpToolParam(description = "Code pays (ex. FR, MA)", required = true) String country) {
        Transaction transaction = transactionService.createTransaction(new CreateTransactionCommand(
                UUID.fromString(accountId),
                new BigDecimal(amount),
                currency,
                merchantCategory,
                country));
        return TransactionView.from(transaction);
    }

    @McpTool(name = "create_transfer",
             description = "Effectuer un virement sécurisé entre deux comptes bancaires.")
    public List<TransactionView> createTransfer(
            @McpToolParam(description = "UUID du compte source (débit)", required = true) String sourceAccountId,
            @McpToolParam(description = "UUID du compte destinataire (crédit)", required = true) String destinationAccountId,
            @McpToolParam(description = "Montant à transférer", required = true) String amount,
            @McpToolParam(description = "Devise ISO", required = true) String currency) {
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
