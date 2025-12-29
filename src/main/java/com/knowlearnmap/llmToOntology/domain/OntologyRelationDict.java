package com.knowlearnmap.llmToOntology.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Ontology Relation Dictionary 엔티티
 * 
 * <p>
 * 온톨로지에서 사용되는 관계(Relation) 용어 사전입니다.
 * </p>
 * 
 * <h3>예시</h3>
 * 
 * <pre>
 * category: "contains"
 * relation_en: "contains"
 * relation_ko: "포함하다"
 * description: "성분이 제품에 포함되는 관계"
 * </pre>
 */
@Entity
@Table(name = "ontology_relation_dict", uniqueConstraints = {
        @UniqueConstraint(name = "uk_relation_workspace_category_en", columnNames = { "workspace_id", "category",
                "relation_en" }),
        @UniqueConstraint(name = "uk_relation_workspace_category_ko", columnNames = { "workspace_id", "category",
                "relation_ko" })
}, indexes = {
        @Index(name = "idx_relation_workspace", columnList = "workspace_id"),
        @Index(name = "idx_relation_category", columnList = "category")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OntologyRelationDict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 워크스페이스 ID
     */
    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    /**
     * 영문 관계명
     */
    @Column(name = "relation_en", nullable = false, length = 200)
    private String relationEn;

    /**
     * 한글 관계명
     */
    @Column(name = "relation_ko", nullable = false, length = 200)
    private String relationKo;

    /**
     * 설명
     */
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "description")
    private String description;

    /**
     * 카테고리
     */
    @Column(name = "category", length = 200)
    private String category;

    /**
     * 상태
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ontologyRelationDict", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<OntologyRelationReference> references = new java.util.ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = "active";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
