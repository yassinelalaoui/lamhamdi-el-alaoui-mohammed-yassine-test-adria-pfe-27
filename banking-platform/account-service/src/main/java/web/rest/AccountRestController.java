package web.rest;

import domain.Account;
import domain.AccountStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountRestController {
    private final AccountService accountService;

    public AccountRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.toCommand());
        return ResponseEntity.created(URI.create("/api/accounts/" + account.getId()))
                .body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable("id") UUID id) {
        return AccountResponse.from(accountService.getAccount(id));
    }

    @GetMapping
    public List<AccountResponse> getAccounts(@RequestParam("customerReference") String customerReference) {
        return accountService.getAccountsByCustomer(customerReference).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @PatchMapping("/{id}")
    public AccountResponse updateAccount(
            @PathVariable("id") UUID id, @Valid @RequestBody UpdateAccountRequest request) {
        return AccountResponse.from(accountService.updateCustomerReference(id, request.toCommand()));
    }

    @GetMapping("/{id}/balance")
    public AccountBalanceResponse getBalance(@PathVariable("id") UUID id) {
        return AccountBalanceResponse.from(accountService.getBalance(id));
    }

    @PatchMapping("/{id}/status")
    public AccountResponse updateStatus(
            @PathVariable("id") UUID id, @Valid @RequestBody UpdateAccountStatusRequest request) {
        AccountStatus status = request.status();
        return AccountResponse.from(accountService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/balance")
    public BalanceAdjustmentResponse adjustBalance(
            @PathVariable("id") UUID id, @Valid @RequestBody BalanceAdjustmentRequest request) {
        return BalanceAdjustmentResponse.from(accountService.adjustBalance(id, request.toCommand()));
    }
}