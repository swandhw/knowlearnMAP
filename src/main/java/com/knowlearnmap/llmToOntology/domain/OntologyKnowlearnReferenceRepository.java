package com.knowlearnmap.llmToOntology.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface OntologyKnowlearnReferenceRepository extends JpaRepository<OntologyKnowlearnReference, Long> {

        boolean existsByOntologyKnowlearnTypeAndDocumentIdAndChunkId(OntologyKnowlearnType ontologyKnowlearnType,
                        Long documentId, Long chunkId);

        @Modifying
        @Transactional
        long deleteByDocumentId(Long documentId);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r.documentId FROM OntologyKnowlearnReference r WHERE r.ontologyKnowlearnType.id = :tripleId")
        java.util.List<Long> findDistinctDocumentIdByOntologyKnowlearnTypeId(
                        @org.springframework.data.repository.query.Param("tripleId") Long tripleId);

        java.util.List<OntologyKnowlearnReference> findByOntologyKnowlearnTypeIdIn(java.util.List<Long> tripleIds);

        java.util.List<OntologyKnowlearnReference> findByOntologyKnowlearnType(
                        OntologyKnowlearnType ontologyKnowlearnType);

        @Modifying
        @Transactional
        void deleteByOntologyKnowlearnType(OntologyKnowlearnType ontologyKnowlearnType);
}
