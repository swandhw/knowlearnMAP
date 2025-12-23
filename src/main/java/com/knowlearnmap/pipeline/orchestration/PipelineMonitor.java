package com.knowlearnmap.pipeline.orchestration;

import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Pipeline monitoring service for progress tracking and event publishing.
 * 
 * <p>
 * Provides real-time visibility into pipeline execution:
 * </p>
 * <ul>
 * <li>Stage start/complete/failure events</li>
 * <li>Progress tracking</li>
 * <li>Metrics collection</li>
 * </ul>
 */
@Service
@Slf4j
public class PipelineMonitor {

    private final ApplicationEventPublisher eventPublisher;

    public PipelineMonitor(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publish stage start event.
     */
    public void publishStageStart(PipelineContext context, PipelineStage stage) {
        log.info("[MONITOR] Stage Started: {} - Document: {}",
                stage.getDisplayName(), context.getDocumentId());

        PipelineEvent event = PipelineEvent.builder()
                .documentId(context.getDocumentId())
                .workspaceId(context.getWorkspaceId())
                .stage(stage)
                .eventType(PipelineEventType.STAGE_STARTED)
                .build();

        eventPublisher.publishEvent(event);
    }

    /**
     * Publish stage complete event.
     */
    public void publishStageComplete(PipelineContext context, PipelineStage stage) {
        log.info("[MONITOR] Stage Completed: {} - Document: {}",
                stage.getDisplayName(), context.getDocumentId());

        PipelineEvent event = PipelineEvent.builder()
                .documentId(context.getDocumentId())
                .workspaceId(context.getWorkspaceId())
                .stage(stage)
                .eventType(PipelineEventType.STAGE_COMPLETED)
                .build();

        eventPublisher.publishEvent(event);
    }

    /**
     * Publish stage failure event.
     */
    public void publishStageFailure(PipelineContext context, PipelineStage stage, Exception error) {
        log.error("[MONITOR] Stage Failed: {} - Document: {} - Error: {}",
                stage.getDisplayName(), context.getDocumentId(), error.getMessage());

        PipelineEvent event = PipelineEvent.builder()
                .documentId(context.getDocumentId())
                .workspaceId(context.getWorkspaceId())
                .stage(stage)
                .eventType(PipelineEventType.STAGE_FAILED)
                .errorMessage(error.getMessage())
                .build();

        eventPublisher.publishEvent(event);
    }

    /**
     * Publish pipeline completion event.
     */
    public void publishCompletion(PipelineContext context) {
        log.info("[MONITOR] Pipeline Completed - Document: {}", context.getDocumentId());

        PipelineEvent event = PipelineEvent.builder()
                .documentId(context.getDocumentId())
                .workspaceId(context.getWorkspaceId())
                .eventType(PipelineEventType.PIPELINE_COMPLETED)
                .build();

        eventPublisher.publishEvent(event);
    }

    /**
     * Publish pipeline failure event.
     */
    public void publishFailure(PipelineContext context, Exception error) {
        log.error("[MONITOR] Pipeline Failed - Document: {} - Error: {}",
                context.getDocumentId(), error.getMessage());

        PipelineEvent event = PipelineEvent.builder()
                .documentId(context.getDocumentId())
                .workspaceId(context.getWorkspaceId())
                .eventType(PipelineEventType.PIPELINE_FAILED)
                .errorMessage(error.getMessage())
                .build();

        eventPublisher.publishEvent(event);
    }
}
