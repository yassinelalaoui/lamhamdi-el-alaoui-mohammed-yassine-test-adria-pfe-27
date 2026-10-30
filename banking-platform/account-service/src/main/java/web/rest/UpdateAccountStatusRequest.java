package web.rest;

import domain.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(@NotNull AccountStatus status) {}