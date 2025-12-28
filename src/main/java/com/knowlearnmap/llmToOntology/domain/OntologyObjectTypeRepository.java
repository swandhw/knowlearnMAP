package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OntologyObjectType 리포지토리
 */
@Repository
public interface OntologyObjectTypeRepository extends JpaRepository<OntologyObjectType2, Long> {

    /**
     * workspace와 object dict로 조회
     */
    Optional<OntologyObjectType2> findByWorkspaceIdAndObjectDictId(Long workspaceId, Long objectDictId);

    /**
     * workspace별 목록 조회
     */
    List<OntologyObjectType2> findByWorkspaceId(Long workspaceId);

    /**
     * object dict로 조회
     */
    List<OntologyObjectType2> findByObjectDictId(Long objectDictId);
}

