package com.knowlearnmap.rag.service;

import com.knowlearnmap.document.domain.DocumentEntity;
import com.knowlearnmap.document.domain.DocumentPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 파싱 서비스
 */
@Service
@Slf4j
public class PdfParsingService {

    /**
     * PDF 파일에서 페이지별로 텍스트 추출
     * 
     * @param document 문서 엔티티
     * @return 추출된 페이지 목록
     * @throws IOException PDF 파싱 실패 시
     */
    public List<DocumentPage> extractPages(DocumentEntity document) throws IOException {
        log.info("PDF 파싱 시작: {}", document.getFilePath());

        List<DocumentPage> pages = new ArrayList<>();
        File pdfFile = new File(document.getFilePath());

        try (PDDocument pdDocument = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = pdDocument.getNumberOfPages();

            log.info("총 페이지 수: {}", totalPages);

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);

                String rawText = stripper.getText(pdDocument);
                String cleanedText = cleanText(rawText);

                if (cleanedText != null && !cleanedText.trim().isEmpty()) {
                    DocumentPage page = new DocumentPage();
                    page.setDocument(document);
                    page.setPageNumber(pageNum);
                    page.setContent(cleanedText);
                    page.setStatus(DocumentPage.PageStatus.COMPLETED);

                    pages.add(page);
                    log.debug("페이지 {} 추출 완료", pageNum);
                }
            }

            log.info("PDF 파싱 완료: {} 페이지 추출", pages.size());
            return pages;

        } catch (IOException e) {
            log.error("PDF 파싱 실패: {}", document.getFilePath(), e);
            throw e;
        }
    }

    /**
     * 단어 수 계산
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /**
     * 텍스트 정제 (간단한 처리)
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replaceAll("\\s+", " ") // 여러 공백을 하나로
                .replaceAll("[\\r\\n]+", "\n") // 여러 줄바꿈을 하나로
                .trim();
    }
}
