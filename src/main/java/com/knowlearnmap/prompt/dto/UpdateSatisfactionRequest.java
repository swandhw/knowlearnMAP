package com.knowlearnmap.prompt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSatisfactionRequest {
    private Integer satisfaction;
    private String notes;
}
