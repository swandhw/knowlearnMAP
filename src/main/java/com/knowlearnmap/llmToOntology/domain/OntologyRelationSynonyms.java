package com.knowlearnmap.llmToOntology.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ontology Relation Synonyms 엔티티
 * 
 * <p>
 * 관계 용어의 동의어를 저장합니다.
 * </p>
 * 
 * <h3>예시</h3>
 * 
 * <pre>
 * relation_dict_id: 456 (포함하다)
 * synonym: "함유하다"
 * </pre>
 */
@Entity
@Table(name = "ontology_relation_synonyms", 
	       uniqueConstraints = {
	               @UniqueConstraint(columnNames = {"workspace_id", "category", "synonym"})
		
}, indexes = {
        @Index(name = "idx_relation_syn_workspace", columnList = "workspace_id"),
        @Index(name = "idx_relation_syn_dict", columnList = "relation_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OntologyRelationSynonyms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 워크스페이스 ID
     */
    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;


    @Column(name = "category", length = 200)
    private String category;
    
    @Column(name = "synonym", nullable = false, length = 200)
    private String synonym;
    
    
    /**
     * Relation Dictionary ID
     */
    @Column(name = "relation_id", nullable = false)
    private Long relationId;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    
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

