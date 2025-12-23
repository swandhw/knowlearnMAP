package com.knowlearnmap.pipeline.orchestration;

import lombok.Data;

/**
 * Request DTO for starting pipeline execution.
 */
@Data
public class PipelineStartRequest {
    private Long workspaceId;
    private Long documentId;
}
