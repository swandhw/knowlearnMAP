package com.knowlearnmap.pipeline.chunking;

import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.domain.DocumentPage;
import com.knowlearnmap.document.repository.DocumentChunkRepository;
import com.knowlearnmap.document.repository.DocumentPageRepository;
import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import com.knowlearnmap.pipeline.core.PipelineStage;
import com.knowlearnmap.pipeline.core.StageProcessor;
import com.knowlearnmap.rag.service.SimpleChunkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * LLM-based chunking stage processor.
 * 
 * <p>
 * Creates intelligent chunks from document using LLM.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmChunkingProcessor implements StageProcessor {

    private final DocumentPageRepository documentPageRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final SimpleChunkingService simpleChunkingService;

    @Override
    @Transactional
    public void process(PipelineContext context) throws PipelineException {
        log.info("Processing LLM chunking for document={}", context.getDocumentId());

        try {
            // 문서의 모든 완료된 페이지 조회
            List<DocumentPage> pages = documentPageRepository
                    .findByDocumentIdAndStatus(context.getDocumentId(), DocumentPage.PageStatus.COMPLETED);

            if (pages.isEmpty()) {
                throw new PipelineException(
                        "No pages found for document: " + context.getDocumentId(),
                        getStage(),
                        null,
                        false);
            }

            log.info("Found {} pages for chunking", pages.size());

            // SimpleChunkingService를 사용하여 청킹
            List<DocumentChunk> chunks = simpleChunkingService.chunkPages(pages, context.getDocumentId());

            // 청크 저장
            documentChunkRepository.saveAll(chunks);

            // 컨텍스트 업데이트
            context.addMetadata("chunks_created", true);
            context.addMetadata("chunk_count", chunks.size());
            context.setProgress(50);

            log.info("LLM chunking completed successfully: {} chunks created", chunks.size());

        } catch (Exception e) {
            throw new PipelineException("LLM chunking failed", getStage(), e, true);
        }
    }

    @Override
    public PipelineStage getStage() {
        return PipelineStage.CHUNK;
    }
}
