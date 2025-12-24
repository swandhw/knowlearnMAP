package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OntologyRelationSynonyms 리포지토리
 */
@Repository
public interface OntologyRelationSynonymsRepository extends JpaRepository<OntologyRelationSynonyms, Long> {

    /**
     * workspace와 relation dict, 동의어로 조회
     */
    Optional<OntologyRelationSynonyms> findByWorkspaceIdAndCategoryAndSynonym(Long workspaceId, String category, String synonym);

    /**
     * workspace별 목록 조회
     */
    List<OntologyRelationSynonyms> findByWorkspaceId(Long workspaceId);

    /**
     * relation dict로 조회
     */
    List<OntologyRelationSynonyms> findByRelationId(Long relationId);
    
}

