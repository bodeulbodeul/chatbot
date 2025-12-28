
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

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/ai/chat")
    public String chat(@RequestParam(value = "message") String message) {
        // 사용자 질문 검색
        List<Document> foundDocs = vectorStore.similaritySearch(SearchRequest.query(message).withTopK(1));

        if (foundDocs.isEmpty()) {
            return "관련된 규정을 찾을 수 없습니다.";
        }

        // 답변 데이터
        Document doc = foundDocs.get(0);
        String context = doc.getContent();
        String source = (String) doc.getMetadata().get("filename");

        // 답변 생성
        String promptText = """
                당신은 회사의 '규정 담당 AI 비서'입니다.
                반드시 아래 제공된 [규정]에 기반하여 사용자의 [질문]에 답변하세요.
                [규칙]
                1. **절대** [규정]에 없는 내용은 지어내지 마세요.
                2. [규정]에서 답을 찾을 수 없으면 솔직하게 "죄송합니다, 해당 내용은 규정에 나와있지 않습니다."라고 답변하세요.
                3. 답변할 때는 가능한 한 근거가 되는 조항(예: 제3조)을 언급해주세요.
                4. 당신의 사전 지식이나 일반 상식을 사용하지 마세요.
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
