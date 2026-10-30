package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.AccountServiceUnavailableException;
import domain.InvalidTransactionException;
import domain.Transaction;
import domain.TransactionNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.TransactionRepository;
import web.client.AccountServiceClient;
import web.client.AccountServiceClient.AccountView;
import web.client.AccountServiceClient.BalanceAdjustmentView;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private TransactionEventPublisher eventPublisher;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, accountServiceClient, eventPublisher, new BigDecimal("1000"));
    }

    @Test
    void createsTransactionWhenAccountIsActiveAndCurrencyMatches() {
        UUID accountId = UUID.randomUUID();
        AccountView account = new AccountView(accountId, "customer-1", "ACTIVE", "EUR", new BigDecimal("500.00"));
        when(accountServiceClient.getAccount(accountId)).thenReturn(account);
        when(accountServiceClient.adjustBalance(any(), any())).thenReturn(
                new BalanceAdjustmentView(UUID.randomUUID(), accountId, "DEBIT",
                        new BigDecimal("100.00"), "EUR", new BigDecimal("400.00"), "2025-01-01T00:00:00Z", false));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("100.00"), "EUR", "FOOD", "FR");
        Transaction result = transactionService.createTransaction(command);

        assertNotNull(result);
        assertEquals(accountId, result.getAccountId());
        assertEquals(new BigDecimal("100.0000"), result.getAmount());
        assertEquals("EUR", result.getCurrency());
        verify(eventPublisher).publish(any());
    }

    @Test
    void rejectsTransactionWhenAmountExceedsMax() {
        UUID accountId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("1500.00"), "EUR", "FOOD", "FR");

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsTransactionWhenAmountIsZero() {
        UUID accountId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, BigDecimal.ZERO, "EUR", "FOOD", "FR");

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.createTransaction(command));
    }

    @Test
    void rejectsTransactionWhenAccountIsBlocked() {
        UUID accountId = UUID.randomUUID();
        AccountView account = new AccountView(accountId, "customer-1", "BLOCKED", "EUR", new BigDecimal("500.00"));
        when(accountServiceClient.getAccount(accountId)).thenReturn(account);

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("50.00"), "EUR", "FOOD", "FR");

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsTransactionWhenCurrencyDoesNotMatchAccount() {
        UUID accountId = UUID.randomUUID();
        AccountView account = new AccountView(accountId, "customer-1", "ACTIVE", "EUR", new BigDecimal("500.00"));
        when(accountServiceClient.getAccount(accountId)).thenReturn(account);

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("50.00"), "USD", "FOOD", "FR");

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void propagatesAccountServiceUnavailableException() {
        UUID accountId = UUID.randomUUID();
        when(accountServiceClient.getAccount(accountId))
                .thenThrow(new AccountServiceUnavailableException("Circuit open"));

        CreateTransactionCommand command = new CreateTransactionCommand(
                accountId, new BigDecimal("50.00"), "EUR", "FOOD", "FR");

        assertThrows(AccountServiceUnavailableException.class,
                () -> transactionService.createTransaction(command));
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void throwsTransactionNotFoundForUnknownId() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findById(txId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.getTransaction(txId));
    }
}
