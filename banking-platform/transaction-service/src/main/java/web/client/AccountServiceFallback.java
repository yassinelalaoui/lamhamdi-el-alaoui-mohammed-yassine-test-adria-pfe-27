package web.client;

import domain.AccountServiceUnavailableException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resilience4J fallback for the {@link AccountServiceClient}.
 *
 * <p>When the circuit breaker is open or the call times out, this bean is called instead of the
 * real HTTP client so that the caller receives a meaningful domain exception.
 */
@Component
public class AccountServiceFallback implements AccountServiceClient {

    @Override
    public AccountView getAccount(UUID accountId) {
        throw new AccountServiceUnavailableException(
                "Account service is temporarily unavailable. Cannot retrieve account " + accountId);
    }

    @Override
    public BalanceAdjustmentView adjustBalance(UUID accountId, BalanceAdjustmentRequest request) {
        throw new AccountServiceUnavailableException(
                "Account service is temporarily unavailable. Cannot adjust balance for account " + accountId);
    }
}
