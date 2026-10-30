package web.rest;

import domain.AccountBlockedException;
import domain.AccountNotFoundException;
import domain.BalanceOperationConflictException;
import domain.InsufficientFundsException;
import domain.InvalidAccountCommandException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(AccountNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Account not found", exception.getMessage());
    }

    @ExceptionHandler({
        AccountBlockedException.class,
        InsufficientFundsException.class,
        BalanceOperationConflictException.class
    })
    ResponseEntity<ProblemDetail> handleConflict(RuntimeException exception) {
        return problem(HttpStatus.CONFLICT, "Account operation conflict", exception.getMessage());
    }

    @ExceptionHandler(InvalidAccountCommandException.class)
    ResponseEntity<ProblemDetail> handleInvalidCommand(InvalidAccountCommandException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid account request", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Request validation failed", detail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request parameter", "A path or query parameter is invalid.");
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }
}