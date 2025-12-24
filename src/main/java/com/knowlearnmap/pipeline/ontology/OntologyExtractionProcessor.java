package com.knowlearnmap.pipeline.ontology;

import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import com.knowlearnmap.pipeline.core.PipelineStage;
import com.knowlearnmap.pipeline.core.StageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Ontology extraction stage processor.
 * 
 * <p>
 * Extracts ontology from chunks using LLM.
 * Runs in parallel with vector embedding.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OntologyExtractionProcessor implements StageProcessor {

    private final com.knowlearnmap.llmToOntology.service.LlmToOntologyService llmToOntologyService;

    @Override
    public void process(PipelineContext context) throws PipelineException {
        log.info("Processing ontology extraction for document={}", context.getDocumentId());

        try {
            // LlmProcessingProcessor가 선행되었으므로 LLM 결과가 준비되어 있다고 가정
            int processedCount = llmToOntologyService.createOntologyFromDocument(
                    context.getWorkspaceId(),
                    context.getDocumentId());

            log.info("Ontology extraction completed. Processed chunks: {}", processedCount);

            context.addMetadata("ontology_extracted", true);
            context.addMetadata("ontology_count", processedCount);
            context.setProgress(70); // 순차적이므로 진행률 증가

        } catch (Exception e) {
            throw new PipelineException("Ontology extraction failed", getStage(), e, true);
        }
    }

    @Override
    public PipelineStage getStage() {
        return PipelineStage.ONTOLOGY;
    }
}
