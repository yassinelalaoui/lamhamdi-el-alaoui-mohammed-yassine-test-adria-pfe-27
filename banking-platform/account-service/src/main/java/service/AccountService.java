package service;

import domain.Account;
import domain.AccountBlockedException;
import domain.AccountNotFoundException;
import domain.AccountStatus;
import domain.BalanceOperation;
import domain.BalanceOperationConflictException;
import domain.BalanceOperationType;
import domain.InsufficientFundsException;
import domain.InvalidAccountCommandException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import repository.AccountRepository;
import repository.BalanceOperationRepository;

@Service
public class AccountService {
    private static final int MONEY_SCALE = 4;
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z]{3}");

    private final AccountRepository accountRepository;
    private final BalanceOperationRepository balanceOperationRepository;

    public AccountService(
            AccountRepository accountRepository, BalanceOperationRepository balanceOperationRepository) {
        this.accountRepository = accountRepository;
        this.balanceOperationRepository = balanceOperationRepository;
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCOPE_accounts.write') and @accountAccessAuthorization.canCreateForCustomer(authentication, #p0.customerReference())")
    public Account createAccount(CreateAccountCommand command) {
        if (command == null) {
            throw new InvalidAccountCommandException("Account details are required.");
        }

        String customerReference = normalizeCustomerReference(command.customerReference());
        String currency = normalizeCurrency(command.currency());
        return accountRepository.save(new Account(customerReference, currency));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SCOPE_accounts.read') and @accountAccessAuthorization.canReadAccount(authentication, #p0)")
    public Account getAccount(@P("accountId") UUID accountId) {
        return accountRepository.findById(requireAccountId(accountId))
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SCOPE_accounts.read') and @accountAccessAuthorization.canReadCustomer(authentication, #p0)")
    public List<Account> getAccountsByCustomer(@P("customerReference") String customerReference) {
        String normalizedReference = normalizeCustomerReference(customerReference);
        return accountRepository.findAllByCustomerReferenceOrderByIdAsc(normalizedReference);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCOPE_accounts.admin.write')")
    public Account updateCustomerReference(
            @P("accountId") UUID accountId, @P("command") UpdateAccountCommand command) {
        if (command == null) {
            throw new InvalidAccountCommandException("Account update details are required.");
        }

        Account account = findAccountForUpdate(accountId);
        account.updateCustomerReference(normalizeCustomerReference(command.customerReference()));
        return accountRepository.save(account);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCOPE_accounts.status.write')")
    public Account updateStatus(@P("accountId") UUID accountId, @P("status") AccountStatus status) {
        if (status == null) {
            throw new InvalidAccountCommandException("Account status is required.");
        }

        Account account = findAccountForUpdate(accountId);
        account.changeStatus(status);
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SCOPE_accounts.read') and @accountAccessAuthorization.canReadAccount(authentication, #p0)")
    public Account getBalance(@P("accountId") UUID accountId) {
        return accountRepository.findById(requireAccountId(accountId))
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCOPE_accounts.balance.write') and @transactionServiceClientAuthorization.isTransactionService(authentication)")
    public BalanceAdjustmentResult adjustBalance(
            @P("accountId") UUID accountId, @P("command") BalanceAdjustmentCommand command) {
        UUID validAccountId = requireAccountId(accountId);
        NormalizedAdjustment adjustment = normalizeAdjustment(command);

        BalanceOperation existingOperation = balanceOperationRepository
                .findById(adjustment.operationReference())
                .orElse(null);
        if (existingOperation != null) {
            return replayOrReject(existingOperation, validAccountId, adjustment);
        }

        Account account = accountRepository.findByIdForUpdate(validAccountId)
                .orElseThrow(() -> new AccountNotFoundException(validAccountId));

        existingOperation = balanceOperationRepository.findById(adjustment.operationReference())
                .orElse(null);
        if (existingOperation != null) {
            return replayOrReject(existingOperation, validAccountId, adjustment);
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountBlockedException(validAccountId);
        }
        if (!account.getCurrency().equals(adjustment.currency())) {
            throw new InvalidAccountCommandException("Adjustment currency must match the account currency.");
        }

        BigDecimal resultingBalance = adjustment.type() == BalanceOperationType.CREDIT
                ? account.getBalance().add(adjustment.amount())
                : account.getBalance().subtract(adjustment.amount());
        if (resultingBalance.signum() < 0) {
            throw new InsufficientFundsException(validAccountId);
        }

        Instant createdAt = Instant.now();
        account.changeBalance(resultingBalance);
        accountRepository.saveAndFlush(account);

        BalanceOperation operation = new BalanceOperation(
                adjustment.operationReference(),
                validAccountId,
                adjustment.type(),
                adjustment.amount(),
                adjustment.currency(),
                resultingBalance,
                createdAt);
        try {
            balanceOperationRepository.saveAndFlush(operation);
        } catch (DataIntegrityViolationException exception) {
            throw new BalanceOperationConflictException(adjustment.operationReference());
        }

        return toResult(operation, false);
    }

    private Account findAccountForUpdate(UUID accountId) {
        UUID validAccountId = requireAccountId(accountId);
        return accountRepository.findByIdForUpdate(validAccountId)
                .orElseThrow(() -> new AccountNotFoundException(validAccountId));
    }

    private UUID requireAccountId(UUID accountId) {
        if (accountId == null) {
            throw new InvalidAccountCommandException("Account ID is required.");
        }
        return accountId;
    }

    private String normalizeCustomerReference(String customerReference) {
        if (!StringUtils.hasText(customerReference)) {
            throw new InvalidAccountCommandException("Customer reference is required.");
        }
        String normalized = customerReference.trim();
        if (normalized.length() > 100) {
            throw new InvalidAccountCommandException("Customer reference must be at most 100 characters.");
        }
        return normalized;
    }

    private String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            throw new InvalidAccountCommandException("Currency is required.");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (!CURRENCY_PATTERN.matcher(normalized).matches()) {
            throw new InvalidAccountCommandException("Currency must be a three-letter ISO code.");
        }
        return normalized;
    }

    private NormalizedAdjustment normalizeAdjustment(BalanceAdjustmentCommand command) {
        if (command == null || command.operationReference() == null || command.type() == null) {
            throw new InvalidAccountCommandException("Operation reference, type, amount and currency are required.");
        }
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new InvalidAccountCommandException("Adjustment amount must be greater than zero.");
        }

        BigDecimal amount;
        try {
            amount = command.amount().setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidAccountCommandException("Adjustment amount supports at most four decimal places.", exception);
        }
        if (amount.precision() > 19) {
            throw new InvalidAccountCommandException("Adjustment amount exceeds the supported precision.");
        }

        return new NormalizedAdjustment(
                command.operationReference(), command.type(), amount, normalizeCurrency(command.currency()));
    }

    private BalanceAdjustmentResult replayOrReject(
            BalanceOperation operation, UUID accountId, NormalizedAdjustment adjustment) {
        boolean sameRequest = operation.getAccountId().equals(accountId)
                && operation.getType() == adjustment.type()
                && operation.getAmount().compareTo(adjustment.amount()) == 0
                && operation.getCurrency().equals(adjustment.currency());
        if (!sameRequest) {
            throw new BalanceOperationConflictException(adjustment.operationReference());
        }
        return toResult(operation, true);
    }

    private BalanceAdjustmentResult toResult(BalanceOperation operation, boolean replayed) {
        return new BalanceAdjustmentResult(
                operation.getOperationReference(),
                operation.getAccountId(),
                operation.getType(),
                operation.getAmount(),
                operation.getCurrency(),
                operation.getResultingBalance(),
                operation.getCreatedAt(),
                replayed);
    }

    private record NormalizedAdjustment(
            UUID operationReference, BalanceOperationType type, BigDecimal amount, String currency) {}
}