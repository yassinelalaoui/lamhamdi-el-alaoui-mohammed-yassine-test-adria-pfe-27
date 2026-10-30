package web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import domain.AccountNotFoundException;
import domain.InsufficientBalanceException;
import domain.InvalidAmountException;
import domain.Transaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import service.TransactionService;

@WebMvcTest(controllers = TransactionRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    @DisplayName("POST /api/transactions - 201 Created pour une transaction valide")
    void testCreateTransactionEndpointSuccess() throws Exception {
        UUID accountId = UUID.randomUUID();
        Transaction tx = new Transaction(
                accountId, new BigDecimal("250.00"), "EUR", "GROCERY", "FR", Instant.now());
        when(transactionService.createTransaction(any())).thenReturn(tx);

        CreateTransactionRequest req = new CreateTransactionRequest(
                accountId, new BigDecimal("250.00"), "EUR", "GROCERY", "FR");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("GET /api/transactions/{id} - 200 OK")
    void testGetTransactionById() throws Exception {
        UUID txId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Transaction tx = new Transaction(
                accountId, new BigDecimal("120.00"), "EUR", "RETAIL", "MA", Instant.now());
        when(transactionService.getTransaction(txId)).thenReturn(tx);

        mockMvc.perform(get("/api/transactions/" + txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.amount").value(120.00));
    }

    @Test
    @DisplayName("GET /api/transactions/account/{accountId} - 200 OK liste des transactions du compte")
    void testGetTransactionsByAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        List<Transaction> list = List.of(
                new Transaction(accountId, new BigDecimal("50.00"), "EUR", "FOOD", "MA", Instant.now()),
                new Transaction(accountId, new BigDecimal("75.00"), "EUR", "FUEL", "MA", Instant.now())
        );
        when(transactionService.getTransactionsByAccount(accountId)).thenReturn(list);

        mockMvc.perform(get("/api/transactions/account/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/transactions - 404 Not Found quand le compte n'existe pas")
    void testCreateTransactionAccountNotFound() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(transactionService.createTransaction(any()))
                .thenThrow(new AccountNotFoundException(accountId));

        CreateTransactionRequest req = new CreateTransactionRequest(
                accountId, new BigDecimal("100.00"), "EUR", "RETAIL", "MA");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Compte introuvable"));
    }

    @Test
    @DisplayName("POST /api/transactions - 400 Bad Request pour solde insuffisant")
    void testCreateTransactionInsufficientBalance() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(transactionService.createTransaction(any()))
                .thenThrow(new InsufficientBalanceException(accountId, new BigDecimal("10.00"), new BigDecimal("100.00")));

        CreateTransactionRequest req = new CreateTransactionRequest(
                accountId, new BigDecimal("100.00"), "EUR", "RETAIL", "MA");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solde insuffisant"));
    }
}

