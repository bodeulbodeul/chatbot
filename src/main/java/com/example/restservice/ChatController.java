
package com.example.restservice;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.defaultSystem("""
                [역할]
                    당신은 회사의 '규정 담당 AI 비서'입니다.
                [규칙]
                   1. [규정]에 있는 내용이라면 상세하게 설명해 주세요.
                   2. 질문에 대한 정확한 답이 [규정]에 없더라도, **관련된 조항이 있다면 인용해서 안내**해 주세요.
                       (예: "연차 일수는 나와있지 않지만, 제3조에 따르면 휴가는 자유롭게 쓸 수 있습니다.")
                   3. [규정]과 전혀 관련 없는 질문일 경우에만 "죄송합니다, 규정에 없는 내용입니다."라고 답하세요.
                   4. 답변할 때는 가능한 한 근거가 되는 조항(예: 제3조)을 언급해주세요.
                   5. 당신의 사전 지식이나 일반 상식을 사용하지 마세요.
                """).build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "message") String message) {
        // 사용자 질문 검색
        List<Document> foundDocs = vectorStore.similaritySearch(SearchRequest.query(message).withTopK(2));

        if (foundDocs.isEmpty()) {
            return "관련된 규정을 찾을 수 없습니다.";
        }

        // 답변 데이터
        String context = foundDocs.stream().map(Document::getContent).collect(Collectors.joining("\n\n---\n\n"));
        String source = foundDocs.stream().map(doc -> (String) doc.getMetadata().get("filename")).distinct().collect(Collectors.joining(", "));

        // 답변 생성
        String promptText = """
                아래 [규정]을 보고 [질문]에 답변해 주세요.
                [규정]
                %s
                [답변]
                %s
                """.formatted(context, message);

        return chatClient.prompt()
                .user(promptText)
                .call()
                .content()
                + "\n\n[출처 :" + source + "]";
    }
}
