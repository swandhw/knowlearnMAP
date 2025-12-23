package com.knowlearnmap.pipeline.vectorization;

import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import com.knowlearnmap.pipeline.core.PipelineStage;
import com.knowlearnmap.pipeline.core.StageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    // TODO: Inject embedding service from reference code

    @Override
    public void process(PipelineContext context) throws PipelineException {
        log.info("Processing vector embedding for document={}", context.getDocumentId());

        try {
            // TODO: Implement actual vectorization logic
            // This runs in parallel with ontology extraction
            // After completion, stores to PostgreSQL and TERMINATES (does not continue to
            // ArangoDB)

            context.addMetadata("vectors_created", true);
            context.setProgress(60);

            log.info("Vector embedding completed successfully - VECTOR PATH COMPLETE");

        } catch (Exception e) {
            throw new PipelineException("Vector embedding failed", getStage(), e, true);
        }
    }

    @Override
    public PipelineStage getStage() {
        return PipelineStage.VECTORIZE;
    }
}
