package com.knowlearnmap.document.domain;

import com.knowlearnmap.workspace.domain.WorkspaceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Document 엔티티
 * 
 * 워크스페이스에 업로드된 소스(파일, URL, YouTube 등)를 표현
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private WorkspaceEntity workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private IngestionStatus status;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_status")
    private PipelineStatus pipelineStatus;

    @Column(name = "pipeline_step")
    private String pipelineStep;

    /**
     * 소스 타입: FILE, URL, YOUTUBE, DATABASE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private SourceType sourceType;

    /**
     * DB 소스용 테이블명
     */
    @Column(name = "source_table")
    private String sourceTable;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = IngestionStatus.PENDING;
        }
        if (pipelineStatus == null) {
            pipelineStatus = PipelineStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum IngestionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public enum PipelineStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public enum SourceType {
        FILE, // PDF, TXT 등 파일
        URL, // 웹사이트 링크
        YOUTUBE, // YouTube 동영상
        DATABASE // 데이터베이스 테이블
    }
}
