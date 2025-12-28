package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OntologyRelationDict 리포지토리
 */
@Repository
public interface OntologyRelationDictRepository extends JpaRepository<OntologyRelationDict, Long> {

        /**
         * workspace와 카테고리, 영문 관계명으로 조회
         */
        Optional<OntologyRelationDict> findByWorkspaceIdAndCategoryAndRelationEn(
                        Long workspaceId, String category, String relationEn);

        Optional<OntologyRelationDict> findByWorkspaceIdAndCategoryAndRelationKo(
                        Long workspaceId, String category, String relationKo);

        /**
         * workspace별 목록 조회 (Paging)
         */
        org.springframework.data.domain.Page<OntologyRelationDict> findByWorkspaceId(Long workspaceId,
                        org.springframework.data.domain.Pageable pageable);

        /**
         * workspace별 목록 조회
         */
        List<OntologyRelationDict> findByWorkspaceId(Long workspaceId);

        /**
         * workspace와 카테고리로 조회
         */
        List<OntologyRelationDict> findByWorkspaceIdAndCategory(Long workspaceId, String category);

        List<OntologyRelationDict> findBySourceContaining(String sourcePattern);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r.category FROM OntologyRelationDict r WHERE r.workspaceId = :workspaceId AND r.category IS NOT NULL ORDER BY r.category ASC")
        List<String> findDistinctCategoriesByWorkspaceId(
                        @org.springframework.data.repository.query.Param("workspaceId") Long workspaceId);
}
