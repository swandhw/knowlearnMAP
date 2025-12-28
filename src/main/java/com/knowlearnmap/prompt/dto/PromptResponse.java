package com.knowlearnmap.prompt.dto;

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
public class PromptResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private List<String> tags;
    private Boolean isActive;
    private Integer activeVersion;
    private Integer versionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
