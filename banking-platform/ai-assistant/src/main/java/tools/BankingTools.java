package tools;

import client.AccountServiceClient;
import client.TransactionServiceClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Banking tools that can be executed by the AI Agent to automate operations on
 * the account-service and transaction-service.
 */
@Component
public class BankingTools {
    private static final Logger log = LoggerFactory.getLogger(BankingTools.class);

    private final AccountServiceClient accountClient;
    private final TransactionServiceClient transactionClient;

    public BankingTools(
            AccountServiceClient accountClient,
            TransactionServiceClient transactionClient) {
        this.accountClient = accountClient;
        this.transactionClient = transactionClient;
    }

    public AccountServiceClient.AccountResponse getAccountDetails(UUID accountId) {
        log.info("Agent Tool Executing: getAccountDetails for accountId={}", accountId);
        return accountClient.getAccount(accountId);
    }

    public AccountServiceClient.AccountResponse createAccount(String customerRef, String currency, BigDecimal initialBalance) {
        log.info("Agent Tool Executing: createAccount customerRef={} currency={} initialBalance={}",
                customerRef, currency, initialBalance);
        return accountClient.createAccount(new AccountServiceClient.CreateAccountRequest(customerRef, currency, initialBalance));
    }

    public List<TransactionServiceClient.TransactionResponse> getAccountTransactions(UUID accountId) {
        log.info("Agent Tool Executing: getAccountTransactions for accountId={}", accountId);
        return transactionClient.getTransactionsByAccount(accountId);
    }

    public TransactionServiceClient.TransactionResponse recordTransaction(
            UUID accountId, BigDecimal amount, String currency, String merchantCategory, String country) {
        log.info("Agent Tool Executing: recordTransaction accountId={} amount={} {}", accountId, amount, currency);
        return transactionClient.createTransaction(
                new TransactionServiceClient.CreateTransactionRequest(accountId, amount, currency, merchantCategory, country));
    }

    public List<TransactionServiceClient.TransactionResponse> executeTransfer(
            UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String currency) {
        log.info("Agent Tool Executing: executeTransfer from={} to={} amount={} {}",
                sourceAccountId, destinationAccountId, amount, currency);
        return transactionClient.createTransfer(
                new TransactionServiceClient.CreateTransferRequest(sourceAccountId, destinationAccountId, amount, currency));
    }
}

