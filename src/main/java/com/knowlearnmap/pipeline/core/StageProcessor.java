package com.knowlearnmap.pipeline.core;

/**
 * Interface for all pipeline stage processors.
 * 
 * <p>
 * Each stage in the pipeline must implement this interface.
 * The processor is responsible for executing its specific stage logic
 * and updating the pipeline context.
 * </p>
 */
public interface StageProcessor {

    /**
     * Process the current stage.
     * 
     * @param context Pipeline execution context
     * @throws PipelineException if processing fails
     */
    void process(PipelineContext context) throws PipelineException;

    /**
     * Get the pipeline stage this processor handles.
     * 
     * @return The pipeline stage
     */
    PipelineStage getStage();

    /**
     * Get the execution order within the pipeline.
     * 
     * @return Execution order (lower numbers execute first)
     */
    default int getOrder() {
        return getStage().getOrder();
    }

    /**
     * Check if this stage can be executed in parallel with other stages.
     * 
     * @return true if parallel execution is supported
     */
    default boolean isParallelExecutable() {
        return getStage().isParallelExecutable();
    }
}
