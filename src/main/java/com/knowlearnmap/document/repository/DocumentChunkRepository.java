package com.knowlearnmap.document.repository;

import com.knowlearnmap.document.domain.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

        /**
         * 문서의 모든 청크 조회 (청크 인덱스 순)
         */
        List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId);

        /**
         * 문서의 청크 개수 조회
         */
        long countByDocumentId(Long documentId);

        /**
         * LLM 미처리 청크 조회
         */
        @Query("SELECT c FROM DocumentChunk c WHERE c.document.id = :documentId " +
                        "AND (c.llmStatus IS NULL OR c.llmStatus = 'FAILED') " +
                        "ORDER BY c.chunkIndex")
        List<DocumentChunk> findPendingLlmChunks(@Param("documentId") Long documentId);

        /**
         * LLM 완료된 청크 조회
         */
        @Query("SELECT c FROM DocumentChunk c WHERE c.document.id = :documentId " +
                        "AND c.llmStatus = 'COMPLETED' " +
                        "ORDER BY c.chunkIndex")
        List<DocumentChunk> findCompletedLlmChunks(@Param("documentId") Long documentId);

        /**
         * Ontology 미처리 청크 조회
         */
        @Query("SELECT c FROM DocumentChunk c WHERE c.document.id = :documentId " +
                        "AND c.llmStatus = 'COMPLETED' " +
                        "AND (c.ontologyStatus IS NULL OR c.ontologyStatus = 'FAILED') " +
                        "ORDER BY c.chunkIndex")
        List<DocumentChunk> findPendingOntologyChunks(@Param("documentId") Long documentId);

        // LlmToOntologyService에서 사용
        List<DocumentChunk> findByDocumentIdAndLlmStatus(Long documentId, String llmStatus);

        List<DocumentChunk> findByLlmStatus(String llmStatus);

        // ChunkToLlmService에서 사용
        List<DocumentChunk> findByLlmStatusIsNull();

        List<DocumentChunk> findByDocumentId(Long documentId, org.springframework.data.domain.Pageable pageable);

        @Query("SELECT c.id, c.document.id FROM DocumentChunk c")
        List<Object[]> findAllChunkIdAndDocumentId();
}
