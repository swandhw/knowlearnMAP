package com.knowlearnmap.workspace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Workspace 엔티티
 * 
 * 워크스페이스(Notebook) 정보를 저장하는 JPA 엔티티
 * - 프론트엔드의 "Notebook"과 동일한 개념
 * - 향후 RAG, LLM, ArangoDB 연동을 위한 필드 포함
 */
@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: 사용자 인증 구현 후 (name + created_by)로 unique 제약 변경
    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    /**
     * UI 아이콘 (emoji 또는 아이콘 코드)
     * 예: "📄", "👥", "🙏"
     */
    @Column(length = 50)
    private String icon;

    /**
     * UI 테마 색상
     * 예: "yellow", "blue", "purple", "default"
     */
    @Column(length = 50)
    private String color;

    /**
     * 활성 상태 (soft delete 용도)
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * 워크스페이스 타입 (향후 확장용)
     * 예: "STRUCTURED", "UNSTRUCTURED"
     */
    @Column(name = "workspace_type", length = 50)
    private String workspaceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private com.knowlearnmap.domain.domain.DomainEntity domain;

    /**
     * 파일 저장 폴더명 (UUID 기반으로 자동 생성)
     * 사용자별로 고유하게 유지
     */
    @Column(name = "folder_name", unique = true, length = 255)
    private String folderName;

    /**
     * LLM 프롬프트 코드 (향후 LLM 연동용)
     */
    @Column(name = "prompt_code", length = 100)
    private String promptCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        // folderName이 없으면 UUID로 고유하게 생성
        if (folderName == null || folderName.isEmpty()) {
            folderName = java.util.UUID.randomUUID().toString();
        }
    }

    /**
     * 폴더명으로 사용할 수 없는 문자 제거 (더 이상 사용 안함)
     */
    @Deprecated
    private String sanitizeFolderName(String name) {
        if (name == null)
            return null;
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    public WorkspaceEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
