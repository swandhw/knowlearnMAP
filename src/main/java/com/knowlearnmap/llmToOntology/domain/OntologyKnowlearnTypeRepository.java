package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * OntologyKnowlearnType 리포지토리
 */
@Repository
public interface OntologyKnowlearnTypeRepository extends JpaRepository<OntologyKnowlearnType, Long> {

    /**
     * workspace와 트리플로 조회
     */
    Optional<OntologyKnowlearnType> findByWorkspaceIdAndSubjectIdAndRelationIdAndObjectId(
            Long workspaceId, Long subjectId, Long relationId, Long objectId);

    /**
     * workspace별 목록 조회
     */
    List<OntologyKnowlearnType> findByWorkspaceId(Long workspaceId);

    /**
     * workspace와 subject로 조회
     */
    List<OntologyKnowlearnType> findByWorkspaceIdAndSubjectId(Long workspaceId, Long subjectId);

    /**
     * workspace와 object로 조회
     */
    List<OntologyKnowlearnType> findByWorkspaceIdAndObjectId(Long workspaceId, Long objectId);

}

