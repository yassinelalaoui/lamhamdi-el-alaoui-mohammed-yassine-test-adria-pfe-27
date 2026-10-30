package web.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import application.AccountServiceApplication;
import domain.AccountStatus;
import domain.BalanceOperationType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AccountServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountServiceIntegrationTest {
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
    void createsAndReadsAccountAndListsOnlyCustomerAccounts() throws Exception {
        String customerReference = "customer-42";
        String response = mockMvc.perform(post("/api/accounts")
                        .with(writeAsCustomer(customerReference))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateAccountRequest(customerReference, "eur"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accountId = objectMapper.readTree(response).get("id").asText();
        mockMvc.perform(get("/api/accounts/{id}", accountId).with(readAsCustomer(customerReference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerReference").value(customerReference));

        mockMvc.perform(get("/api/accounts")
                        .queryParam("customerReference", customerReference)
                        .with(readAsCustomer(customerReference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(accountId));
    }

    @Test
    void returnsNotFoundForMissingAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/{id}", UUID.randomUUID()).with(readAsCustomer("customer-42")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Account not found"));
    }

        @Test
        void customerCannotReadAnotherCustomersAccount() throws Exception {
                String accountId = createAccount("customer-1");

                mockMvc.perform(get("/api/accounts/{id}", accountId).with(readAsCustomer("customer-2")))
                                .andExpect(status().isForbidden());
        }

    @Test
    void updatesAllowedInformationAndBlocksThenUnblocksAccount() throws Exception {
        String accountId = createAccount("customer-before-update");

        mockMvc.perform(patch("/api/accounts/{id}", accountId)
                        .with(jwt().authorities(authority("SCOPE_accounts.admin.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAccountRequest("customer-after-update"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerReference").value("customer-after-update"));

        for (AccountStatus status : new AccountStatus[] {AccountStatus.BLOCKED, AccountStatus.ACTIVE}) {
            mockMvc.perform(patch("/api/accounts/{id}/status", accountId)
                            .with(jwt().authorities(authority("SCOPE_accounts.status.write")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateAccountStatusRequest(status))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(status.name()));
        }
    }

    @Test
    void transactionServiceCanAdjustBalanceIdempotentlyButOtherClientsCannot() throws Exception {
        String accountId = createAccount("customer-42");
        UUID operationReference = UUID.randomUUID();
        BalanceAdjustmentRequest credit = new BalanceAdjustmentRequest(
                operationReference, BalanceOperationType.CREDIT, new BigDecimal("100.00"), "EUR");

        mockMvc.perform(patch("/api/accounts/{id}/balance", accountId)
                        .with(transactionServiceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultingBalance").value(100))
                .andExpect(jsonPath("$.replayed").value(false));

        mockMvc.perform(patch("/api/accounts/{id}/balance", accountId)
                        .with(transactionServiceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultingBalance").value(100))
                .andExpect(jsonPath("$.replayed").value(true));

        BalanceAdjustmentRequest debit = new BalanceAdjustmentRequest(
                UUID.randomUUID(), BalanceOperationType.DEBIT, new BigDecimal("30.00"), "EUR");
        mockMvc.perform(patch("/api/accounts/{id}/balance", accountId)
                        .with(transactionServiceJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultingBalance").value(70));

        mockMvc.perform(get("/api/accounts/{id}/balance", accountId).with(readAsCustomer("customer-42")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70))
                .andExpect(jsonPath("$.currency").value("EUR"));

        mockMvc.perform(patch("/api/accounts/{id}/balance", accountId)
                        .with(jwt().authorities(authority("SCOPE_accounts.balance.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(debit)))
                .andExpect(status().isForbidden());
    }

    @Test
    void graphqlMutationAndQueryUseAccountService() throws Exception {
        String mutation = "mutation { createAccount(input: {customerReference: \"graphql-customer\", currency: \"USD\"}) { id status currency balance } }";
        String response = mockMvc.perform(post("/graphql")
                        .with(writeAsCustomer("graphql-customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GraphqlRequest(mutation))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createAccount.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accountId = objectMapper.readTree(response).path("data").path("createAccount").path("id").asText();
        String query = "query { accountBalance(id: \"" + accountId + "\") { id currency balance } }";
        mockMvc.perform(post("/graphql")
                        .with(readAsCustomer("graphql-customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GraphqlRequest(query))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountBalance.currency").value("USD"));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .with(jwt().authorities(authority("SCOPE_accounts.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerReference\":\"\",\"currency\":\"USDX\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Request validation failed"));
    }

        @Test
        void customerCannotCreateAnAccountForAnotherCustomer() throws Exception {
                mockMvc.perform(post("/api/accounts")
                                                .with(writeAsCustomer("customer-1"))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(new CreateAccountRequest("customer-2", "EUR"))))
                                .andExpect(status().isForbidden());
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
                .authorities(authority("SCOPE_accounts.read"));
    }

        private org.springframework.test.web.servlet.request.RequestPostProcessor writeAsCustomer(String customerReference) {
                return jwt().jwt(token -> token.claim("customer_reference", customerReference))
                                .authorities(authority("SCOPE_accounts.write"));
        }

    private org.springframework.test.web.servlet.request.RequestPostProcessor transactionServiceJwt() {
        return jwt().jwt(token -> token.claim("azp", "transaction-service"))
                .authorities(authority("SCOPE_accounts.balance.write"));
    }

    private SimpleGrantedAuthority authority(String value) {
        return new SimpleGrantedAuthority(value);
    }

    private record GraphqlRequest(String query) {}
}