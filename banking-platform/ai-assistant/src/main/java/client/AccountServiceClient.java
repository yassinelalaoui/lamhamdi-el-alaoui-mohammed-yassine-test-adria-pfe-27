package client;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{id}")
    AccountResponse getAccount(@PathVariable("id") UUID id);

    @PostMapping("/api/accounts")
    AccountResponse createAccount(@RequestBody CreateAccountRequest request);

    record AccountResponse(
            UUID id,
            String customerReference,
            String status,
            String currency,
            BigDecimal balance) {}

    record CreateAccountRequest(
            String customerReference,
            String currency,
            BigDecimal initialBalance) {}
}
