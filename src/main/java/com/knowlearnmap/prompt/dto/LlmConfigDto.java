package com.knowlearnmap.prompt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmConfigDto {
    private String model;
    private Double temperature;
    private Double topP;
    private Integer maxOutputTokens;
    private Integer topK;
    private Integer n;
}
