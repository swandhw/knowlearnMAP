package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OntologyObjectDict 리포지토리
 */
@Repository
public interface OntologyObjectDictRepository extends JpaRepository<OntologyObjectDict, Long> {

        /**
         * workspace와 카테고리, 영문 용어로 조회
         */
        Optional<OntologyObjectDict> findByWorkspaceIdAndCategoryAndTermEn(
                        Long workspaceId, String category, String termEn);

        Optional<OntologyObjectDict> findByWorkspaceIdAndCategoryAndTermKo(
                        Long workspaceId, String category, String termKo);

        /**
         * workspace별 목록 조회 (Paging)
         */
        org.springframework.data.domain.Page<OntologyObjectDict> findByWorkspaceId(Long workspaceId,
                        org.springframework.data.domain.Pageable pageable);

        /**
         * workspace별 목록 조회
         */
        List<OntologyObjectDict> findByWorkspaceId(Long workspaceId);

        /**
         * workspace와 카테고리로 조회
         */
        Optional<OntologyObjectDict> findById(Long id);

        /**
         * Delete Orphaned Synonyms (Objects with no refs and no usage)
         */
        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        @org.springframework.data.jpa.repository.Query("DELETE FROM OntologyObjectSynonyms s WHERE s.objectId IN (SELECT o.id FROM OntologyObjectDict o WHERE o.references IS EMPTY AND NOT EXISTS (SELECT k FROM OntologyKnowlearnType k WHERE k.subjectId = o.id OR k.objectId = o.id))")
        void deleteOrphanSynonyms();

        /**
         * Delete Orphaned Objects (No references and no usage)
         */
        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        @org.springframework.data.jpa.repository.Query("DELETE FROM OntologyObjectDict o WHERE o.references IS EMPTY AND NOT EXISTS (SELECT k FROM OntologyKnowlearnType k WHERE k.subjectId = o.id OR k.objectId = o.id)")
        void deleteOrphans();

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM OntologyObjectDict o JOIN o.references r WHERE o.workspaceId = :workspaceId AND r.documentId IN :documentIds")
        org.springframework.data.domain.Page<OntologyObjectDict> findByWorkspaceIdAndDocumentIds(
                        @org.springframework.data.repository.query.Param("workspaceId") Long workspaceId,
                        @org.springframework.data.repository.query.Param("documentIds") java.util.List<Long> documentIds,
                        org.springframework.data.domain.Pageable pageable);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM OntologyObjectDict o JOIN o.references r WHERE o.workspaceId = :workspaceId AND r.documentId IN :documentIds")
        List<OntologyObjectDict> findByWorkspaceIdAndDocumentIds(
                        @org.springframework.data.repository.query.Param("workspaceId") Long workspaceId,
                        @org.springframework.data.repository.query.Param("documentIds") java.util.List<Long> documentIds);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o.category FROM OntologyObjectDict o JOIN o.references r WHERE o.workspaceId = :workspaceId AND r.documentId IN :documentIds AND o.category IS NOT NULL ORDER BY o.category ASC")
        List<String> findDistinctCategoriesByWorkspaceIdAndDocumentIds(
                        @org.springframework.data.repository.query.Param("workspaceId") Long workspaceId,
                        @org.springframework.data.repository.query.Param("documentIds") java.util.List<Long> documentIds);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o.category FROM OntologyObjectDict o WHERE o.workspaceId = :workspaceId AND o.category IS NOT NULL ORDER BY o.category ASC")
        List<String> findDistinctCategoriesByWorkspaceId(
                        @org.springframework.data.repository.query.Param("workspaceId") Long workspaceId);
}
