package web;

import agent.BankingAgentService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatRestController {

    private final BankingAgentService agentService;

    public ChatRestController(BankingAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<BankingAgentService.AgentResponse> chat(
            @RequestBody ChatRequest request) {
        BankingAgentService.AgentResponse response = agentService.processMessage(request.message());
        return ResponseEntity.ok(response);
    }

    public record ChatRequest(String message, String conversationId) {}
}
