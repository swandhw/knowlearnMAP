package com.knowlearnmap.pipeline.llm;

import com.knowlearnmap.chunkToLlm.service.ChunkToLlmService;
import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.repository.DocumentChunkRepository;
import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import com.knowlearnmap.pipeline.core.PipelineStage;
import com.knowlearnmap.pipeline.core.StageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * LLM Processing Stage Processor (ChunkToLlm)
 * <p>
 * Process DocumentChunks with LLM to generate intermediate JSON results
 * which are subsequently used by OntologyExtractionProcessor.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmProcessingProcessor implements StageProcessor {

    private final ChunkToLlmService chunkToLlmService;
    private final DocumentChunkRepository documentChunkRepository;

    @Override
    public void process(PipelineContext context) throws PipelineException {
        log.info("Processing LLM stage for document={}", context.getDocumentId());

        try {
            // 1. 문서의 모든 청크 조회 (TODO: 페이징 혹은 배치 처리 고려)
            // 현재는 전체 청크를 한 번에 조회하여 병렬 처리
            List<DocumentChunk> chunks = documentChunkRepository
                    .findByDocumentIdOrderByChunkIndex(context.getDocumentId());

            if (chunks.isEmpty()) {
                log.warn("No chunks found for document {}", context.getDocumentId());
                return;
            }

            log.info("Found {} chunks to process with LLM", chunks.size());

            // 2. ChunkToLlmService를 통해 병렬 처리
            // 이미 처리된(COMPLETED) 청크는 서비스 내부 로직 혹은 필터링으로 스킵할 수 있음
            // 하지만 현재 ChunkToLlmService는 'findByLlmStatusIsNull'를 주로 사용하므로,
            // 여기서는 명시적으로 필터링 후 넘겨주는 것이 안전함.

            // 스트림으로 미처리 청크만 필터링
            List<DocumentChunk> pendingChunks = chunks.stream()
                    .filter(c -> !"COMPLETED".equals(c.getLlmStatus()))
                    .toList();

            if (pendingChunks.isEmpty()) {
                log.info("All chunks already processed.");
            } else {
                int successCount = chunkToLlmService.processChunksParallel(pendingChunks);
                log.info("LLM processing completed. Success: {}/{}", successCount, pendingChunks.size());

                if (successCount < pendingChunks.size()) {
                    throw new PipelineException("Some chunks failed LLM processing", getStage(), null, false);
                }
            }

            // 3. 컨텍스트 업데이트
            context.addMetadata("llm_processed", true);
            context.setProgress(55); // VECTORIZE(50~60)와 병렬이므로 적절히 배분

        } catch (Exception e) {
            throw new PipelineException("LLM processing failed", getStage(), e, true);
        }
    }

    @Override
    public PipelineStage getStage() {
        return PipelineStage.LLM_PROCESSING;
    }
}
