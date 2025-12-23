package com.knowlearnmap.pipeline.core;

/**
 * Base exception for pipeline-related errors.
 */
public class PipelineException extends Exception {

    private final PipelineStage stage;
    private final boolean retryable;

    public PipelineException(String message, PipelineStage stage) {
        super(message);
        this.stage = stage;
        this.retryable = false;
    }

    public PipelineException(String message, PipelineStage stage, Throwable cause) {
        super(message, cause);
        this.stage = stage;
        this.retryable = false;
    }

    public PipelineException(String message, PipelineStage stage, boolean retryable) {
        super(message);
        this.stage = stage;
        this.retryable = retryable;
    }

    public PipelineException(String message, PipelineStage stage, Throwable cause, boolean retryable) {
        super(message, cause);
        this.stage = stage;
        this.retryable = retryable;
    }

    public PipelineStage getStage() {
        return stage;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
