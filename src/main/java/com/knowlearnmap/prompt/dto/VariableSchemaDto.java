package com.knowlearnmap.prompt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariableSchemaDto {
    private String key;
    private String type;
    private Boolean required;
    private String defaultValue;
    private String description;
    private String placeholder;
    private String content;  // ?ㅼ젣 ?뚯뒪??媛?(optional)
}
