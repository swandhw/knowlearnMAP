package com.knowlearnmap.pipeline.core;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Pipeline execution context carrying state through pipeline stages.
 * 
 * <p>
 * This context object is passed between stage processors and contains
 * all necessary information for pipeline execution.
 * </p>
 */
@Data
@Builder
public class PipelineContext {

    /**
     * Workspace ID
     */
    private Long workspaceId;

    /**
     * Document ID being processed
     */
    private Long documentId;

    /**
     * Current pipeline stage
     */
    private PipelineStage currentStage;

    /**
     * Current pipeline status
     */
    private PipelineStatus status;

    /**
     * Pipeline execution start time
     */
    private LocalDateTime startTime;

    /**
     * Current stage progress (0-100)
     */
    private Integer progress;

    /**
     * Additional metadata for stage-specific data
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Error message if status is FAILED
     */
    private String errorMessage;

    /**
     * Add metadata entry
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * Get metadata value
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        return (T) metadata.get(key);
    }
}
