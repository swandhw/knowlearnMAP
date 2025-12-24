package com.knowlearnmap.llmToOntology.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ontology Object Type 엔티티
 * 
 * <p>
 * OntologyObjectDict의 실제 인스턴스를 저장합니다.
 * </p>
 * 
 * <h3>관계</h3>
 * 
 * <pre>
 * OntologyObjectDict (사전) --1:N--> OntologyObjectType (인스턴스)
 * 예: "비타민C" (사전) --> "비타민C#1", "비타민C#2" (인스턴스)
 * </pre>
 */
@Entity
@Table(name = "ontology_object_type", uniqueConstraints = {
        @UniqueConstraint(name = "uk_object_type_workspace_dict", columnNames = { "workspace_id", "object_dict_id" })
}, indexes = {
        @Index(name = "idx_object_type_workspace", columnList = "workspace_id"),
        @Index(name = "idx_object_type_dict", columnList = "object_dict_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OntologyObjectType2 {

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
     * Object Dictionary ID (참조)
     */
    @Column(name = "object_dict_id", nullable = false)
    private Long objectDictId;

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

