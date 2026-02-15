package com.example.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    @Value("classpath:prompts/rag-prompt.st")
    private Resource ragPrompt;

    @Value("classpath:prompts/query-transformer.st")
    private Resource queryTraResource;

    public String chat(String message) {
        // 검색 조건 설정
        String queryMessage = transformQuery("user1", message);
        SearchRequest searchRequest = SearchRequest.builder()
                .query(queryMessage)
                .topK(3) // 개수
                .similarityThreshold(0.0)
                .build(); // 유사도

        // 사용자 질문 벡터 검색
        List<Document> foundDocs = vectorStore.similaritySearch(searchRequest);

        String context = foundDocs.stream()
                .map(doc -> {
                    String fileName = (String) doc.getMetadata().getOrDefault("filename", "참고문서");
                    return String.format("""
                            [문서: %s]
                            %s
                            """, fileName, doc.getText());
                }).collect(Collectors.joining("\n\n---\n\n"));

        // 프롬프트 렌더링
        PromptTemplate promptTemplate = new PromptTemplate(ragPrompt);
        String promptText = promptTemplate.render(Map.of(
                "context", context,
                "message", queryMessage
        ));


        ChatClientResponse res = chatClient.prompt()
                .user(promptText)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user1"))
                .call()
                .chatClientResponse();

        return res.chatResponse().getResult().getOutput().getText();
    }

    private String transformQuery(String conversationId, String message) {
        // 이전 대화 가져오기
        List<Message> history = chatMemory.get(conversationId);

        if (history.isEmpty()) {
            return message;
        }

        // 질문 재구성
        String historyText = history.stream()
                .map(m -> m.getMessageType() + ": " + m.getText())
                .collect(Collectors.joining("\n"));

        PromptTemplate promptTemplate = new PromptTemplate(queryTraResource);
        String prompt = promptTemplate.render(Map.of(
                "history", historyText,
                "message", message
        ));

        return chatClient.prompt().user(prompt).call().content();
    }

}
