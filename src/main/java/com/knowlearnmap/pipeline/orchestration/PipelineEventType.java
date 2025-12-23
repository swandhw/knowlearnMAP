package com.knowlearnmap.pipeline.orchestration;

/**
 * Types of pipeline events.
 */
public enum PipelineEventType {
    STAGE_STARTED,
    STAGE_COMPLETED,
    STAGE_FAILED,
    PIPELINE_COMPLETED,
    PIPELINE_FAILED
}
