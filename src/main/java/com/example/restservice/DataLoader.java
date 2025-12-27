package com.example.restservice;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader {

    private final VectorStore vectorStore;

    @Value("classpath:regulations.txt")
    private Resource requlation;

    public DataLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void load() {

        // 테스트 파일 읽기
        TextReader textReader = new TextReader(requlation);

        // 메타데이터 추가
        textReader.getCustomMetadata().put("filename", "regulations.txt");

        List<Document> documents = textReader.get();

        // 크기 자르기
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(documents);

        // 저장소에 데이터 적재 (비용발생)
        vectorStore.add(documents);
        System.out.println("✅ 'regulations.txt' 파일 내용을 성공적으로 학습했습니다!");
    }
}
