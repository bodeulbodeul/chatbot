package com.example.restservice;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataLoader {

    private final VectorStore vectorStore;

    @Value("classpath:regulations.txt")
    private Resource textResource;

    @Value("classpath:rule.pdf")
    private Resource pdfResource;

    public DataLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        loadText();
        loadPdf();
    }

    public void loadText() {

        if (!textResource.exists()) {
            System.out.println("text 규정집이 없습니다.");
            return;
        }

        // 테스트 파일 읽기
        TextReader textReader = new TextReader(textResource);

        // 메타데이터 추가
        textReader.getCustomMetadata().put("filename", textResource.getFilename());

        List<Document> documents = textReader.get();

        // 크기 자르기
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(documents);

        // 저장소에 데이터 적재 (비용발생)
        vectorStore.add(splitDocuments);
        System.out.println(textResource.getFilename() + "파일 내용을 성공적으로 학습했습니다!");
    }

    public void loadPdf() {
        if (!pdfResource.exists()) {
            System.out.println("PDF 규정집이 없습니다.");
            return;
        }

        // PDF 파일 읽기
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                pdfResource, PdfDocumentReaderConfig.builder().withPagesPerDocument(1).build()
        );

        List<Document> cleanDocuments = new ArrayList<>();

        // 메타데이터 추가
        for (Document doc : pdfReader.get()) {
            doc.getMetadata().put("filename", pdfResource.getFilename());

            String content = doc.getContent();
            String cleanContent = content.replaceAll("\\s+", " ").trim();

            Document newDoc = new Document(cleanContent, doc.getMetadata());

            cleanDocuments.add(newDoc);
        }

        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(cleanDocuments);

        vectorStore.add(splitDocuments);
        System.out.println(pdfResource.getFilename() + "파일 내용을 성공적으로 학습했습니다!");
    }
}
