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
import java.util.Objects;

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
//        loadText();
//        loadPdf();
    }

    public void loadText() {

        if (!textResource.exists()) {
            System.out.println("text 규정집이 없습니다.");
            return;
        }

        // 테스트 파일 읽기
        TextReader textReader = new TextReader(textResource);

        // 메타데이터 추가
        String orgFileName = stripExtension(Objects.requireNonNull(textResource.getFilename()));
        textReader.getCustomMetadata().put("filename", orgFileName);

        List<Document> documents = textReader.get();

        // 저장소에 데이터 적재 (비용발생)
        vectorStore.add(getSplitDocuments(documents));
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
            String orgFileName = stripExtension((Objects.requireNonNull(pdfResource.getFilename())));
            doc.getMetadata().put("filename", orgFileName);

            String content = doc.getContent();
            String cleanContent = content.replaceAll("\\s+", " ").trim();

            Document newDoc = new Document(cleanContent, doc.getMetadata());

            cleanDocuments.add(newDoc);
        }

        vectorStore.add(getSplitDocuments(cleanDocuments));
        System.out.println(pdfResource.getFilename() + "파일 내용을 성공적으로 학습했습니다!");
    }

    private List<Document> getSplitDocuments(List<Document> docs) {
        TokenTextSplitter splitter = new TokenTextSplitter(150, 50, 5, 10000, true);
        return splitter.apply(docs);
    }

    // 확장자 제거 헬퍼 메서드
    private String stripExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(0, lastDotIndex);
        }
        return filename;
    }
}
