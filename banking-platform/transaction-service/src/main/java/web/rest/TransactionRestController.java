package web.rest;

import domain.Transaction;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionRestController {
    private final TransactionService transactionService;

    public TransactionRestController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(request.toCommand());
        return ResponseEntity.created(URI.create("/api/transactions/" + transaction.getId()))
                .body(TransactionResponse.from(transaction));
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable("id") UUID id) {
        return TransactionResponse.from(transactionService.getTransaction(id));
    }

    @GetMapping
    public List<TransactionResponse> getTransactionsByAccount(
            @RequestParam("accountId") UUID accountId) {
        return transactionService.getTransactionsByAccount(accountId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @PostMapping("/transfers")
    public ResponseEntity<List<TransactionResponse>> createTransfer(
            @Valid @RequestBody CreateTransferRequest request) {
        List<TransactionResponse> responses = transactionService.createTransfer(request.toCommand())
                .stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
