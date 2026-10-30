package telegram;

import agent.BankingAgentService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Telegram Bot integration webhook controller.
 *
 * <p>Receives updates from Telegram and replies with AI agent responses.
 */
@RestController
@RequestMapping("/api/chat/telegram")
public class TelegramBotService {
    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    private final BankingAgentService agentService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot.token:}")
    private String botToken;

    public TelegramBotService(BankingAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleTelegramUpdate(@RequestBody TelegramUpdate update) {
        if (update != null && update.message() != null && update.message().text() != null) {
            long chatId = update.message().chat().id();
            String userText = update.message().text();
            log.info("Received Telegram message from chatId={}: {}", chatId, userText);

            BankingAgentService.AgentResponse response = agentService.processMessage(userText);
            sendTelegramMessage(chatId, response.message());
        }
        return ResponseEntity.ok().build();
    }

    public void sendTelegramMessage(long chatId, String text) {
        if (botToken == null || botToken.isBlank()) {
            log.info("Telegram Bot token not configured. Response text for chatId {}: {}", chatId, text);
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            Map<String, Object> payload = Map.of(
                    "chat_id", chatId,
                    "text", text);
            restTemplate.postForObject(url, payload, String.class);
        } catch (Exception e) {
            log.error("Failed to send message to Telegram: {}", e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramUpdate(
            @JsonProperty("update_id") long updateId,
            @JsonProperty("message") TelegramMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramMessage(
            @JsonProperty("message_id") long messageId,
            @JsonProperty("chat") TelegramChat chat,
            @JsonProperty("text") String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramChat(@JsonProperty("id") long id) {}
}

