package web.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("transactionServiceClientAuthorization")
public class TransactionServiceClientAuthorization {
    private final String transactionServiceClientId;

    public TransactionServiceClientAuthorization(
            @Value("${security.transaction-service-client-id:transaction-service}") String transactionServiceClientId) {
        this.transactionServiceClientId = transactionServiceClientId;
    }

    public boolean isTransactionService(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }

        String authorizedParty = jwtAuthentication.getToken().getClaimAsString("azp");
        String clientId = jwtAuthentication.getToken().getClaimAsString("client_id");
        return transactionServiceClientId.equals(authorizedParty) || transactionServiceClientId.equals(clientId);
    }
}