package com.knowlearnmap.llmToOntology.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Ontology Object Dictionary 엔티티
 * 
 * <p>온톨로지에서 사용되는 객체(Object/Subject) 용어 사전입니다.</p>
 * 
 * <h3>예시</h3>
 * <pre>
 * category: "ingredient"
 * term_en: "Vitamin C"
 * term_ko: "비타민C"
 * description: "강력한 항산화 성분"
 * </pre>
 * 
 * <h3>Unique Constraint</h3>
 * workspace_id + category + term_en 조합으로 중복 방지
 */
@Entity
@Table(name = "ontology_object_dict",
   uniqueConstraints = {
       @UniqueConstraint(
           name = "uk_object_workspace_category_en",
           columnNames = {"workspace_id", "category", "term_en"}
       ),
       @UniqueConstraint(
           name = "uk_object_workspace_category_ko",
           columnNames = {"workspace_id", "category", "term_ko"}
       )
   },
   indexes = {
       @Index(name = "idx_object_workspace", columnList = "workspace_id"),
       @Index(name = "idx_object_category", columnList = "category")
   })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OntologyObjectDict {
    
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
     * 영문 용어
     */
    @Column(name = "term_en", nullable = false, length = 200)
    private String termEn;
    
    /**
     * 한글 용어
     */
    @Column(name = "term_ko", nullable = false, length = 200)
    private String termKo;
    
    /**
     * 설명
     */
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "description")
    private String description;
    
    /**
     * 카테고리
     * 예: ingredient, product, effect, skin_type
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
    
    /**
     * 출처 정보 (JSON 배열 형식)
     */
    @Column(name = "source", length = 100)
    @Builder.Default
    private String source = "initial_data";
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        
        if (this.status == null) {
            this.status = "active";
        }
        if (this.source == null) {
            this.source = "initial_data";
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

