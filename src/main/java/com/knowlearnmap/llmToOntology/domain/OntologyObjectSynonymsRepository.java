package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OntologyObjectSynonyms 리포지토리
 */
@Repository
public interface OntologyObjectSynonymsRepository extends JpaRepository<OntologyObjectSynonyms, Long> {

    /**
     * workspace와 object dict, 동의어로 조회
     */
	 // 동의어 검색
    Optional<OntologyObjectSynonyms> findByWorkspaceIdAndCategoryAndSynonym(Long workspaceId, String category, String synonym);


    /**
     * workspace별 목록 조회
     */
    List<OntologyObjectSynonyms> findByWorkspaceId(Long workspaceId);

    /**
     * object dict로 조회
     */
    List<OntologyObjectSynonyms> findByObjectId(Long objectId);
}

