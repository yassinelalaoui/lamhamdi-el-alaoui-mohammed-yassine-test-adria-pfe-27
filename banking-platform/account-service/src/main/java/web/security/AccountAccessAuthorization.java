package web.security;

import domain.Account;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import repository.AccountRepository;

@Component("accountAccessAuthorization")
public class AccountAccessAuthorization {
    private static final String READ_SCOPE = "SCOPE_accounts.read";
    private static final String ADMIN_READ_SCOPE = "SCOPE_accounts.admin.read";
    private static final String ADMIN_WRITE_SCOPE = "SCOPE_accounts.admin.write";

    private final AccountRepository accountRepository;
    private final TransactionServiceClientAuthorization transactionServiceAuthorization;

    public AccountAccessAuthorization(
            AccountRepository accountRepository,
            TransactionServiceClientAuthorization transactionServiceAuthorization) {
        this.accountRepository = accountRepository;
        this.transactionServiceAuthorization = transactionServiceAuthorization;
    }

    public boolean canReadCustomer(Authentication authentication, String customerReference) {
        if (!hasAuthority(authentication, READ_SCOPE)) {
            return false;
        }
        if (hasAuthority(authentication, ADMIN_READ_SCOPE)
                || transactionServiceAuthorization.isTransactionService(authentication)) {
            return true;
        }
        return customerReference != null
                && authentication instanceof JwtAuthenticationToken jwtAuthentication
                && customerReference.equals(jwtAuthentication.getToken().getClaimAsString("customer_reference"));
    }

    public boolean canCreateForCustomer(Authentication authentication, String customerReference) {
        if (!hasAuthority(authentication, "SCOPE_accounts.write")) {
            return false;
        }
        if (hasAuthority(authentication, ADMIN_WRITE_SCOPE)
                || transactionServiceAuthorization.isTransactionService(authentication)) {
            return true;
        }
        return customerReference != null
                && authentication instanceof JwtAuthenticationToken jwtAuthentication
                && customerReference.equals(jwtAuthentication.getToken().getClaimAsString("customer_reference"));
    }

    public boolean canReadAccount(Authentication authentication, UUID accountId) {
        if (!hasAuthority(authentication, READ_SCOPE)) {
            return false;
        }
        if (hasAuthority(authentication, ADMIN_READ_SCOPE)
                || transactionServiceAuthorization.isTransactionService(authentication)) {
            return true;
        }

        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return true;
        }
        return authentication instanceof JwtAuthenticationToken jwtAuthentication
                && account.getCustomerReference()
                        .equals(jwtAuthentication.getToken().getClaimAsString("customer_reference"));
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority::equals);
    }
}