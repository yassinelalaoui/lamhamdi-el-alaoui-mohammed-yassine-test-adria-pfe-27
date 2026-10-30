package agent;

import client.AccountServiceClient;
import client.TransactionServiceClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.BankingTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankingAgentServiceTest {

    @Mock
    private AccountServiceClient accountClient;

    @Mock
    private TransactionServiceClient transactionClient;

    private BankingAgentService agentService;

    @BeforeEach
    void setUp() {
        BankingTools tools = new BankingTools(accountClient, transactionClient);
        agentService = new BankingAgentService(tools);
    }

    @Test
    void processesBalanceQuerySuccessfully() {
        UUID accountId = UUID.randomUUID();
        when(accountClient.getAccount(accountId)).thenReturn(
                new AccountServiceClient.AccountResponse(accountId, "CUST-100", "ACTIVE", "EUR", new BigDecimal("1250.50")));

        BankingAgentService.AgentResponse response =
                agentService.processMessage("Quel est le solde du compte " + accountId + " ?");

        assertEquals("GET_ACCOUNT", response.intent());
        assertTrue(response.message().contains("1250.50 EUR"));
        assertTrue(response.message().contains("ACTIVE"));
        verify(accountClient).getAccount(accountId);
    }

    @Test
    void processesTransferQuerySuccessfully() {
        UUID srcId = UUID.randomUUID();
        UUID dstId = UUID.randomUUID();

        List<TransactionServiceClient.TransactionResponse> txList = List.of(
                new TransactionServiceClient.TransactionResponse(UUID.randomUUID(), srcId, new BigDecimal("100"), "EUR", "TRANSFER_OUT", "INTERNAL", Instant.now()),
                new TransactionServiceClient.TransactionResponse(UUID.randomUUID(), dstId, new BigDecimal("100"), "EUR", "TRANSFER_IN", "INTERNAL", Instant.now())
        );
        when(transactionClient.createTransfer(any())).thenReturn(txList);

        BankingAgentService.AgentResponse response = agentService.processMessage(
                "Effectuer un virement de 100 EUR de " + srcId + " vers " + dstId);

        assertEquals("TRANSFER_COMPLETED", response.intent());
        assertTrue(response.message().contains("succès"));
        verify(transactionClient).createTransfer(any());
    }

    @Test
    void processesHelpQueryOnEmptyMessage() {
        BankingAgentService.AgentResponse response = agentService.processMessage("");
        assertEquals("HELP", response.intent());
        assertTrue(response.message().contains("assistant bancaire"));
    }
}
