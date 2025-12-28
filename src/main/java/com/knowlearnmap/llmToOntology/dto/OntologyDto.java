package com.knowlearnmap.llmToOntology.dto;

import lombok.Data;

/**
 * Ontology DTO
 * 
 * <p>
 * Subject-Relation-Object 트리플 데이터 전송 객체
 * </p>
 */
@Data
public class OntologyDto {
    private String subjectCategory;
    private String subjectTermEn;
    private String subjectTermKo;

    private String relationCategory;
    private String relationEn;
    private String relationKo;

    private String objectCategory;
    private String objectTermEn;
    private String objectTermKo;

    private java.math.BigDecimal confidenceScore;
    private String evidenceLevel;
}

