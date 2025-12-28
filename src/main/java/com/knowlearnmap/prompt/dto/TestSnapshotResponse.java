package com.knowlearnmap.prompt.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSnapshotResponse {
    private Long id;
    private Long versionId;
    private String testName;
    private String content;
    private Map<String, Object> variables;
    private LlmConfigDto llmConfig;
    private TestResponseDto response;
    private Integer satisfaction;
    private String notes;
    private LocalDateTime createdAt;
}
