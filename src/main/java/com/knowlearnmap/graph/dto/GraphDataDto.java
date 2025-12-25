package com.knowlearnmap.graph.dto;

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
public class GraphDataDto {
    private List<Map<String, Object>> nodes;
    private List<Map<String, Object>> links;
}
