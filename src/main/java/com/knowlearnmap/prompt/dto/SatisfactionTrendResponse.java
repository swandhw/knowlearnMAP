package com.knowlearnmap.prompt.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SatisfactionTrendResponse {
    private Integer version;
    private BigDecimal avgSatisfaction;
    private Integer testCount;
}
