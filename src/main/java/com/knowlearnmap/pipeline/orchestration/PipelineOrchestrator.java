package com.knowlearnmap.pipeline.orchestration;

import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import com.knowlearnmap.pipeline.core.PipelineStage;
import com.knowlearnmap.pipeline.core.PipelineStatus;
import com.knowlearnmap.pipeline.core.StageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Main pipeline orchestrator coordinating all pipeline stages.
 * 
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 * <li>Execute pipeline stages in correct order</li>
 * <li>Handle parallel execution for stages marked as parallelExecutable</li>
 * <li>Manage error handling and retry logic</li>
 * <li>Persist pipeline execution state</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PipelineOrchestrator {

    private final List<StageProcessor> stageProcessors;
    private final PipelineExecutionRepository executionRepository;

    private final PipelineMonitor pipelineMonitor;
    private final com.knowlearnmap.workspace.repository.WorkspaceRepository workspaceRepository;

    /**
     * Execute the full pipeline asynchronously.
     * 
     * @param workspaceId Workspace ID
     * @param documentId  Document ID to process
     * @return CompletableFuture of the execution result
     */
    @Async
    public CompletableFuture<PipelineContext> executeAsync(Long workspaceId, Long documentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(workspaceId, documentId);
            } catch (PipelineException e) {
                log.error("Pipeline execution failed for workspace={}, document={}",
                        workspaceId, documentId, e);
                throw new RuntimeException("Pipeline execution failed", e);
            }
        });
    }

    /**
     * Execute the full pipeline synchronously.
     * 
     * @param workspaceId Workspace ID
     * @param documentId  Document ID to process
     * @return Pipeline execution context
     * @throws PipelineException if execution fails
     */
    public PipelineContext execute(Long workspaceId, Long documentId) throws PipelineException {
        log.info("=== Starting Pipeline Execution for workspace={}, document={} ===",
                workspaceId, documentId);

        // Initialize context
        PipelineContext context = PipelineContext.builder()
                .workspaceId(workspaceId)
                .documentId(documentId)
                .status(PipelineStatus.PENDING)
                .startTime(LocalDateTime.now())
                .progress(0)
                .build();

        // Create execution record
        PipelineExecutionEntity execution = createExecutionRecord(context);

        try {
            context.setStatus(PipelineStatus.PROCESSING);
            updateExecutionRecord(execution, context);

            // Group processors by order for parallel execution
            Map<Integer, List<StageProcessor>> processorsByOrder = stageProcessors.stream()
                    .collect(Collectors.groupingBy(StageProcessor::getOrder));

            // Execute stages in order
            for (Integer order : processorsByOrder.keySet().stream().sorted().toList()) {
                List<StageProcessor> processors = processorsByOrder.get(order);

                if (processors.size() == 1) {
                    // Single processor - execute sequentially
                    executeStage(processors.get(0), context, execution);
                } else {
                    // Multiple processors at same order - execute in parallel
                    executeParallelStages(processors, context, execution);
                }
            }

            // Mark as completed
            context.setStatus(PipelineStatus.COMPLETED);
            context.setProgress(100);
            updateExecutionRecord(execution, context);

            log.info("=== Pipeline Execution Completed Successfully ===");

            pipelineMonitor.publishCompletion(context);

            // Mark workspace as needing sync
            // Mark workspace as needing sync - SKIPPED because ArangoSyncProcessor handles
            // it
            /*
             * workspaceRepository.findById(workspaceId).ifPresent(ws -> {
             * ws.setNeedsArangoSync(true);
             * workspaceRepository.save(ws);
             * });
             */

            return context;

        } catch (Exception e) {
            log.error("Pipeline execution failed", e);
            context.setStatus(PipelineStatus.FAILED);
            context.setErrorMessage(e.getMessage());
            updateExecutionRecord(execution, context);

            pipelineMonitor.publishFailure(context, e);

            if (e instanceof PipelineException) {
                throw (PipelineException) e;
            }
            throw new PipelineException("Pipeline execution failed: " + e.getMessage(),
                    context.getCurrentStage(), e);
        }
    }

    /**
     * Execute a single stage.
     */
    private void executeStage(StageProcessor processor, PipelineContext context,
            PipelineExecutionEntity execution) throws PipelineException {
        PipelineStage stage = processor.getStage();
        log.info(">>> Executing Stage: {} [{}]", stage.getDisplayName(), stage.name());

        context.setCurrentStage(stage);
        updateExecutionRecord(execution, context);

        pipelineMonitor.publishStageStart(context, stage);

        try {
            processor.process(context);
            log.info(">>> Stage Completed: {}", stage.getDisplayName());
            pipelineMonitor.publishStageComplete(context, stage);
        } catch (Exception e) {
            log.error("Stage failed: {}", stage.getDisplayName(), e);
            pipelineMonitor.publishStageFailure(context, stage, e);
            throw new PipelineException("Stage failed: " + stage.getDisplayName(), stage, e);
        }
    }

    /**
     * Execute multiple stages in parallel.
     */
    private void executeParallelStages(List<StageProcessor> processors, PipelineContext context,
            PipelineExecutionEntity execution) throws PipelineException {
        log.info(">>> Executing Parallel Stages: {}",
                processors.stream().map(p -> p.getStage().name()).collect(Collectors.joining(", ")));

        List<CompletableFuture<Void>> futures = processors.stream()
                .map(processor -> CompletableFuture.runAsync(() -> {
                    try {
                        PipelineStage stage = processor.getStage();
                        context.setCurrentStage(stage);
                        pipelineMonitor.publishStageStart(context, stage);

                        processor.process(context);

                        log.info(">>> Parallel Stage Completed: {}", stage.getDisplayName());
                        pipelineMonitor.publishStageComplete(context, stage);
                    } catch (Exception e) {
                        log.error("Parallel stage failed", e);
                        throw new RuntimeException(e);
                    }
                }))
                .toList();

        // Wait for all parallel stages to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info(">>> All Parallel Stages Completed");
        } catch (Exception e) {
            throw new PipelineException("Parallel stage execution failed",
                    context.getCurrentStage(), e);
        }
    }

    /**
     * Create initial execution record.
     */
    @Transactional
    protected PipelineExecutionEntity createExecutionRecord(PipelineContext context) {
        PipelineExecutionEntity execution = new PipelineExecutionEntity();
        execution.setWorkspaceId(context.getWorkspaceId());
        execution.setDocumentId(context.getDocumentId());
        execution.setStatus(context.getStatus());
        execution.setCurrentStage(context.getCurrentStage() != null ? context.getCurrentStage().name() : null);
        execution.setProgress(context.getProgress());
        execution.setStartTime(context.getStartTime());

        return executionRepository.save(execution);
    }

    /**
     * Update execution record with current context.
     */
    @Transactional
    protected void updateExecutionRecord(PipelineExecutionEntity execution, PipelineContext context) {
        execution.setStatus(context.getStatus());
        execution.setCurrentStage(context.getCurrentStage() != null ? context.getCurrentStage().name() : null);
        execution.setProgress(context.getProgress());
        execution.setErrorMessage(context.getErrorMessage());

        if (context.getStatus() == PipelineStatus.COMPLETED ||
                context.getStatus() == PipelineStatus.FAILED) {
            execution.setEndTime(LocalDateTime.now());
        }

        executionRepository.save(execution);
    }
}
