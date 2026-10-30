package web.client;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client that calls the {@code account-service} via Eureka service discovery.
 *
 * <p>Requests are signed with an internal service token obtained from the security configuration.
 * The circuit breaker fallback is handled by {@link AccountServiceFallback}.
 */
@FeignClient(
        name = "account-service",
        fallback = AccountServiceFallback.class,
        configuration = AccountServiceFeignConfig.class)
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{id}")
    AccountView getAccount(@PathVariable("id") UUID accountId);

    @PatchMapping("/api/accounts/{id}/balance")
    BalanceAdjustmentView adjustBalance(
            @PathVariable("id") UUID accountId, @RequestBody BalanceAdjustmentRequest request);

    record AccountView(
            UUID id,
            String customerReference,
            String status,
            String currency,
            BigDecimal balance) {}

    record BalanceAdjustmentRequest(
            UUID operationReference, String type, BigDecimal amount, String currency) {}

    record BalanceAdjustmentView(
            UUID operationReference,
            UUID accountId,
            String type,
            BigDecimal amount,
            String currency,
            BigDecimal resultingBalance,
            String createdAt,
            boolean replayed) {}
}
