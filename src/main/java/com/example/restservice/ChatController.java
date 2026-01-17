
package com.example.restservice;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
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
        this.vectorStore = vectorStore;
        this.chatClient = builder
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam("message") String message, @RequestParam(value = "filename", required = false) String filename) {

        // 검색 조건 설정
        SearchRequest searchRequest = SearchRequest
                .query(message)
                .withTopK(3) // 개수
                .withSimilarityThreshold(0.2); // 유사도

        if (StringUtils.isNotBlank(filename)) {
            searchRequest.withFilterExpression(
                    new FilterExpressionBuilder()
                            .eq("filename", filename)
                            .build()
            );
        }

        // 사용자 질문 벡터 검색
        List<Document> foundDocs = vectorStore.similaritySearch(searchRequest);

        String context = foundDocs.stream()
                .map(doc -> {
                    String fileName = (String) doc.getMetadata().getOrDefault("filename", "참고문서");
                    return String.format("""
                            [문서: %s]
                            %s
                            """, fileName, doc.getContent());
                }).collect(Collectors.joining("\n\n---\n\n"));

        // 프롬프트 렌더링
        PromptTemplate promptTemplate = new PromptTemplate(ragPrompt);
        String promptText = promptTemplate.render(Map.of(
                "context", context,
                "message", message
        ));

        return chatClient.prompt()
                .user(promptText)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, "user1"))
                .call()
                .content();
    }
}
