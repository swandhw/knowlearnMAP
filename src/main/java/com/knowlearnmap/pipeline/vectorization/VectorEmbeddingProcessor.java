package com.knowlearnmap.pipeline.vectorization;

import com.knowlearnmap.ai.service.EmbeddingService;
import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.repository.DocumentChunkRepository;
import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import com.knowlearnmap.pipeline.core.PipelineStage;
import com.knowlearnmap.pipeline.core.StageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Vector embedding stage processor.
 * 
 * <p>
 * Generates embeddings for chunks and stores in PostgreSQL.
 * Runs in parallel with ontology extraction.
 * </p>
 * 
 * <p>
 * <strong>Terminal Stage:</strong> This is the end of the vector path.
 * After completion, this branch of the pipeline terminates (does not proceed to
 * ArangoDB stages).
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorEmbeddingProcessor implements StageProcessor {

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository chunkRepository;

    @Override
    public void process(PipelineContext context) throws PipelineException {
        Long documentId = context.getDocumentId();
        log.info("Processing vector embedding for document={}", documentId);

        try {
            // 1. 문서의 모든 청크 조회
            List<DocumentChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);

            if (chunks.isEmpty()) {
                log.warn("No chunks found for document={}, skipping vector embedding", documentId);
                return;
            }

            int totalChunks = chunks.size();
            log.info("Found {} chunks to embed", totalChunks);

            // 2. 각 청크에 대해 임베딩 생성 및 저장
            for (int i = 0; i < totalChunks; i++) {
                DocumentChunk chunk = chunks.get(i);

                // 이미 임베딩이 있으면 스킵할 수도 있으나, 여기서는 재처리로 간주 (필요 시 로직 추가)

                String content = chunk.getContent();
                if (content != null && !content.trim().isEmpty()) {
                    List<Double> vector = embeddingService.embed(content);
                    chunk.setEmbedding(vector);
                }

                // 진행률 업데이트 (60% ~ 90%)
                int progress = 60 + (int) ((double) (i + 1) / totalChunks * 30);
                context.setProgress(progress);
            }

            // 3. 변경사항 저장
            chunkRepository.saveAll(chunks);

            context.addMetadata("vectors_created", true);
            context.addMetadata("chunk_count", totalChunks);
            context.setProgress(100); // 이 스테이지 완료 시 100%? 아니면 전체 파이프라인 상의 진행률?
                                      // PipelineOrchestrator 주석을 보면 스테이지 완료 시 업데이트 함.
                                      // 일단 스테이지 내부 진행률은 의미가 덜할 수 있으므로, 완료 시점에만 맞춰도 됨.
                                      // 하지만 process() 종료 시점에는 PipelineOrchestrator가 관리할 수 있음.
                                      // 여기서는 context에 진행률을 기록해두면 Orchestrator가 DB 업데이트 함.

            log.info("Vector embedding completed successfully for document={} ({} chunks)", documentId, totalChunks);

        } catch (Exception e) {
            throw new PipelineException("Vector embedding failed", getStage(), e, true);
        }
    }

    @Override
    public PipelineStage getStage() {
        return PipelineStage.VECTORIZE;
    }
}
