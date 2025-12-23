package com.knowlearnmap.pipeline.orchestration;

import com.knowlearnmap.pipeline.core.PipelineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for pipeline execution entities.
 */
@Repository
public interface PipelineExecutionRepository extends JpaRepository<PipelineExecutionEntity, Long> {

    /**
     * Find latest execution for a document.
     */
    Optional<PipelineExecutionEntity> findTopByDocumentIdOrderByCreatedAtDesc(Long documentId);

    /**
     * Find all executions for a workspace.
     */
    List<PipelineExecutionEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    /**
     * Find executions by status.
     */
    List<PipelineExecutionEntity> findByStatus(PipelineStatus status);

    /**
     * Find running executions for a document.
     */
    List<PipelineExecutionEntity> findByDocumentIdAndStatus(Long documentId, PipelineStatus status);
}
