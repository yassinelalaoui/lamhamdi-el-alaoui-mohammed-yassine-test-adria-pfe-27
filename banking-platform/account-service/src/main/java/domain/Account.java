package domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.Check;

@Entity
@Table(
        name = "accounts",
        indexes = @Index(name = "idx_accounts_customer_reference", columnList = "customer_reference"))
@Check(constraints = "balance >= 0")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_reference", nullable = false, length = 100)
    private String customerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Version
    @Column(nullable = false)
    private long version;

    protected Account() {}

    public Account(String customerReference, String currency) {
        this.customerReference = customerReference;
        this.currency = currency;
        this.status = AccountStatus.ACTIVE;
        this.balance = BigDecimal.ZERO.setScale(4);
    }

    public UUID getId() {
        return id;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public long getVersion() {
        return version;
    }

    public void updateCustomerReference(String customerReference) {
        this.customerReference = customerReference;
    }

    public void changeStatus(AccountStatus status) {
        this.status = status;
    }

    public void changeBalance(BigDecimal balance) {
        this.balance = balance;
    }
}