package com.knowlearnmap.prompt.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;

// ============================================
// 2. PromptVersion Entity
// ============================================

@Entity
@Table(name = "prompt_versions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_prompt_version", columnNames = { "prompt_code", "version" })
}, indexes = {
        @Index(name = "idx_versions_prompt_code", columnList = "prompt_code"),
        @Index(name = "idx_versions_status", columnList = "status"),
        @Index(name = "idx_versions_active", columnList = "is_active")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "prompt_code", length = 100, nullable = false)
    private String promptCode;

    @Column(name = "version", nullable = false)
    private Integer version;

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "content", nullable = false)
    private String content;

    @Type(JsonBinaryType.class)
    @Column(name = "variable_schema", columnDefinition = "jsonb")
    private String variableSchema;
    /*
     * 예시:
     * [
     * {
     * "key": "question",
     * "type": "string",
     * "required": true,
     * "description": "고객 문의 내용",
     * "placeholder": "제 배송은 언제 도착하나요?"
     * }
     * ]
     */

    @Type(JsonBinaryType.class)
    @Column(name = "llm_config", columnDefinition = "jsonb")
    private String llmConfig;
    /*
     * 예시:
     * {
     * "model": "AISTUDIO",
     * "temperature": 0.7,
     * "topP": 0.95,
     * "maxOutputTokens": 2000,
     * "topK": 40,
     * "n": 1
     * }
     */

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "draft"; // draft, published, deprecated

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "notes")
    private String notes;

    // ============================================
    // 테스트 통계 (자동 계산)
    // ============================================
    @Column(name = "avg_satisfaction", precision = 3, scale = 2)
    private BigDecimal avgSatisfaction;

    @Column(name = "test_count")
    @Builder.Default
    private Integer testCount = 0;

    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;

    // ============================================
    // 종합 평가 (수동 입력)
    // ============================================
    @Column(name = "overall_rating")
    private Integer overallRating; // 1-5

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "overall_notes")
    private String overallNotes;

    // ============================================
    // 생성 및 수정 정보
    // ============================================
    @Column(name = "created_id", nullable = false, updatable = false, length = 255)
    private String createdId;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @Column(name = "updated_id", nullable = false, length = 255)
    private String updatedId;

    @Column(name = "updated_datetime", nullable = false)
    private LocalDateTime updatedDatetime;

    @PrePersist
    public void prePersist() {
        this.createdDatetime = LocalDateTime.now();
        this.updatedDatetime = LocalDateTime.now();
        if (this.createdId == null) {
            this.createdId = "admin";
        }
        if (this.updatedId == null) {
            this.updatedId = "admin";
        }
        if (this.status == null) {
            this.status = "draft";
        }
        if (this.isActive == null) {
            this.isActive = false;
        }
        if (this.testCount == null) {
            this.testCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedDatetime = LocalDateTime.now();
        if (this.updatedId == null) {
            this.updatedId = "admin";
        }
    }
}
