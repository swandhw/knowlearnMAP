package com.knowlearnmap.llmToOntology.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryDto {
    private Long id;
    private Long workspaceId;

    // Common Fields
    private String label; // matches termKo or relationKo
    private String labelEn; // matches termEn or relationEn
    private String category;
    private String description;
    private String status;
    private String source;

    // Synonyms (Consolidated from sub-tables)
    private String synonym; // JSON string or comma separated for UI simplicity

    private String type; // "concept" or "relation"
}
