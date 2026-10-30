package web.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration that injects a static service-to-service bearer token from configuration.
 *
 * <p>In production the token would be obtained from Keycloak client-credentials flow. For the exam
 * context a pre-configured static token is accepted.
 */
public class AccountServiceFeignConfig {

    @Bean
    RequestInterceptor serviceAuthInterceptor(
            @Value("${bank.internal.service-token:}") String serviceToken) {
        return template -> {
            if (serviceToken != null && !serviceToken.isBlank()) {
                template.header("Authorization", "Bearer " + serviceToken);
            }
        };
    }
}
