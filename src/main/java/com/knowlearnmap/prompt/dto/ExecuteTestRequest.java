package com.knowlearnmap.prompt.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteTestRequest {
    private Long versionId;
    private Map<String, Object> variables;
    private LlmConfigDto llmConfig;
}
