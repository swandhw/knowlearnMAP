package com.knowlearnmap.prompt.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVersionRequest {
    private String content;
    private List<VariableSchemaDto> variableSchema;
    private String notes;
    private String status;
}
