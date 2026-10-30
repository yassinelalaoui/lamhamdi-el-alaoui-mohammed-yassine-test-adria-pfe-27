package agent;

import client.AccountServiceClient;
import client.TransactionServiceClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.BankingTools;

@Service
public class BankingAgentService {
    private static final Logger log = LoggerFactory.getLogger(BankingAgentService.class);

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(\\d+(\\.\\d{1,4})?)\\s*(EUR|USD|MAD)");

    private final BankingTools tools;

    public BankingAgentService(BankingTools tools) {
        this.tools = tools;
    }

    public AgentResponse processMessage(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return new AgentResponse("Bonjour ! Je suis votre assistant bancaire virtuel. Comment puis-je vous aider aujourd'hui ?", "HELP", null);
        }

        String msg = userMessage.trim();
        log.info("Processing user query: {}", msg);

        try {
            // Intent 1: Check balance / account details
            if (matches(msg, "solde|compte|balance|status")) {
                UUID accountId = extractUuid(msg);
                if (accountId != null) {
                    AccountServiceClient.AccountResponse account = tools.getAccountDetails(accountId);
                    String reply = String.format(
                            "Le compte %s (%s) est actuellement %s avec un solde de %s %s.",
                            account.id(), account.customerReference(), account.status(), account.balance(), account.currency());
                    return new AgentResponse(reply, "GET_ACCOUNT", account);
                }
            }

            // Intent 2: Transfer between accounts
            if (matches(msg, "virement|transfer|virer|envoyer")) {
                List<UUID> uuids = extractAllUuids(msg);
                Matcher amountMatcher = AMOUNT_PATTERN.matcher(msg);
                if (uuids.size() >= 2 && amountMatcher.find()) {
                    UUID src = uuids.get(0);
                    UUID dst = uuids.get(1);
                    BigDecimal amount = new BigDecimal(amountMatcher.group(1));
                    String currency = amountMatcher.group(3);

                    List<TransactionServiceClient.TransactionResponse> txs =
                            tools.executeTransfer(src, dst, amount, currency);
                    String reply = String.format(
                            "Virement de %s %s effectué avec succès du compte %s vers le compte %s (ID Débit: %s, ID Crédit: %s).",
                            amount, currency, src, dst, txs.get(0).id(), txs.get(1).id());
                    return new AgentResponse(reply, "TRANSFER_COMPLETED", txs);
                }
            }

            // Intent 3: View transaction history
            if (matches(msg, "historique|transactions|operations")) {
                UUID accountId = extractUuid(msg);
                if (accountId != null) {
                    List<TransactionServiceClient.TransactionResponse> txs = tools.getAccountTransactions(accountId);
                    StringBuilder reply = new StringBuilder();
                    reply.append(String.format("Historique des transactions pour le compte %s (%d trouvées) :\n", accountId, txs.size()));
                    for (TransactionServiceClient.TransactionResponse tx : txs) {
                        reply.append(String.format("- [%s] %s %s | %s (%s)\n",
                                tx.createdAt(), tx.amount(), tx.currency(), tx.merchantCategory(), tx.country()));
                    }
                    return new AgentResponse(reply.toString(), "TRANSACTION_HISTORY", txs);
                }
            }

            // Intent 4: Record a new transaction (payment/debit)
            if (matches(msg, "payer|achat|debit|depense|transaction")) {
                UUID accountId = extractUuid(msg);
                Matcher amountMatcher = AMOUNT_PATTERN.matcher(msg);
                if (accountId != null && amountMatcher.find()) {
                    BigDecimal amount = new BigDecimal(amountMatcher.group(1));
                    String currency = amountMatcher.group(3);
                    TransactionServiceClient.TransactionResponse tx = tools.recordTransaction(
                            accountId, amount, currency, "RETAIL", "MA");
                    String reply = String.format(
                            "Transaction de %s %s enregistrée avec succès pour le compte %s (ID: %s).",
                            amount, currency, accountId, tx.id());
                    return new AgentResponse(reply, "TRANSACTION_RECORDED", tx);
                }
            }

            // Fallback: Assistance guide
            return new AgentResponse(
                    "Je suis prêt à vous aider. Exemples de requêtes :\n" +
                    "- 'Quel est le solde du compte 11111111-1111-1111-1111-111111111111 ?'\n" +
                    "- 'Virement de 150 EUR de 11111111-1111-1111-1111-111111111111 vers 22222222-2222-2222-2222-222222222222'\n" +
                    "- 'Historique des transactions du compte 11111111-1111-1111-1111-111111111111'",
                    "HELP",
                    null);
        } catch (Exception e) {
            log.error("Error executing agent action: {}", e.getMessage(), e);
            return new AgentResponse("Une erreur est survenue lors du traitement de l'opération : " + e.getMessage(), "ERROR", null);
        }
    }

    private boolean matches(String text, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    private UUID extractUuid(String text) {
        Matcher matcher = UUID_PATTERN.matcher(text);
        if (matcher.find()) {
            return UUID.fromString(matcher.group(1));
        }
        return null;
    }

    private List<UUID> extractAllUuids(String text) {
        List<UUID> list = new java.util.ArrayList<>();
        Matcher matcher = UUID_PATTERN.matcher(text);
        while (matcher.find()) {
            list.add(UUID.fromString(matcher.group(1)));
        }
        return list;
    }

    public record AgentResponse(String message, String intent, Object details) {}
}

