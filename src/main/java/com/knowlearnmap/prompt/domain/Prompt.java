package com.knowlearnmap.prompt.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.sql.Types;
import java.time.LocalDateTime;

// ============================================
// 1. Prompt Entity
// ============================================
@Entity
@Table(name = "prompt_prompts", indexes = {
        @Index(name = "idx_prompts_code", columnList = "code"),
        @Index(name = "idx_prompts_active", columnList = "is_active")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", length = 100, unique = true, nullable = false)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "description")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(name = "tags", columnDefinition = "jsonb")
    private String tags; // JSON string: {"team": "backend", "domain": "customer"}

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "publish_version_id")
    private Long publishVersionId;

    // 기본정보 생성 및 수정 정보 (공통 컬럼)
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
        if (this.isActive == null) {
            this.isActive = true;
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
