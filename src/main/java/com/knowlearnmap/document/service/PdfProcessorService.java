package com.knowlearnmap.document.service;

import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.domain.DocumentEntity;
import com.knowlearnmap.document.domain.DocumentPage;
import com.knowlearnmap.document.repository.DocumentChunkRepository;
import com.knowlearnmap.document.repository.DocumentPageRepository;
import com.knowlearnmap.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 처리 서비스
 * - PDF 텍스트 추출
 * - DocumentPage 생성 (페이지별)
 * - DocumentChunk 생성 (청킹)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfProcessorService {

    private final DocumentRepository documentRepository;
    private final DocumentPageRepository documentPageRepository;
    private final DocumentChunkRepository documentChunkRepository;

    private static final int CHUNK_SIZE = 1000; // 청크 크기 (문자 수)
    private static final int CHUNK_OVERLAP = 200; // 청크 오버랩

    /**
     * PDF 파일 처리 (전체 파이프라인)
     */
    @Transactional
    public void processPdf(Long documentId) {
        log.info("PDF 처리 시작: documentId={}", documentId);

        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다: " + documentId));

        try {
            // 1. 상태 업데이트
            document.setStatus(DocumentEntity.IngestionStatus.PROCESSING);
            documentRepository.save(document);

            // 2. PDF 파일 열기
            File pdfFile = new File(document.getFilePath());
            if (!pdfFile.exists()) {
                throw new IOException("PDF 파일을 찾을 수 없습니다: " + document.getFilePath());
            }

            // 3. PDF 페이지별 텍스트 추출 및 DocumentPage 생성
            List<DocumentPage> pages = extractPagesFromPdf(document, pdfFile);
            documentPageRepository.saveAll(pages);
            log.info("DocumentPage 생성 완료: {} 페이지", pages.size());

            // 4. DocumentChunk 생성 (페이지별 청킹)
            List<DocumentChunk> allChunks = new ArrayList<>();
            int globalChunkIndex = 0;

            for (DocumentPage page : pages) {
                List<DocumentChunk> pageChunks = createChunksFromPage(document, page, globalChunkIndex);
                allChunks.addAll(pageChunks);
                globalChunkIndex += pageChunks.size();
            }

            documentChunkRepository.saveAll(allChunks);
            log.info("DocumentChunk 생성 완료: {} 청크", allChunks.size());

            // 5. 상태 업데이트
            document.setStatus(DocumentEntity.IngestionStatus.COMPLETED);
            documentRepository.save(document);

            log.info("PDF 처리 완료: documentId={}, pages={}, chunks={}",
                    documentId, pages.size(), allChunks.size());

        } catch (Exception e) {
            log.error("PDF 처리 실패: documentId={}", documentId, e);
            document.setStatus(DocumentEntity.IngestionStatus.FAILED);
            documentRepository.save(document);
            throw new RuntimeException("PDF 처리 실패: " + e.getMessage(), e);
        }
    }

    /**
     * PDF에서 페이지별 텍스트 추출
     */
    private List<DocumentPage> extractPagesFromPdf(DocumentEntity document, File pdfFile) throws IOException {
        List<DocumentPage> pages = new ArrayList<>();

        try (PDDocument pdDocument = Loader.loadPDF(pdfFile)) {
            int totalPages = pdDocument.getNumberOfPages();
            log.debug("PDF 총 페이지 수: {}", totalPages);

            PDFTextStripper stripper = new PDFTextStripper();

            for (int i = 1; i <= totalPages; i++) {
                try {
                    // 페이지 범위 설정
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);

                    // 텍스트 추출
                    String pageText = stripper.getText(pdDocument);

                    // DocumentPage 생성
                    DocumentPage page = new DocumentPage(
                            document,
                            i,
                            pageText,
                            DocumentPage.PageStatus.COMPLETED);
                    pages.add(page);

                    log.debug("페이지 {} 추출 완료: {} 문자", i, pageText.length());

                } catch (Exception e) {
                    log.error("페이지 {} 추출 실패", i, e);
                    // 실패한 페이지도 기록
                    DocumentPage failedPage = new DocumentPage(
                            document,
                            i,
                            null,
                            DocumentPage.PageStatus.FAILED);
                    failedPage.setErrorMessage(e.getMessage());
                    pages.add(failedPage);
                }
            }
        }

        return pages;
    }

    /**
     * 페이지 텍스트를 청크로 분할
     */
    private List<DocumentChunk> createChunksFromPage(
            DocumentEntity document,
            DocumentPage page,
            int startChunkIndex) {

        List<DocumentChunk> chunks = new ArrayList<>();

        if (page.getContent() == null || page.getContent().isEmpty()) {
            return chunks; // 빈 페이지는 청크 생성 안함
        }

        String content = page.getContent();
        int chunkIndex = startChunkIndex;
        int start = 0;

        while (start < content.length()) {
            // 청크 끝 위치 계산
            int end = Math.min(start + CHUNK_SIZE, content.length());

            // 청크 추출
            String chunkContent = content.substring(start, end);

            // DocumentChunk 생성
            DocumentChunk chunk = new DocumentChunk(
                    document,
                    chunkContent,
                    chunkIndex++,
                    page.getPageNumber());
            chunks.add(chunk);

            // 다음 시작 위치 (오버랩 적용)
            start = end - CHUNK_OVERLAP;
            if (start >= content.length()) {
                break;
            }
        }

        log.debug("페이지 {} 청킹 완료: {} 청크", page.getPageNumber(), chunks.size());
        return chunks;
    }
}
