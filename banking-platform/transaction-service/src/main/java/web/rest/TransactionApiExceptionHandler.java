package web.rest;

import domain.AccountNotActiveException;
import domain.AccountNotFoundException;
import domain.AccountServiceUnavailableException;
import domain.CurrencyMismatchException;
import domain.InsufficientBalanceException;
import domain.InvalidAmountException;
import domain.InvalidTransactionException;
import domain.MaxAmountExceededException;
import domain.TransactionNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransactionApiExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAccountNotFound(AccountNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Compte introuvable");
        problem.setType(URI.create("https://adria.banking/errors/account-not-found"));
        problem.setProperty("accountId", exception.getAccountId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleTransactionNotFound(TransactionNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Transaction introuvable");
        problem.setType(URI.create("https://adria.banking/errors/transaction-not-found"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ProblemDetail> handleAccountNotActive(AccountNotActiveException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Compte inactif ou bloqué");
        problem.setType(URI.create("https://adria.banking/errors/account-not-active"));
        problem.setProperty("accountId", exception.getAccountId());
        problem.setProperty("currentStatus", exception.getStatus());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<ProblemDetail> handleCurrencyMismatch(CurrencyMismatchException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Incompatibilité de devises");
        problem.setType(URI.create("https://adria.banking/errors/currency-mismatch"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientBalance(InsufficientBalanceException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Solde insuffisant");
        problem.setType(URI.create("https://adria.banking/errors/insufficient-balance"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MaxAmountExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxAmountExceeded(MaxAmountExceededException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Plafond de transaction dépassé");
        problem.setType(URI.create("https://adria.banking/errors/max-amount-exceeded"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAmount(InvalidAmountException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Montant invalide");
        problem.setType(URI.create("https://adria.banking/errors/invalid-amount"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTransaction(InvalidTransactionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Requête de transaction invalide");
        problem.setType(URI.create("https://adria.banking/errors/invalid-transaction"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(AccountServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleServiceUnavailable(AccountServiceUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setTitle("Communication Account-Service indisponible");
        problem.setType(URI.create("https://adria.banking/errors/account-service-unavailable"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> violations = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> violations.put(error.getField(), error.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "La validation de la requête a échoué.");
        problem.setTitle("Données de requête invalides");
        problem.setType(URI.create("https://adria.banking/errors/validation-failed"));
        problem.setProperty("violations", violations);
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }
}
