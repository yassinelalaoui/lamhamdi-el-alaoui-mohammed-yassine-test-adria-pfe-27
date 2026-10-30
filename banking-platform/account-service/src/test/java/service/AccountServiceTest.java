package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import domain.Account;
import domain.AccountNotFoundException;
import domain.AccountStatus;
import domain.BalanceOperation;
import domain.BalanceOperationConflictException;
import domain.BalanceOperationType;
import domain.InsufficientFundsException;
import domain.InvalidAccountCommandException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.AccountRepository;
import repository.BalanceOperationRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BalanceOperationRepository balanceOperationRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, balanceOperationRepository);
    }

    @Test
    void createsActiveAccountWithZeroBalanceAndNormalizedCurrency() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = accountService.createAccount(new CreateAccountCommand("customer-1", " eur "));

        assertEquals("customer-1", account.getCustomerReference());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals("EUR", account.getCurrency());
        assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void rejectsInvalidCurrency() {
        assertThrows(
                InvalidAccountCommandException.class,
                () -> accountService.createAccount(new CreateAccountCommand("customer-1", "EURO")));
    }

    @Test
    void reportsMissingAccount() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(accountId));
    }

    @Test
    void blocksAndUnblocksAccount() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account("customer-1", "EUR");
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        assertEquals(AccountStatus.BLOCKED, accountService.updateStatus(accountId, AccountStatus.BLOCKED).getStatus());
        assertEquals(AccountStatus.ACTIVE, accountService.updateStatus(accountId, AccountStatus.ACTIVE).getStatus());
    }

    @Test
    void creditsBalanceAndStoresAnIdempotencyRecord() {
        UUID accountId = UUID.randomUUID();
        UUID operationReference = UUID.randomUUID();
        Account account = new Account("customer-1", "EUR");
        account.changeBalance(new BigDecimal("20.0000"));
        when(balanceOperationRepository.findById(operationReference)).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));

        BalanceAdjustmentResult result = accountService.adjustBalance(
                accountId,
                new BalanceAdjustmentCommand(
                        operationReference, BalanceOperationType.CREDIT, new BigDecimal("12.50"), "EUR"));

        assertEquals(new BigDecimal("32.5000"), result.resultingBalance());
        assertFalse(result.replayed());
        ArgumentCaptor<BalanceOperation> operationCaptor = ArgumentCaptor.forClass(BalanceOperation.class);
        verify(balanceOperationRepository).saveAndFlush(operationCaptor.capture());
        assertEquals(operationReference, operationCaptor.getValue().getOperationReference());
    }

    @Test
    void rejectsDebitThatWouldMakeBalanceNegative() {
        UUID accountId = UUID.randomUUID();
        UUID operationReference = UUID.randomUUID();
        Account account = new Account("customer-1", "EUR");
        when(balanceOperationRepository.findById(operationReference)).thenReturn(Optional.empty());
        when(accountRepository.findByIdForUpdate(accountId)).thenReturn(Optional.of(account));

        assertThrows(
                InsufficientFundsException.class,
                () -> accountService.adjustBalance(
                        accountId,
                        new BalanceAdjustmentCommand(
                                operationReference, BalanceOperationType.DEBIT, new BigDecimal("1.00"), "EUR")));
        verify(balanceOperationRepository, never()).saveAndFlush(any(BalanceOperation.class));
    }

    @Test
    void returnsSameResultWhenOperationIsReplayed() {
        UUID accountId = UUID.randomUUID();
        UUID operationReference = UUID.randomUUID();
        BalanceOperation operation = new BalanceOperation(
                operationReference,
                accountId,
                BalanceOperationType.CREDIT,
                new BigDecimal("10.0000"),
                "EUR",
                new BigDecimal("10.0000"),
                java.time.Instant.parse("2025-01-01T00:00:00Z"));
        when(balanceOperationRepository.findById(operationReference)).thenReturn(Optional.of(operation));

        BalanceAdjustmentResult replay = accountService.adjustBalance(
                accountId,
                new BalanceAdjustmentCommand(
                        operationReference, BalanceOperationType.CREDIT, new BigDecimal("10"), "eur"));

        assertTrue(replay.replayed());
        assertEquals(new BigDecimal("10.0000"), replay.resultingBalance());
        verify(accountRepository, never()).findByIdForUpdate(accountId);
    }

    @Test
    void rejectsReusingOperationReferenceForDifferentRequest() {
        UUID accountId = UUID.randomUUID();
        UUID operationReference = UUID.randomUUID();
        BalanceOperation operation = new BalanceOperation(
                operationReference,
                accountId,
                BalanceOperationType.CREDIT,
                new BigDecimal("10.0000"),
                "EUR",
                new BigDecimal("10.0000"),
                java.time.Instant.now());
        when(balanceOperationRepository.findById(operationReference)).thenReturn(Optional.of(operation));

        assertThrows(
                BalanceOperationConflictException.class,
                () -> accountService.adjustBalance(
                        accountId,
                        new BalanceAdjustmentCommand(
                                operationReference, BalanceOperationType.CREDIT, new BigDecimal("11"), "EUR")));
    }
}