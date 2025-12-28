package com.knowlearnmap.prompt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSetResponse {
    private Long id;
    private Long versionId;
    private String testName;
    private String variables;
    private String createdId;
    private LocalDateTime createdDatetime;
    private LlmConfigDto llmConfig;
    private Integer snapshotCount; // 이 테스트 셋으로 실행된 스냅샷 개수
}
