package client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "transaction-service")
public interface TransactionServiceClient {

    @GetMapping("/api/transactions/{id}")
    TransactionResponse getTransaction(@PathVariable("id") UUID id);

    @GetMapping("/api/transactions")
    List<TransactionResponse> getTransactionsByAccount(@RequestParam("accountId") UUID accountId);

    @PostMapping("/api/transactions")
    TransactionResponse createTransaction(@RequestBody CreateTransactionRequest request);

    @PostMapping("/api/transactions/transfers")
    List<TransactionResponse> createTransfer(@RequestBody CreateTransferRequest request);

    record TransactionResponse(
            UUID id,
            UUID accountId,
            BigDecimal amount,
            String currency,
            String merchantCategory,
            String country,
            Instant createdAt) {}

    record CreateTransactionRequest(
            UUID accountId,
            BigDecimal amount,
            String currency,
            String merchantCategory,
            String country) {}

    record CreateTransferRequest(
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency) {}
}

