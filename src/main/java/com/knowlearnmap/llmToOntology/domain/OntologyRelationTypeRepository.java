package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OntologyRelationType 리포지토리
 */
@Repository
public interface OntologyRelationTypeRepository extends JpaRepository<OntologyRelationType2, Long> {

    /**
     * workspace와 relation dict로 조회
     */
    Optional<OntologyRelationType2> findByWorkspaceIdAndRelationDictId(Long workspaceId, Long relationDictId);

    /**
     * workspace별 목록 조회
     */
    List<OntologyRelationType2> findByWorkspaceId(Long workspaceId);

    /**
     * relation dict로 조회
     */
    List<OntologyRelationType2> findByRelationDictId(Long relationDictId);
}

