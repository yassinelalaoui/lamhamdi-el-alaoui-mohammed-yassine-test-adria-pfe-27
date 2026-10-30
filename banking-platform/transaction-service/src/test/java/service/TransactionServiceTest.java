package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.AccountNotActiveException;
import domain.AccountNotFoundException;
import domain.AccountServiceUnavailableException;
import domain.CurrencyMismatchException;
import domain.InsufficientBalanceException;
import domain.InvalidAmountException;
import domain.MaxAmountExceededException;
import domain.Transaction;
import domain.TransactionNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.TransactionRepository;
import web.client.AccountServiceClient;
import web.client.AccountServiceClient.AccountView;
import web.client.AccountServiceClient.BalanceAdjustmentView;

/**
 * Suite de tests unitaires pour {@link TransactionService}.
 *
 * <p>Vérifie scrupuleusement l'ensemble des règles métier demandées :
 * <ol>
 *   <li>Transaction valide.</li>
 *   <li>Montant nul ou négatif.</li>
 *   <li>Montant supérieur au plafond configuré (1000).</li>
 *   <li>Compte inexistant.</li>
 *   <li>Compte bloqué (non actif).</li>
 *   <li>Devise incorrecte / non concordante.</li>
 *   <li>Solde insuffisant.</li>
 *   <li>Erreur de communication avec Account-Service (tolérance aux pannes).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private TransactionEventPublisher eventPublisher;

    private TransactionService transactionService;

    private final BigDecimal maxAmount = new BigDecimal("1000");

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, accountServiceClient, eventPublisher, maxAmount);
    }

    @Test
    @DisplayName("Cas 1 : Transaction valide avec solde suffisant et compte actif")
    void testValidTransaction() {
        UUID accountId = UUID.randomUUID();
        AccountView account = new AccountView(accountId, "CUST-001", "ACTIVE", "EUR", new BigDecimal("500.00"));
        when(accountServiceClient.getAccount(accountId)).thenReturn(account);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountServiceClient.adjustBalance(any(), any())).thenReturn(
                new BalanceAdjustmentView(UUID.randomUUID(), accountId, "DEBIT",
                        new BigDecimal("150.00"), "EUR", new BigDecimal("350.00"), "2026-10-30T10:00:00Z", false));

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("150.00"), "EUR", "ELECTRONICS", "MA");
        Transaction result = transactionService.createTransaction(command);

        assertNotNull(result);
        assertEquals(accountId, result.getAccountId());
        assertEquals(new BigDecimal("150.0000"), result.getAmount());
        assertEquals("EUR", result.getCurrency());
        verify(accountServiceClient).getAccount(accountId);
        verify(accountServiceClient).adjustBalance(any(), any());
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Cas 2 : Montant nul ou négatif rejeté")
    void testZeroOrNegativeAmount() {
        UUID accountId = UUID.randomUUID();
        CreateTransactionCommand cmdZero = new CreateTransactionCommand(
                accountId, BigDecimal.ZERO, "EUR", "GROCERY", "FR");
        CreateTransactionCommand cmdNegative = new CreateTransactionCommand(
                accountId, new BigDecimal("-50.00"), "EUR", "GROCERY", "FR");

        assertThrows(InvalidAmountException.class, () -> transactionService.createTransaction(cmdZero));
        assertThrows(InvalidAmountException.class, () -> transactionService.createTransaction(cmdNegative));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas 3 : Montant supérieur au plafond (1000) rejeté")
    void testAmountExceedsMaxPlafond() {
        UUID accountId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("1000.01"), "EUR", "LUXURY", "US");

        assertThrows(MaxAmountExceededException.class, () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas 4 : Compte inexistant rejeté avec AccountNotFoundException")
    void testAccountNotFound() {
        UUID accountId = UUID.randomUUID();
        when(accountServiceClient.getAccount(accountId)).thenReturn(null);

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("100.00"), "EUR", "RETAIL", "MA");

        assertThrows(AccountNotFoundException.class, () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas 5 : Compte bloqué rejeté avec AccountNotActiveException")
    void testBlockedAccount() {
        UUID accountId = UUID.randomUUID();
        AccountView account = new AccountView(accountId, "CUST-BLOCKED", "BLOCKED", "EUR", new BigDecimal("500.00"));
        when(accountServiceClient.getAccount(accountId)).thenReturn(account);

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("50.00"), "EUR", "RETAIL", "MA");

        assertThrows(AccountNotActiveException.class, () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas 6 : Devise incorrecte / non concordante rejetée avec CurrencyMismatchException")
    void testCurrencyMismatch() {
        UUID accountId = UUID.randomUUID();
        AccountView account = new AccountView(accountId, "CUST-002", "ACTIVE", "EUR", new BigDecimal("500.00"));
        when(accountServiceClient.getAccount(accountId)).thenReturn(account);

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("50.00"), "USD", "RETAIL", "US");

        assertThrows(CurrencyMismatchException.class, () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas 7 : Solde insuffisant rejeté avec InsufficientBalanceException")
    void testInsufficientBalance() {
        UUID accountId = UUID.randomUUID();
        AccountView account = new AccountView(accountId, "CUST-003", "ACTIVE", "EUR", new BigDecimal("50.00"));
        when(accountServiceClient.getAccount(accountId)).thenReturn(account);

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("100.00"), "EUR", "RETAIL", "MA");

        assertThrows(InsufficientBalanceException.class, () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cas 8 : Erreur de communication avec Account-Service (Circuit Breaker)")
    void testAccountServiceCommunicationError() {
        UUID accountId = UUID.randomUUID();
        when(accountServiceClient.getAccount(accountId))
                .thenThrow(new AccountServiceUnavailableException("Circuit open / Service inaccessible"));

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("100.00"), "EUR", "RETAIL", "MA");

        assertThrows(AccountServiceUnavailableException.class, () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Consultation d'une transaction inexistante")
    void testTransactionNotFound() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findById(txId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransaction(txId));
    }
}
