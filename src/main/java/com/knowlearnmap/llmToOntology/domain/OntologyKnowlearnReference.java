package com.knowlearnmap.llmToOntology.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ontology_knowlearn_reference", indexes = {
        @Index(name = "idx_kl_ref_doc_id", columnList = "document_id"),
        @Index(name = "idx_kl_ref_chunk_id", columnList = "chunk_id")
})
public class OntologyKnowlearnReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ontology_knowlearn_id", nullable = false)
    private OntologyKnowlearnType ontologyKnowlearnType;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;
}
