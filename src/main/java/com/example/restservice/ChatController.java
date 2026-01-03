
package com.example.restservice;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("classpath:prompts/rag-prompt.st")
    private Resource ragPrompt;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "message") String message) {
        // 사용자 질문 벡터 검색
        List<Document> foundDocs = vectorStore.similaritySearch(SearchRequest.query(message).withTopK(2));

        // 답변 데이터
        String context = foundDocs.stream().map(Document::getContent).collect(Collectors.joining("\n\n---\n\n"));
        // 참조 규정집 목록
        String sourceList = foundDocs.stream()
                .map(doc -> (String) doc.getMetadata().getOrDefault("filename", "unknown"))
                .distinct()
                .collect(Collectors.joining(", "));

        PromptTemplate promptTemplate = new PromptTemplate(ragPrompt);
        String promptText = promptTemplate.render(Map.of(
                "context", context,
                "message", message,
                "source_list", sourceList
        ));

        return chatClient.prompt()
                .user(promptText)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, "user1"))
                .call()
                .content();
    }
}
