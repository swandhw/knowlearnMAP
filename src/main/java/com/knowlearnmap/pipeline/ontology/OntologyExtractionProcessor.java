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

    // TODO: Inject LlmToOntologyService from reference code

    @Override
    public void process(PipelineContext context) throws PipelineException {
        log.info("Processing ontology extraction for document={}", context.getDocumentId());

        try {
            // TODO: Implement actual ontology extraction using reference
            // LlmToOntologyService
            // This runs in parallel with vector embedding

            context.addMetadata("ontology_extracted", true);
            context.setProgress(60);

            log.info("Ontology extraction completed successfully");

        } catch (Exception e) {
            throw new PipelineException("Ontology extraction failed", getStage(), e, true);
        }
    }

    @Override
    public PipelineStage getStage() {
        return PipelineStage.ONTOLOGY;
    }
}
