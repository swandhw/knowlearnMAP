package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface OntologyRelationReferenceRepository extends JpaRepository<OntologyRelationReference, Long> {

    boolean existsByOntologyRelationDictAndDocumentIdAndChunkId(OntologyRelationDict ontologyRelationDict,
            Long documentId, Long chunkId);

    @Modifying
    @Transactional
    long deleteByDocumentId(Long documentId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r.documentId FROM OntologyRelationReference r WHERE r.ontologyRelationDict.id = :relationId")
    java.util.List<Long> findDistinctDocumentIdByOntologyRelationDictId(
            @org.springframework.data.repository.query.Param("relationId") Long relationId);

    java.util.List<OntologyRelationReference> findByOntologyRelationDictIdIn(java.util.List<Long> relationIds);
}
