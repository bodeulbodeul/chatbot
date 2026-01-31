package com.example.restservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
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

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    @Value("classpath:prompts/rag-prompt.st")
    private Resource ragPrompt;

    public String chat(String message) {
        // 검색 조건 설정
        String queryMessage= transformQuery("user1", message);
        SearchRequest searchRequest = SearchRequest
//                .query(transformQuery("user1", message))
                .query(queryMessage)
                .withTopK(3) // 개수
                .withSimilarityThreshold(0.0); // 유사도

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
                "message", queryMessage
        ));

        return chatClient.prompt()
                .user(promptText)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, "user1") // 사용자별ID
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)) // 최근 10마디 기억
                .call()
                .content();
    }

    private String transformQuery(String conversationId, String message) {
        // 이전 대화 가져오기
        List<Message> history = chatMemory.get(conversationId, 3);

        if (history.isEmpty()) {
            return message;
        }

        // 질문 재구성
        String historyText = history.stream()
                .map(m -> m.getMessageType() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                당신은 '검색어 최적화 도구'입니다. 질문에 답변하지 마십시오.
                아래 [대화 내역]을 참고하여, [사용자 질문]의 의도를 명확하게 파악한 '검색용 질문'을 만드세요.
                
                [대화 내역]
                %s
                
                [사용자 질문]
                %s
                
                [지시사항]
                1. 대명사(이것, 저것)나 생략된 주어(휴가, 급여 등)를 찾아서 복원하세요.
                2. 답변이나 설명을 포함하지 말고, 오직 '하나의 질문 문장'만 출력하세요.
                3. 반드시 물음표(?)로 끝나야 합니다.
                
                Example:
                사용자: 안 쓰면 줘?
                출력: 미사용 연차는 수당으로 지급되나요?
                """, historyText, message);

        return chatClient.prompt().user(prompt).call().content();
    }

}
