package com.knowlearnmap.rag.service;

import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.domain.DocumentPage;
import com.knowlearnmap.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 간단한 청킹 서비스
 * 페이지를 고정 크기로 나눔
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimpleChunkingService {

    private static final int CHUNK_SIZE = 1000; // 문자 수
    private static final int OVERLAP = 200; // 겹치는 부분

    private final DocumentRepository documentRepository;

    /**
     * 페이지들을 청크로 나눔
     * 
     * @param pages      문서 페이지 목록
     * @param documentId 문서 ID
     * @return 생성된 청크 목록
     */
    public List<DocumentChunk> chunkPages(List<DocumentPage> pages, Long documentId) {
        log.info("청킹 시작: {} 페이지", pages.size());

        // 문서 조회
        log.debug("Document 조회 시작: documentId={}", documentId);
        var document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        log.debug("Document 조회 완료: documentId={}", documentId);

        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        log.debug("페이지 순회 시작: {} 페이지", pages.size());
        for (DocumentPage page : pages) {
            log.debug("페이지 {} 처리 중", page.getPageNumber());
            String content = page.getContent();
            if (content == null || content.trim().isEmpty()) {
                log.debug("페이지 {} 내용 없음, 건너뜀", page.getPageNumber());
                continue;
            }

            log.debug("페이지 {} 내용 길이: {} 문자", page.getPageNumber(), content.length());

            // 페이지 내용을 청크로 분할
            int start = 0;
            int pageChunkCount = 0;
            int lastEnd = 0; // 무한 루프 방지용

            while (start < content.length()) {
                int end = Math.min(start + CHUNK_SIZE, content.length());

                // 문장 경계에서 자르기 (마침표, 느낌표, 물음표)
                if (end < content.length()) {
                    int lastPeriod = content.lastIndexOf('.', end);
                    int lastExclamation = content.lastIndexOf('!', end);
                    int lastQuestion = content.lastIndexOf('?', end);
                    int boundary = Math.max(Math.max(lastPeriod, lastExclamation), lastQuestion);

                    if (boundary > start + CHUNK_SIZE / 2) {
                        end = boundary + 1;
                    }
                }

                // 무한 루프 방지: 같은 위치를 반복하지 않도록
                if (end <= lastEnd) {
                    log.warn("청킹 무한 루프 감지 (페이지 {}): start={}, end={}, lastEnd={}",
                            page.getPageNumber(), start, end, lastEnd);
                    break;
                }
                lastEnd = end;

                String chunkContent = content.substring(start, end).trim();

                if (!chunkContent.isEmpty()) {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocument(document); // document 객체 설정
                    chunk.setPageNumber(page.getPageNumber());
                    chunk.setChunkIndex(chunkIndex++);
                    chunk.setContent(chunkContent);

                    chunks.add(chunk);
                    pageChunkCount++;
                }

                // end가 content 끝에 도달하면 종료
                if (end >= content.length()) {
                    break;
                }

                // 다음 청크 시작 위치 (겹침 고려)
                start = end - OVERLAP;
                if (start < 0) {
                    start = 0;
                }
            }
            log.debug("페이지 {} 청킹 완료: {} 청크 생성", page.getPageNumber(), pageChunkCount);
        }

        log.info("청킹 완료: {} 청크 생성", chunks.size());
        return chunks;
    }
}
