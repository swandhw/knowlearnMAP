package com.knowlearnmap.prompt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmDirectCallResponse {
    private Boolean success;
    private String text;
    private Long snapshotId;  // ?ㅻ깄??ID (??λ맂 寃쎌슦)
    private Integer tokensUsed;
    private Long latencyMs;
}
