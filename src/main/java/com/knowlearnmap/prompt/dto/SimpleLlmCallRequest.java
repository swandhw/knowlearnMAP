package com.knowlearnmap.prompt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleLlmCallRequest {
    private java.util.Map<String, Object> variables; // ?꾨＼?꾪듃 蹂??
}
