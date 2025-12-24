package com.knowlearnmap.chunkToLlm.service;

import com.knowlearnmap.prompt.dto.LlmDirectCallResponse;
import com.knowlearnmap.prompt.dto.SimpleLlmCallRequest;
import com.knowlearnmap.prompt.service.PromptTestService;
import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chunk → LLM 호출 → 결과 저장 서비스
 * 
 * <h3>처리 흐름</h3>
 * 
 * <pre>
 * 1. Chunk 테이블에서 llm_status = null 조회
 * 2. PromptTestService로 LLM 호출
 * 3. LLM 결과를 chunk.llm_result에 JSON으로 저장
 * 4. llm_status = 'COMPLETED' 업데이트
 * </pre>
 * 
 * <h3>사용 예시</h3>
 * 
 * <pre>
 * // 단일 청크 처리
 * chunkToLlmService.processChunkWithLlm(chunkId, "ANTHROPIC");
 * 
 * // 배치 처리 (Parallel)
 * List&lt;DocumentChunk&gt; pendingChunks = chunkRepository.findByLlmStatusIsNull();
 * chunkToLlmService.processChunksParallel(pendingChunks);
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkToLlmService {

    private final DocumentChunkRepository chunkRepository;
    private final PromptTestService promptTestService;

    // Self-injection for Transactional method calls within async threads
    @Lazy
    @Autowired
    private ChunkToLlmService self;

    private static final int MAX_RETRY = 3;
    private static final int THREAD_POOL_SIZE = 5;
    private static final String PROMPT_CODE = "CHUNK_TO_ONTOLOGY";

    /**
     * Chunk를 LLM으로 처리하여 결과 저장
     * 
     * @param chunkId  처리할 청크 ID
     * @param llmModel LLM 모델 (ANTHROPIC, OPENAI, AISTUDIO)
     */
    @Transactional
    public void processChunkWithLlm(Long chunkId, String llmModel) {
        DocumentChunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new RuntimeException("Chunk를 찾을 수 없습니다: " + chunkId));

        log.info("=== Chunk LLM 처리 시작 ===");
        log.info("Chunk ID: {}, Model: {}", chunkId, llmModel);

        int attempt = 0;
        boolean success = false;
        String lastErrorMessage = null;

        while (attempt < MAX_RETRY && !success) {
            attempt++;
            try {
                // 상태 업데이트 (별도 트랜잭션이 필요할 수도 있으나, 여기서는 같은 트랜잭션 내 처리)
                // 주의: 긴 처리 시간 동안 DB Connection을 점유할 수 있음.
                // 프로덕션 레벨에서는 상태 업데이트와 LLM 호출을 분리하는 것이 좋음.
                chunk.setLlmStatus("PROCESSING");
                chunkRepository.saveAndFlush(chunk); // 즉시 반영

                log.info("LLM 호출 시도 {}/{} - Chunk ID: {}", attempt, MAX_RETRY, chunkId);

                // LLM 호출 (시간 소요)
                String llmResult = callLlm(chunk.getContent());

                // 결과 저장
                chunk.setLlmResult(llmResult);
                chunk.setLlmStatus("COMPLETED");
                chunk.setLlmProcessedAt(LocalDateTime.now());
                chunk.setLlmErrorMessage(null);
                chunkRepository.save(chunk);

                success = true;
                log.info("Chunk LLM 처리 완료 - Chunk ID: {}, 시도: {}/{}", chunkId, attempt, MAX_RETRY);

            } catch (Exception e) {
                lastErrorMessage = String.format(
                        "LLM 호출 실패 (시도 %d/%d): %s",
                        attempt, MAX_RETRY, e.getMessage());
                log.error(lastErrorMessage, e);

                // 재시도 전 대기
                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(1000L * attempt); // 1초, 2초, 3초
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                    }
                }
            }
        }

        // 최종 실패 처리
        if (!success) {
            chunk.setLlmStatus("FAILED");
            chunk.setLlmErrorMessage(lastErrorMessage);
            chunkRepository.save(chunk);

            log.error("Chunk LLM 처리 최종 실패 - Chunk ID: {}, Error: {}", chunkId, lastErrorMessage);
            throw new RuntimeException("Chunk LLM 처리 실패: " + lastErrorMessage);
        }
    }

    /**
     * LLM API 호출
     */
    private String callLlm(String chunkContent) {
        try {
            // 요청 파라미터 구성
            Map<String, Object> variables = new HashMap<>();
            variables.put("CHUNK_LIST", chunkContent);

            SimpleLlmCallRequest request = new SimpleLlmCallRequest();
            request.setVariables(variables);

            // PromptTestService 호출
            LlmDirectCallResponse result = promptTestService
                    .callLlmWithPublishedPrompt(PROMPT_CODE, request);

            if (result == null || result.getSuccess() == null || !result.getSuccess()) {
                throw new RuntimeException("LLM 호출 실패: 응답이 없거나 실패 상태");
            }

            String llmResult = result.getText();

            // 마크다운 코드 블록 제거 (```json ... ```)
            if (llmResult != null) {
                llmResult = llmResult.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "");
            }

            return llmResult;

        } catch (Exception e) {
            log.error("LLM API 호출 실패", e);
            throw new RuntimeException("LLM API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 미처리 청크 목록 조회
     */
    public List<DocumentChunk> getPendingChunks() {
        return chunkRepository.findByLlmStatusIsNull();
    }

    /**
     * 실패한 청크 목록 조회
     */
    public List<DocumentChunk> getFailedChunks() {
        return chunkRepository.findByLlmStatus("FAILED");
    }

    /**
     * 청크 목록을 비동기 병렬 처리
     * 
     * @param chunks 처리할 청크 목록
     * @return 성공한 청크 수
     */
    public int processChunksParallel(List<DocumentChunk> chunks) {
        log.info("병렬 청크 처리 시작 (대상: {}개, 스레드: {})", chunks.size(), THREAD_POOL_SIZE);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (DocumentChunk chunk : chunks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Self-injection을 이용해 트랜잭션 적용
                    self.processChunkWithLlm(chunk.getId(), "ANTHROPIC");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("청크 {} 비동기 처리 중 오류: {}", chunk.getId(), e.getMessage());
                }
            }, executorService);
            futures.add(future);
        }

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        log.info("병렬 청크 처리 완료. 성공: {}/{}", successCount.get(), chunks.size());
        return successCount.get();
    }

    /**
     * 특정 문서의 청크를 지정된 개수만큼 LLM 처리 (테스트용)
     * 5개 스레드로 비동기 병렬 처리
     * 
     * @param documentId 문서 ID
     * @param limit      처리할 청크 개수
     * @return 처리된 청크 수
     */
    public int processDocumentChunks(Long documentId, int limit) {
        log.info("문서 ID {} 의 청크 {} 개 조회 및 처리 시작", documentId, limit);

        List<DocumentChunk> chunks = chunkRepository.findByDocumentId(
                documentId,
                org.springframework.data.domain.PageRequest.of(0, limit));

        if (chunks.isEmpty()) {
            log.info("처리할 청크가 없습니다.");
            return 0;
        }

        return processChunksParallel(chunks);
    }
}
