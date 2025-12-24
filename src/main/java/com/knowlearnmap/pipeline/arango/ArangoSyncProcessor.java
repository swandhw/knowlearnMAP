package com.knowlearnmap.pipeline.arango;

import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import com.knowlearnmap.pipeline.core.PipelineStage;
import com.knowlearnmap.pipeline.core.StageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ArangoDB synchronization stage processor.
 * 
 * <p>
 * Synchronizes ontology data to ArangoDB.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArangoSyncProcessor implements StageProcessor {

    private final com.knowlearnmap.ontologyToArango.service.OntologyToArangoService ontologyToArangoService;

    @Override
    public void process(PipelineContext context) throws PipelineException {
        log.info("Processing ArangoDB sync for workspace={}, document={}",
                context.getWorkspaceId(), context.getDocumentId());

        try {
            // Ontology 추출이 완료된 후 실행됨
            // 전체 워크스페이스 동기화 (Drop existing = false)
            ontologyToArangoService.syncOntologyToArango(context.getWorkspaceId(), false);

            context.addMetadata("arango_synced", true);
            context.setProgress(80);

            log.info("ArangoDB sync completed successfully");

        } catch (Exception e) {
            throw new PipelineException("ArangoDB sync failed", getStage(), e, true);
        }
    }

    @Override
    public PipelineStage getStage() {
        return PipelineStage.ARANGO_SYNC;
    }
}
