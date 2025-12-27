
package com.example.restservice;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "Hi") String message) {

        // ask to ai
        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();

        return response;
    }
}
