
package com.example.restservice.controller;

import com.example.restservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ChatController {

    private final ChatService service;

    @GetMapping("/ai/chat")
    public String chat(@RequestParam("message") String message) {
        return service.chat(message);
    }
}
