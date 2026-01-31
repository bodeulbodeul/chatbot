package com.example.restservice.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    // 기억저장소 등록
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    // 기억력 Advisor 추가
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder.defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory)
        ).build();
    }
}
