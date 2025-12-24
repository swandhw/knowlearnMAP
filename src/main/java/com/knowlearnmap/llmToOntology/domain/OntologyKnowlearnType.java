package com.knowlearnmap.llmToOntology.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Ontology Knowlearn Type 엔티티
 * 
 * <p>
 * Subject-Relation-Object 트리플 관계를 저장합니다.
 * </p>
 * 
 * <h3>구조</h3>
 * 
 * <pre>
 * Subject (객체) --[Relation (관계)]--> Object (객체)
 * 예: "비타민C" --[포함]--> "세럼"
 * </pre>
 * 
 * <h3>Unique Constraint</h3>
 * workspace_id + subject_id + relation_id + object_id 조합으로 중복 방지
 */
@Entity
@Table(name = "ontology_knowlearn_type", uniqueConstraints = {
        @UniqueConstraint(name = "uk_knowlearn_workspace_triple", columnNames = { "workspace_id", "subject_id",
                "relation_id", "object_id" })
}, indexes = {
        @Index(name = "idx_knowlearn_workspace", columnList = "workspace_id"),
        @Index(name = "idx_knowlearn_subject", columnList = "subject_id"),
        @Index(name = "idx_knowlearn_object", columnList = "object_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OntologyKnowlearnType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 워크스페이스 ID (멀티테넌시)
     */
    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;


    /**
     * Subject 객체 ID
     */
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /**
     * Relation ID
     */
    @Column(name = "relation_id", nullable = false)
    private Long relationId;

    /**
     * Object 객체 ID
     */
    @Column(name = "object_id", nullable = false)
    private Long objectId;

    /**
     * 신뢰도 점수 (0.0 ~ 1.0)
     */
    @Column(name = "confidence_score", precision = 10, scale = 2)
    private BigDecimal confidenceScore;

    /**
     * 출처 정보 (JSON 배열 형식)
     * 예: ["chunk_id_1", "chunk_id_2"]
     */
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "source", length = 200)
    private String source;

    /**
     * 증거 수준
     * - high: 높은 신뢰도
     * - standard: 일반
     * - low: 낮은 신뢰도
     */
    @Column(name = "evidence_level", length = 20)
    @Builder.Default
    private String evidenceLevel = "standard";

    /**
     * 상태
     * - active: 활성
     * - inactive: 비활성
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.evidenceLevel == null) {
            this.evidenceLevel = "standard";
        }
        if (this.status == null) {
            this.status = "active";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

