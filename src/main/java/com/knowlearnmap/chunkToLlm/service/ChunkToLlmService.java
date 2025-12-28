package com.knowlearnmap.chunkToLlm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.knowlearnmap.prompt.dto.LlmDirectCallResponse;
import com.knowlearnmap.prompt.dto.SimpleLlmCallRequest;
import com.knowlearnmap.prompt.service.PromptTestService;
import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.repository.DocumentChunkRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Chunk → LLM 호출 → 결과 저장 서비스 (Batch Processing)
 * 
 * <h3>처리 흐름</h3>
 * 
 * <pre>
 * 1. Chunk 테이블에서 llm_status = null 조회
 * 2. 10개씩(BATCH_SIZE) 묶어서 JSON Array로 변환
 * 3. PromptTestService로 LLM 호출 (배치 단위)
 * 4. LLM 결과(JSON)를 파싱하여 각 Chunk의 llm_result에 저장
 * 5. llm_status = 'COMPLETED' 업데이트
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkToLlmService {

    private final DocumentChunkRepository chunkRepository;
    private final TransactionTemplate transactionTemplate;
    private final PromptTestService promptTestService;
    private final ObjectMapper objectMapper; // JSON 처리를 위해 주입 필요

    private static final int MAX_RETRY = 3;
    private static final int THREAD_POOL_SIZE = 5; // 배치 처리이므로 스레드 수 조정
    private static final String PROMPT_CODE = "CHUNK_TO_ONTOLOGY";
    private static final int BATCH_SIZE = 10; // 배치 크기 상수

    // ==========================================
    // Inner DTOs for Batch Processing
    // ==========================================

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class ChunkRequest {
        private Long id;
        private String text;
    }

    @Data
    @NoArgsConstructor
    private static class BatchLlmResponse {
        private List<ChunkResult> chunks;
    }

    @Data
    @NoArgsConstructor
    private static class ChunkResult {
        private Long id;
        private List<Object> objects_to_add;
        private List<Object> relations_to_add;
        private List<Object> knowlearns_to_add;
        // LLM이 반환하는 기타 필드들...
    }

    /**
     * Chunk를 LLM으로 처리하여 결과 저장 (단건 처리 - 호환성 유지)
     */
    public void processChunkWithLlm(Long chunkId, String llmModel) {
        // 단건 처리도 내부적으로는 리스트로 감싸서 배치 로직 활용 가능하나,
        // 기존 로직과 다르게 배치 프롬프트를 쓰게 되므로 주의 필요.
        // 여기서는 단건 요청도 배치 프롬프트 형식에 맞춰서 처리하도록 변경함.
        DocumentChunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new RuntimeException("Chunk를 찾을 수 없습니다: " + chunkId));

        processBatchWithLlm(List.of(chunk));
    }

    /**
     * 청크 목록을 비동기 배치 병렬 처리
     */
    public int processChunksParallel(List<DocumentChunk> chunks) {
        log.info("배치 청크 처리 시작 (대상: {}개, 배치크기: {}, 스레드: {})", chunks.size(), BATCH_SIZE, THREAD_POOL_SIZE);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        // 리스트를 배치 크기로 분할
        List<List<DocumentChunk>> batches = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {
            batches.add(chunks.subList(i, Math.min(chunks.size(), i + BATCH_SIZE)));
        }

        log.info("총 {} 개의 배치가 생성되었습니다.", batches.size());

        for (List<DocumentChunk> batch : batches) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    processBatchWithLlm(batch);
                    successCount.addAndGet(batch.size());
                } catch (Exception e) {
                    log.error("배치 처리 중 오류 발생 (First Chunk ID: {}): {}", batch.get(0).getId(), e.getMessage());
                    // 실패 시 별도 카운팅 하지 않음
                }
            }, executorService);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        log.info("배치 청크 처리 완료. 성공: {}/{}", successCount.get(), chunks.size());
        return successCount.get();
    }

    /**
     * 배치 단위 LLM 호출 및 결과 저장
     */
    private void processBatchWithLlm(List<DocumentChunk> batch) {
        List<Long> chunkIds = batch.stream().map(DocumentChunk::getId).toList();
        log.info("=== Batch LLM 처리 시작 (Ids: {}) ===", chunkIds);

        int attempt = 0;
        boolean success = false;
        String lastErrorMessage = null;

        while (attempt < MAX_RETRY && !success) {
            attempt++;
            try {
                // 1. 상태 업데이트 (PROCESSING)
                transactionTemplate.execute(status -> {
                    List<DocumentChunk> targets = chunkRepository.findAllById(chunkIds);
                    targets.forEach(c -> c.setLlmStatus("PROCESSING"));
                    chunkRepository.saveAllAndFlush(targets);
                    return null;
                });

                // 2. 요청 데이터(JSON) 생성
                List<ChunkRequest> requests = batch.stream()
                        .map(c -> new ChunkRequest(c.getId(), c.getContent()))
                        .collect(Collectors.toList());
                String jsonInput = objectMapper.writeValueAsString(requests);

                log.info("LLM 호출 시도 {}/{} - Batch Size: {}", attempt, MAX_RETRY, batch.size());

                // 3. LLM 호출
                String llmResultJson = callLlm(jsonInput);

                // 4. 결과 파싱 및 저장
                parseAndSaveBatchResult(batch, llmResultJson);

                success = true;
                log.info("Batch LLM 처리 완료 - 시도: {}/{}", attempt, MAX_RETRY);

            } catch (Exception e) {
                lastErrorMessage = String.format("LLM Batch 호출 실패 (시도 %d/%d): %s", attempt, MAX_RETRY, e.getMessage());
                log.error(lastErrorMessage, e);

                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 중 인터럽트", ie);
                    }
                }
            }
        }

        // 최종 실패 처리
        if (!success) {
            String finalError = lastErrorMessage;
            transactionTemplate.execute(status -> {
                List<DocumentChunk> targets = chunkRepository.findAllById(chunkIds);
                targets.forEach(c -> {
                    c.setLlmStatus("FAILED");
                    c.setLlmErrorMessage(finalError);
                });
                chunkRepository.saveAll(targets);
                // 모니터링 알림 등 추가 가능
                return null;
            });
            log.error("Batch LLM 처리 최종 실패 - Ids: {}", chunkIds);
            throw new RuntimeException("Batch LLM 처리 실패");
        }
    }

    private void parseAndSaveBatchResult(List<DocumentChunk> batch, String llmResultJson)
            throws JsonProcessingException {
        // LLM 응답이 {"chunks": [...]} 형태라고 가정
        // 혹은 최상위가 배열일 수도 있음. 프롬프트에 따라 다름.
        // 현재 프롬프트 예시는 {"chunks": [...]} 구조임.

        BatchLlmResponse response;
        try {
            response = objectMapper.readValue(llmResultJson, BatchLlmResponse.class);
        } catch (Exception e) {
            // 만약 배열로 바로 들어오는 경우 대응
            try {
                List<ChunkResult> list = objectMapper.readValue(llmResultJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ChunkResult.class));
                response = new BatchLlmResponse();
                response.setChunks(list);
            } catch (Exception e2) {
                throw new RuntimeException("LLM 응답 파싱 실패: " + llmResultJson, e);
            }
        }

        if (response.getChunks() == null || response.getChunks().isEmpty()) {
            throw new RuntimeException("LLM 응답에 chunks 데이터가 없습니다.");
        }

        Map<Long, ChunkResult> resultMap = response.getChunks().stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(ChunkResult::getId, r -> r, (a, b) -> a));

        transactionTemplate.execute(status -> {
            List<DocumentChunk> targets = chunkRepository
                    .findAllById(batch.stream().map(DocumentChunk::getId).toList());

            for (DocumentChunk chunk : targets) {
                ChunkResult result = resultMap.get(chunk.getId());
                if (result != null) {
                    // 결과 JSON으로 변환하여 저장
                    try {
                        String resultJson = objectMapper.writeValueAsString(result);
                        chunk.setLlmResult(resultJson);
                        chunk.setLlmStatus("COMPLETED");
                        chunk.setLlmProcessedAt(LocalDateTime.now());
                        chunk.setLlmErrorMessage(null);
                    } catch (JsonProcessingException e) {
                        chunk.setLlmStatus("FAILED");
                        chunk.setLlmErrorMessage("결과 JSON 변환 실패");
                    }
                } else {
                    chunk.setLlmStatus("FAILED");
                    chunk.setLlmErrorMessage("배치 응답에 해당 ID 결과가 누락됨");
                }
            }
            chunkRepository.saveAll(targets);

            // 모니터링 서비스에 알림 (옵션)
            // 모니터링 서비스 제거로 인한 단순 로그 처리
            log.info("Batch Process Success: Processed {} chunks", targets.size());

            return null;
        });
    }

    /**
     * LLM API 호출
     */
    private String callLlm(String jsonInput) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("CHUNK_LIST", jsonInput);

            SimpleLlmCallRequest request = new SimpleLlmCallRequest();
            request.setVariables(variables);

            // PromptTestService 호출
            LlmDirectCallResponse result = promptTestService
                    .callLlmWithPublishedPrompt(PROMPT_CODE, request);

            if (result == null || result.getSuccess() == null || !result.getSuccess()) {
                throw new RuntimeException("LLM 호출 실패: 응답 없음");
            }

            String llmResult = result.getText();
            if (llmResult != null) {
                llmResult = llmResult.replaceAll("^```json\\s*", "").replaceAll("\\s*```$", "");
            }
            return llmResult;

        } catch (Exception e) {
            log.error("LLM API 호출 실패", e);
            throw new RuntimeException("LLM API 호출 실패: " + e.getMessage(), e);
        }
    }

    public List<DocumentChunk> getPendingChunks() {
        return chunkRepository.findByLlmStatusIsNull();
    }

    public List<DocumentChunk> getFailedChunks() {
        return chunkRepository.findByLlmStatus("FAILED");
    }

    /**
     * 특정 문서의 청크를 지정된 개수만큼 LLM 처리 (테스트용)
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

    /**
     * 실패한 청크들을 재처리
     */
    public int retryFailedChunks() {
        List<DocumentChunk> failedChunks = getFailedChunks();
        if (failedChunks.isEmpty()) {
            log.info("재처리할 실패 청크가 없습니다.");
            return 0;
        }

        log.info("실패한 청크 {} 개 재처리 시작", failedChunks.size());

        // 상태 초기화 (선택 사항: 재처리 중임을 표시)
        // processChunksParallel 내부에서 PROCESSING으로 변경하므로 여기서는 생략 가능하지만,
        // 명확성을 위해 로그 남김.

        return processChunksParallel(failedChunks);
    }
}
