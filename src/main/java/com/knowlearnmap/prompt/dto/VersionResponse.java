package com.knowlearnmap.prompt.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {
    private Long id;
    private String promptCode;
    private Integer version;
    private String content;
    private List<VariableSchemaDto> variableSchema;
    private LlmConfigDto llmConfig;
    private String status;
    private Boolean isActive;
    private String notes;
    private BigDecimal avgSatisfaction;
    private Integer testCount;
    private LocalDateTime lastTestedAt;
    private Integer overallRating;
    private String overallNotes;
    private String createdBy;
    private LocalDateTime createdAt;
}
