package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface OntologyObjectReferenceRepository extends JpaRepository<OntologyObjectReference, Long> {

        boolean existsByOntologyObjectDictAndDocumentIdAndChunkId(OntologyObjectDict ontologyObjectDict,
                        Long documentId,
                        Long chunkId);

        @Modifying
        @Transactional
        long deleteByDocumentId(Long documentId);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r.documentId FROM OntologyObjectReference r WHERE r.ontologyObjectDict.id = :objectId")
        java.util.List<Long> findDistinctDocumentIdByOntologyObjectDictId(
                        @org.springframework.data.repository.query.Param("objectId") Long objectId);

        java.util.List<OntologyObjectReference> findByOntologyObjectDictId(Long objectId);

        java.util.List<OntologyObjectReference> findByOntologyObjectDictIdIn(java.util.List<Long> objectIds);

        @Modifying
        @Transactional
        void deleteByOntologyObjectDictId(Long ontologyObjectDictId);
}
