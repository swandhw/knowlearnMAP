package com.knowlearnmap.chunkToLlm.controller;

import com.knowlearnmap.chunkToLlm.service.ChunkToLlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chunk-to-llm")
@RequiredArgsConstructor
@Slf4j
public class ChunkToLlmController {

    private final ChunkToLlmService chunkToLlmService;

    /**
     * 특정 문서의 청크를 LLM으로 처리 (테스트용)
     * 
     * @param documentId 문서 ID
     * @param limit      처리할 청크 개수 (기본값: 30)
     * @return 처리 결과 메시지
     */
    @PostMapping("/process/{documentId}")
    public ResponseEntity<String> processDocumentChunks(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "30") int limit) {

        log.info("API 요청: 문서 {} 의 청크 {} 개 처리", documentId, limit);

        try {
            int count = chunkToLlmService.processDocumentChunks(documentId, limit);
            return ResponseEntity.ok(String.format("문서 %d 의 청크 %d 개 처리가 완료되었습니다.", documentId, count));
        } catch (Exception e) {
            log.error("API 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 실패한 청크 재처리 API
     * 
     * @return 재처리된 청크 개수
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<String> retryFailedChunks() {
        log.info("API 요청: 실패한 청크 재처리 시작");
        try {
            int count = chunkToLlmService.retryFailedChunks();
            return ResponseEntity.ok(String.format("실패했던 청크 %d 개의 재처리가 완료되었습니다.", count));
        } catch (Exception e) {
            log.error("재처리 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("재처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
