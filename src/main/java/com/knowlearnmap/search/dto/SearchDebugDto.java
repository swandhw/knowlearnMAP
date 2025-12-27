package com.knowlearnmap.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDebugDto {
    private List<SearchResult> ragResults;
    private List<SearchResult> aqlResults;
    private List<SearchResult> knowlearnResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String content;
        private Double score;
        private Map<String, Object> metadata;
    }
}
