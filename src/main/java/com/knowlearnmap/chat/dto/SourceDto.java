package com.knowlearnmap.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDto {
    private String type; // RAG, ONTOLOGY_TEXT, ONTOLOGY_VECTOR
    private String content;
    private Double score;
    private Map<String, Object> metadata;
}
