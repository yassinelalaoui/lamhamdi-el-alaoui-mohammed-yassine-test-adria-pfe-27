package service;

import domain.AccountNotActiveException;
import domain.AccountNotFoundException;
import domain.AccountServiceUnavailableException;
import domain.CurrencyMismatchException;
import domain.InsufficientBalanceException;
import domain.InvalidAmountException;
import domain.InvalidTransactionException;
import domain.MaxAmountExceededException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import repository.TransactionRepository;
import web.client.AccountServiceClient;

/**
 * Service métier central pour la gestion des transactions bancaires.
 *
 * <p>Ce service unique est partagé par les contrôleurs REST, GraphQL et les outils MCP.
 *
 * <p>Règles métier strictement appliquées :
 * <ol>
 *   <li>Le montant doit être strictement positif.</li>
 *   <li>Le montant ne doit pas dépasser le plafond configuré (ex. 1000).</li>
 *   <li>Le compte doit exister (vérification via OpenFeign vers account-service).</li>
 *   <li>Le compte doit être ACTIVE.</li>
 *   <li>La devise doit correspondre à celle du compte.</li>
 *   <li>Le solde doit être suffisant pour couvrir le montant.</li>
 *   <li>Le solde est obligatoirement consulté avant tout enregistrement.</li>
 *   <li>Après enregistrement, le solde du compte est débité de manière idempotente.</li>
 *   <li>Si le débit échoue, la transaction est annulée (rollback) et rejetée.</li>
 * </ol>
 */
@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
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

    /**
     * Crée et enregistre une transaction unitaire après validation complète.
     */
    @Transactional
    public Transaction createTransaction(CreateTransactionCommand command) {
        if (command == null) {
            throw new InvalidTransactionException("Les données de transaction sont obligatoires.");
        }

        UUID accountId = requireNonNull(command.accountId(), "L'identifiant de compte");
        BigDecimal amount = validateAndNormalizeAmount(command.amount());
        String currency = validateAndNormalizeCurrency(command.currency());
        String merchantCategory = requireText(command.merchantCategory(), "La catégorie du marchand");
        String country = requireText(command.country(), "Le pays");

        // RÈGLE 3 & 7 : Consultation préalable du compte et de son solde via OpenFeign
        AccountServiceClient.AccountView account = fetchAccount(accountId);

        // RÈGLE 4 : Le compte doit être ACTIVE
        if (!"ACTIVE".equalsIgnoreCase(account.status())) {
            throw new AccountNotActiveException(accountId, account.status());
        }

        // RÈGLE 5 : La devise de la transaction doit correspondre à celle du compte
        if (!account.currency().equalsIgnoreCase(currency)) {
            throw new CurrencyMismatchException(currency, account.currency());
        }

        // RÈGLE 6 : Le solde doit être suffisant
        if (account.balance() == null || account.balance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(accountId, account.balance(), amount);
        }

        // Enregistrement de la transaction
        Instant now = Instant.now();
        Transaction transaction = new Transaction(accountId, amount, currency, merchantCategory, country, now);
        transaction = transactionRepository.save(transaction);
        log.info("Transaction persistée [id={}, accountId={}, amount={} {}]",
                transaction.getId(), accountId, amount, currency);

        // RÈGLE 8 & 9 : Débit du solde avec clé d'idempotence (ID de la transaction)
        AccountServiceClient.BalanceAdjustmentRequest adjustment =
                new AccountServiceClient.BalanceAdjustmentRequest(
                        transaction.getId(), "DEBIT", amount, currency);

        try {
            accountServiceClient.adjustBalance(accountId, adjustment);
            log.info("Solde débité avec succès pour le compte [id={}, txId={}]", accountId, transaction.getId());
        } catch (Exception ex) {
            // RÈGLE 9 : Si le débit échoue, la transaction est annulée (rollback)
            log.error("Échec du débit du compte {}. Annulation de la transaction {}.",
                    accountId, transaction.getId(), ex);
            transactionRepository.delete(transaction);
            if (ex instanceof AccountServiceUnavailableException) {
                throw ex;
            }
            throw new AccountServiceUnavailableException(
                    "Échec de mise à jour du solde auprès du service de comptes : " + ex.getMessage(), ex);
        }

        // Publication de l'événement Kafka
        eventPublisher.publish(TransactionCreatedEvent.from(transaction));

        return transaction;
    }

    /**
     * Consultation d'une transaction par son identifiant unique.
     */
    @Transactional(readOnly = true)
    public Transaction getTransaction(UUID transactionId) {
        return transactionRepository
                .findById(requireNonNull(transactionId, "L'identifiant de transaction"))
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    /**
     * Consultation de l'ensemble des transactions (triées par date décroissante).
     */
    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Consultation de toutes les transactions associées à un compte donné.
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAccount(UUID accountId) {
        return transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(
                requireNonNull(accountId, "L'identifiant de compte"));
    }

    /**
     * Virement inter-comptes avec double débit/crédit et validation.
     */
    @Transactional
    public List<Transaction> createTransfer(CreateTransferCommand command) {
        if (command == null) {
            throw new InvalidTransactionException("Les données du virement sont obligatoires.");
        }

        UUID sourceId = requireNonNull(command.sourceAccountId(), "Le compte source");
        UUID destinationId = requireNonNull(command.destinationAccountId(), "Le compte destinataire");
        if (sourceId.equals(destinationId)) {
            throw new InvalidTransactionException("Le compte source et le compte destinataire doivent être différents.");
        }

        BigDecimal amount = validateAndNormalizeAmount(command.amount());
        String currency = validateAndNormalizeCurrency(command.currency());

        AccountServiceClient.AccountView source = fetchAccount(sourceId);
        AccountServiceClient.AccountView destination = fetchAccount(destinationId);

        if (!"ACTIVE".equalsIgnoreCase(source.status())) {
            throw new AccountNotActiveException(sourceId, source.status());
        }
        if (!"ACTIVE".equalsIgnoreCase(destination.status())) {
            throw new AccountNotActiveException(destinationId, destination.status());
        }
        if (!source.currency().equalsIgnoreCase(currency)) {
            throw new CurrencyMismatchException(currency, source.currency());
        }
        if (!destination.currency().equalsIgnoreCase(currency)) {
            throw new CurrencyMismatchException(currency, destination.currency());
        }
        if (source.balance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(sourceId, source.balance(), amount);
        }

        Instant now = Instant.now();
        Transaction debitTx = transactionRepository.save(
                new Transaction(sourceId, amount, currency, "TRANSFER_OUT", "INTERNAL", now));
        Transaction creditTx = transactionRepository.save(
                new Transaction(destinationId, amount, currency, "TRANSFER_IN", "INTERNAL", now));

        // Débit source
        accountServiceClient.adjustBalance(
                sourceId,
                new AccountServiceClient.BalanceAdjustmentRequest(debitTx.getId(), "DEBIT", amount, currency));

        // Crédit destination
        accountServiceClient.adjustBalance(
                destinationId,
                new AccountServiceClient.BalanceAdjustmentRequest(creditTx.getId(), "CREDIT", amount, currency));

        eventPublisher.publish(TransactionCreatedEvent.from(debitTx));
        eventPublisher.publish(TransactionCreatedEvent.from(creditTx));

        return List.of(debitTx, creditTx);
    }

    // ---- Méthodes de validation et normalisation privées ----

    private AccountServiceClient.AccountView fetchAccount(UUID accountId) {
        try {
            AccountServiceClient.AccountView view = accountServiceClient.getAccount(accountId);
            if (view == null) {
                throw new AccountNotFoundException(accountId);
            }
            return view;
        } catch (AccountNotFoundException ex) {
            throw ex;
        } catch (AccountServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("404")) {
                throw new AccountNotFoundException(accountId);
            }
            throw new AccountServiceUnavailableException(
                    "Impossible de joindre Account-Service pour le compte " + accountId + " : " + ex.getMessage(), ex);
        }
    }

    private BigDecimal validateAndNormalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException(amount);
        }
        BigDecimal scaled;
        try {
            scaled = amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidAmountException("Le montant autorise au maximum 4 décimales.");
        }
        if (scaled.compareTo(maxAmount.setScale(MONEY_SCALE)) > 0) {
            throw new MaxAmountExceededException(amount, maxAmount);
        }
        return scaled;
    }

    private String validateAndNormalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            throw new InvalidTransactionException("La devise est obligatoire.");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (!CURRENCY_PATTERN.matcher(normalized).matches()) {
            throw new InvalidTransactionException("La devise doit être un code ISO à 3 lettres majuscules (ex. EUR, USD).");
        }
        return normalized;
    }

    private String requireText(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidTransactionException(label + " est obligatoire.");
        }
        return value.trim();
    }

    private <T> T requireNonNull(T value, String label) {
        if (value == null) {
            throw new InvalidTransactionException(label + " est obligatoire.");
        }
        return value;
    }
}
