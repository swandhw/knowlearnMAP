package com.knowlearnmap.pipeline.orchestration;

import com.knowlearnmap.pipeline.core.PipelineStage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Pipeline event for monitoring and notifications.
 */
@Data
@Builder
public class PipelineEvent {

    private Long documentId;
    private Long workspaceId;
    private PipelineStage stage;
    private PipelineEventType eventType;
    private String errorMessage;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
