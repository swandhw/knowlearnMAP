package com.knowlearnmap.pipeline.orchestration;

import com.knowlearnmap.pipeline.core.PipelineStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * JPA Entity for tracking pipeline execution.
 */
@Entity
@Table(name = "pipeline_execution")
@Data
public class PipelineExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PipelineStatus status;

    @Column(name = "current_stage")
    private String currentStage;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
