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
     * workspace별 목록 조회
     */
    List<OntologyObjectDict> findByWorkspaceId(Long workspaceId);

    /**
     * workspace와 카테고리로 조회
     */
    Optional<OntologyObjectDict> findById(Long id);
}

