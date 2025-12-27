package com.example.restservice;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    //    AI가 이해할 수 있는 Vector 형태로 데이터를 저장하는 저장소 생성
    @Bean
    public VectorStore vectorStore(EmbeddingModel model) {
        return new SimpleVectorStore(model);
    }
}
