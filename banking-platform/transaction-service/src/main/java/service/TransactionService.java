package service;

import domain.AccountServiceUnavailableException;
import domain.InvalidTransactionException;
import domain.Transaction;
import domain.TransactionCreatedEvent;
import domain.TransactionNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import repository.TransactionRepository;
import web.client.AccountServiceClient;

/**
 * Core business logic for creating and consulting transactions.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Amount must be strictly positive and within the configured maximum.</li>
 *   <li>The account must exist and be ACTIVE (verified via OpenFeign → account-service).</li>
 *   <li>Transaction currency must match the account currency.</li>
 *   <li>Balance is updated atomically in account-service after the transaction is persisted.</li>
 *   <li>A Kafka event is published after successful persistence.</li>
 * </ul>
 */
@Service
public class TransactionService {
    private static final int MONEY_SCALE = 4;
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z]{3}");

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionEventPublisher eventPublisher;
    private final BigDecimal maxAmount;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountServiceClient accountServiceClient,
            TransactionEventPublisher eventPublisher,
            @Value("${bank.transaction.max-amount:1000}") BigDecimal maxAmount) {
        this.transactionRepository = transactionRepository;
        this.accountServiceClient = accountServiceClient;
        this.eventPublisher = eventPublisher;
        this.maxAmount = maxAmount;
    }

    @Transactional
    public Transaction createTransaction(CreateTransactionCommand command) {
        if (command == null) {
            throw new InvalidTransactionException("Transaction details are required.");
        }

        UUID accountId = requireAccountId(command.accountId());
        BigDecimal amount = normalizeAmount(command.amount());
        String currency = normalizeCurrency(command.currency());
        String merchantCategory = requireText(command.merchantCategory(), "Merchant category");
        String country = requireText(command.country(), "Country");

        // Verify account existence and active status
        AccountServiceClient.AccountView account = fetchAccount(accountId);
        assertAccountActive(account);
        assertCurrencyMatches(account, currency);

        Instant now = Instant.now();
        Transaction transaction = new Transaction(accountId, amount, currency, merchantCategory, country, now);
        transaction = transactionRepository.save(transaction);

        // Update balance in account-service (DEBIT – funds leave the account)
        AccountServiceClient.BalanceAdjustmentRequest adjustment =
                new AccountServiceClient.BalanceAdjustmentRequest(
                        transaction.getId(), "DEBIT", amount, currency);
        try {
            accountServiceClient.adjustBalance(accountId, adjustment);
        } catch (AccountServiceUnavailableException exception) {
            // Propagate: the transaction has been saved but balance update failed.
            // The Kafka event will not be published in this case.
            throw exception;
        }

        eventPublisher.publish(TransactionCreatedEvent.from(transaction));
        return transaction;
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(UUID transactionId) {
        return transactionRepository
                .findById(requireNonNull(transactionId, "Transaction ID"))
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAccount(UUID accountId) {
        return transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(
                requireNonNull(accountId, "Account ID"));
    }

    /**
     * Transfer: debit source account, credit destination account.
     *
     * <p>Both legs are executed sequentially. If the credit fails after a successful debit the
     * caller should handle compensation (beyond exam scope — circuit-breaker fallback throws).
     */
    @Transactional
    public List<Transaction> createTransfer(CreateTransferCommand command) {
        if (command == null) {
            throw new InvalidTransactionException("Transfer details are required.");
        }

        UUID sourceId = requireAccountId(command.sourceAccountId());
        UUID destinationId = requireAccountId(command.destinationAccountId());
        if (sourceId.equals(destinationId)) {
            throw new InvalidTransactionException("Source and destination accounts must differ.");
        }

        BigDecimal amount = normalizeAmount(command.amount());
        String currency = normalizeCurrency(command.currency());

        AccountServiceClient.AccountView source = fetchAccount(sourceId);
        AccountServiceClient.AccountView destination = fetchAccount(destinationId);
        assertAccountActive(source);
        assertAccountActive(destination);
        assertCurrencyMatches(source, currency);
        assertCurrencyMatches(destination, currency);

        Instant now = Instant.now();
        Transaction debit = transactionRepository.save(
                new Transaction(sourceId, amount, currency, "TRANSFER_OUT", "INTERNAL", now));
        Transaction credit = transactionRepository.save(
                new Transaction(destinationId, amount, currency, "TRANSFER_IN", "INTERNAL", now));

        // Debit source
        accountServiceClient.adjustBalance(
                sourceId,
                new AccountServiceClient.BalanceAdjustmentRequest(debit.getId(), "DEBIT", amount, currency));

        // Credit destination
        accountServiceClient.adjustBalance(
                destinationId,
                new AccountServiceClient.BalanceAdjustmentRequest(credit.getId(), "CREDIT", amount, currency));

        eventPublisher.publish(TransactionCreatedEvent.from(debit));
        eventPublisher.publish(TransactionCreatedEvent.from(credit));
        return List.of(debit, credit);
    }

    // ---- helpers ----

    private AccountServiceClient.AccountView fetchAccount(UUID accountId) {
        try {
            return accountServiceClient.getAccount(accountId);
        } catch (AccountServiceUnavailableException exception) {
            throw exception;
        }
    }

    private void assertAccountActive(AccountServiceClient.AccountView account) {
        if (!"ACTIVE".equals(account.status())) {
            throw new InvalidTransactionException("Account " + account.id() + " is not active.");
        }
    }

    private void assertCurrencyMatches(AccountServiceClient.AccountView account, String currency) {
        if (!account.currency().equals(currency)) {
            throw new InvalidTransactionException(
                    "Transaction currency " + currency + " does not match account currency " + account.currency() + ".");
        }
    }

    private UUID requireAccountId(UUID accountId) {
        return requireNonNull(accountId, "Account ID");
    }

    private <T> T requireNonNull(T value, String label) {
        if (value == null) {
            throw new InvalidTransactionException(label + " is required.");
        }
        return value;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionException("Amount must be strictly positive.");
        }
        BigDecimal scaled;
        try {
            scaled = amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidTransactionException("Amount supports at most four decimal places.", exception);
        }
        if (scaled.compareTo(maxAmount.setScale(MONEY_SCALE)) > 0) {
            throw new InvalidTransactionException(
                    "Amount " + amount + " exceeds the configured maximum of " + maxAmount + ".");
        }
        return scaled;
    }

    private String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            throw new InvalidTransactionException("Currency is required.");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (!CURRENCY_PATTERN.matcher(normalized).matches()) {
            throw new InvalidTransactionException("Currency must be a three-letter ISO code.");
        }
        return normalized;
    }

    private String requireText(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidTransactionException(label + " is required.");
        }
        return value.trim();
    }
}
