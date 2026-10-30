package domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_balance_operations")
public class BalanceOperation {
    @Id
    @Column(name = "operation_reference", nullable = false, updatable = false)
    private UUID operationReference;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 16, updatable = false)
    private BalanceOperationType type;

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "resulting_balance", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal resultingBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BalanceOperation() {}

    public BalanceOperation(
            UUID operationReference,
            UUID accountId,
            BalanceOperationType type,
            BigDecimal amount,
            String currency,
            BigDecimal resultingBalance,
            Instant createdAt) {
        this.operationReference = operationReference;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.resultingBalance = resultingBalance;
        this.createdAt = createdAt;
    }

    public UUID getOperationReference() {
        return operationReference;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public BalanceOperationType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}