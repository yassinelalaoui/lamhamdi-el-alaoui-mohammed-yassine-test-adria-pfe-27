package web.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import service.UpdateAccountCommand;

public record UpdateAccountRequest(@NotBlank @Size(max = 100) String customerReference) {
    public UpdateAccountCommand toCommand() {
        return new UpdateAccountCommand(customerReference);
    }
}