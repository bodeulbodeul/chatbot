package com.example.restservice;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader {

    private final VectorStore vectorStore;

    public DataLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void load() {
        // 테스트용 사내 규정 데이터 생성
        List<Document> documents = List.of(
                new Document("제1조(목적) 이 규정은 우리 회사의 휴가 사용 및 복지에 관한 사항을 규정함을 목적으로 한다."),
                new Document("제2조(연차) 신입 사원은 입사 후 1년간 매월 1일의 유급 휴가가 발생한다."),
                new Document("제3조(재택근무) 주 2회 재택근무가 가능하며, 미리 팀장 승인을 받아야 한다."),
                new Document("제4조(식대) 점심 식대는 월 20만원까지 법인카드로 지원한다.")
        );

        // 저장소에 데이터 적재 (비용발생)
        vectorStore.add(documents);
        System.out.println("✅ 규정 데이터가 Vector Store에 로딩되었습니다.");
    }
}
