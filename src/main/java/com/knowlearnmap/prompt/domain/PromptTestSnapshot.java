package com.knowlearnmap.prompt.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;

//============================================
//3. PromptTestSnapshot Entity
//============================================
@Entity
@Table(name = "prompt_test_snapshots", indexes = {
        @Index(name = "idx_snapshots_version", columnList = "version_id"),
        @Index(name = "idx_snapshots_llm_config", columnList = "llm_config_id"),
        @Index(name = "idx_snapshots_created_at", columnList = "created_datetime"),
        @Index(name = "idx_snapshots_satisfaction", columnList = "satisfaction")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTestSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", length = 100, nullable = false)
    private String code;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "llm_config_id")
    private Long llmConfigId;

    @Column(name = "test_name", length = 200)
    private String testName;

    @Type(JsonBinaryType.class)
    @Column(name = "variables", columnDefinition = "jsonb")
    private String variables;
    /*
     * 예시:
     * {
     * "question": "배송이 늦어지고 있어요",
     * "customer_info": "VIP 고객"
     * }
     */

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "content", nullable = false)
    private String content;

    @Type(JsonBinaryType.class)
    @Column(name = "llm_config", columnDefinition = "jsonb", nullable = false)
    private String llmConfig;
    /*
     * 예시:
     * {
     * "model": "AISTUDIO",
     * "temperature": 0.7,
     * "top_p": 0.95,
     * "max_tokens": 2000,
     * "top_k": 40
     * }
     */

    @Type(JsonBinaryType.class)
    @Column(name = "response", columnDefinition = "jsonb")
    private String response;
    /*
     * 예시:
     * {
     * "text": "고객님의 주문 건에 대해...",
     * "tokens_used": 150,
     * "latency_ms": 1200
     * }
     */

    @Column(name = "satisfaction")
    private Integer satisfaction; // 1-5

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "notes")
    private String notes;

    // 생성 정보
    @Column(name = "created_id", nullable = false, updatable = false, length = 255)
    private String createdId;

    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createdDatetime;

    @PrePersist
    public void prePersist() {
        this.createdDatetime = LocalDateTime.now();
        if (this.createdId == null) {
            this.createdId = "admin";
        }
    }
}
