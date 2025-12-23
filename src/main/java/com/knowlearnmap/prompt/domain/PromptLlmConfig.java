package com.knowlearnmap.prompt.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.time.LocalDateTime;

/**
 * PromptLlmConfig Entity
 * 
 * LLM 설정을 저장하는 엔티티 (구 PromptTestSet)
 * - 재사용 가능한 LLM 설정 관리
 * - 버전별 1개 (Singleton)
 */
@Entity
@Table(name = "prompt_llm_config", indexes = {
        @Index(name = "idx_llm_config_version", columnList = "version_id"),
        @Index(name = "idx_llm_config_created_at", columnList = "created_datetime")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_llm_config_version", columnNames = "version_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptLlmConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "config_name", length = 200)
    private String configName; // Optional description (구 testName)

    @Type(JsonBinaryType.class)
    @Column(name = "variables", columnDefinition = "jsonb", nullable = false)
    private String variables;

    @Type(JsonBinaryType.class)
    @Column(name = "llm_config", columnDefinition = "jsonb")
    private String llmConfig;

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
