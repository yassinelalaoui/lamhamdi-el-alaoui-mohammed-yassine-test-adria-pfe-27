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

    /**
     * Enregistrement d'une nouvelle transaction bancaire.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(request.toCommand());
        return ResponseEntity.created(URI.create("/api/transactions/" + transaction.getId()))
                .body(TransactionResponse.from(transaction));
    }

    /**
     * Consultation d'une transaction par son ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(TransactionResponse.from(transactionService.getTransaction(id)));
    }

    /**
     * Consultation de l'ensemble des transactions (avec filtre optionnel accountId).
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(value = "accountId", required = false) UUID accountId) {
        List<Transaction> transactions = (accountId != null)
                ? transactionService.getTransactionsByAccount(accountId)
                : transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions.stream().map(TransactionResponse::from).toList());
    }

    /**
     * Consultation de toutes les transactions d'un compte spécifique.
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccount(
            @PathVariable("accountId") UUID accountId) {
        List<TransactionResponse> list = transactionService.getTransactionsByAccount(accountId).stream()
                .map(TransactionResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    /**
     * Exécution d'un virement entre deux comptes.
     */
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
