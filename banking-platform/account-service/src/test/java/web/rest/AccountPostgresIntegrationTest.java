package web.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import application.AccountServiceApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.BalanceOperationType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = AccountServiceApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class AccountPostgresIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("account_service_test")
            .withUsername("account_test")
            .withPassword("account_test");

    @DynamicPropertySource
    static void configureSecurity(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:9999/realms/test");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM account_balance_operations");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    void appliesFlywayMigrationAndPersistsAccountInPostgres() throws Exception {
        String customerReference = "postgres-customer";
        String accountId = createAccount(customerReference);

        mockMvc.perform(get("/api/accounts/{id}", accountId).with(readAsCustomer(customerReference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerReference").value(customerReference))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance").value(0));

        Integer accountCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accounts WHERE id = ?::uuid", Integer.class, accountId);
        org.junit.jupiter.api.Assertions.assertEquals(1, accountCount);
    }

    @Test
    void serializesConcurrentCreditsWithoutLosingBalanceUpdates() throws Exception {
        String accountId = createAccount("concurrent-customer");
        int operationCount = 12;
        var executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Void>> operations = new ArrayList<>();
            for (int index = 0; index < operationCount; index++) {
                operations.add(() -> {
                    BalanceAdjustmentRequest credit = new BalanceAdjustmentRequest(
                            UUID.randomUUID(), BalanceOperationType.CREDIT, new BigDecimal("1.00"), "EUR");
                    mockMvc.perform(patch("/api/accounts/{id}/balance", accountId)
                                    .with(transactionServiceJwt())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(credit)))
                            .andExpect(status().isOk());
                    return null;
                });
            }
            executor.invokeAll(operations).forEach(future -> {
                try {
                    future.get();
                } catch (Exception exception) {
                    throw new IllegalStateException("Concurrent PostgreSQL balance operation failed.", exception);
                }
            });
        } finally {
            executor.shutdownNow();
        }

        mockMvc.perform(get("/api/accounts/{id}/balance", accountId).with(readAsCustomer("concurrent-customer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(operationCount));

        Integer operationCountInDatabase = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_balance_operations WHERE account_id = ?::uuid", Integer.class, accountId);
        org.junit.jupiter.api.Assertions.assertEquals(operationCount, operationCountInDatabase);
    }

    private String createAccount(String customerReference) throws Exception {
        String response = mockMvc.perform(post("/api/accounts")
                        .with(writeAsCustomer(customerReference))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest(customerReference, "EUR"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor readAsCustomer(String customerReference) {
        return jwt().jwt(token -> token.claim("customer_reference", customerReference))
                .authorities(new SimpleGrantedAuthority("SCOPE_accounts.read"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor writeAsCustomer(String customerReference) {
        return jwt().jwt(token -> token.claim("customer_reference", customerReference))
                .authorities(new SimpleGrantedAuthority("SCOPE_accounts.write"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor transactionServiceJwt() {
        return jwt().jwt(token -> token.claim("azp", "transaction-service"))
                .authorities(new SimpleGrantedAuthority("SCOPE_accounts.balance.write"));
    }
}