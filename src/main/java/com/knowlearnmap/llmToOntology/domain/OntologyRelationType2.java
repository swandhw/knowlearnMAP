package com.knowlearnmap.llmToOntology.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ontology Relation Type 엔티티
 * 
 * <p>
 * OntologyRelationDict의 실제 인스턴스를 저장합니다.
 * </p>
 */
@Entity
@Table(name = "ontology_relation_type", uniqueConstraints = {
        @UniqueConstraint(name = "uk_relation_type_workspace_dict", columnNames = { "workspace_id",
                "relation_dict_id" })
}, indexes = {
        @Index(name = "idx_relation_type_workspace", columnList = "workspace_id"),
        @Index(name = "idx_relation_type_dict", columnList = "relation_dict_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OntologyRelationType2 {

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
     * Relation Dictionary ID
     */
    @Column(name = "relation_dict_id", nullable = false)
    private Long relationDictId;

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

