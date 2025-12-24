package com.knowlearnmap.document.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import org.hibernate.type.SqlTypes;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DocumentChunk 엔티티
 * 
 * 문서를 분할한 청크 단위 데이터
 * - 벡터 임베딩 대상
 * - LLM 처리 및 Ontology 추출 대상
 */
@Entity
@Table(name = "document_chunks", indexes = {
        @Index(name = "idx_chunk_document", columnList = "document_id"),
        @Index(name = "idx_chunk_llm_status", columnList = "llm_status")
})
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    /**
     * 청크 내용 (임베딩 대상)
     */
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(nullable = false, length = 32600)
    private String content;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    /**
     * 원본 페이지/섹션 참조
     */
    @Column(name = "page_number")
    private Integer pageNumber;

    // ===== Vector Embedding =====

    /**
     * 벡터 임베딩 데이터 (JSON 형식으로 저장)
     * pgvector 사용 시 vector 타입으로 캐스팅하여 사용 가능
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "embedding_vector", columnDefinition = "jsonb")
    private List<Double> embedding;

    // ===== LLM 처리 관련 =====

    /**
     * LLM 처리 결과 (JSON 형식)
     */
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "llm_result", length = 32600)
    private String llmResult;

    /**
     * LLM 처리 상태
     * - null: 미처리
     * - PROCESSING: 처리 중
     * - COMPLETED: 완료
     * - FAILED: 실패
     */
    @Column(name = "llm_status", length = 20)
    private String llmStatus;

    @Column(name = "llm_processed_at")
    private LocalDateTime llmProcessedAt;

    @Column(name = "llm_error_message", length = 500)
    private String llmErrorMessage;

    // ===== Ontology 처리 관련 =====

    @Column(name = "ontology_status", length = 20)
    private String ontologyStatus;

    @Column(name = "ontology_processed_at")
    private LocalDateTime ontologyProcessedAt;

    @Column(name = "ontology_error_message", length = 500)
    private String ontologyErrorMessage;

    public DocumentChunk(DocumentEntity document, String content, Integer chunkIndex, Integer pageNumber) {
        this.document = document;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
    }
}
