package web.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import service.CreateAccountCommand;

public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String customerReference,
        @NotBlank @Pattern(regexp = "(?i)^[A-Z]{3}$") String currency) {
    public CreateAccountCommand toCommand() {
        return new CreateAccountCommand(customerReference, currency);
    }
}