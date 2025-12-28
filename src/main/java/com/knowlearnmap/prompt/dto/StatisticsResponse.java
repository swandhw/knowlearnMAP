package com.knowlearnmap.prompt.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {
    private Integer totalVersions;
    private Integer publishedVersions;
    private Integer draftVersions;
    private Integer totalTests;
    private BigDecimal avgSatisfaction;
    private LocalDateTime lastTestDate;
}
