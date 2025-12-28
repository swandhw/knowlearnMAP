package com.knowlearnmap.prompt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmDirectCallRequest {
    private String llmModel;
    private String content;
    private String prompt;
    private Double temperature;
    private Double topP;
    private Integer maxOutputTokens;
    private Integer topK;
    private Integer n;
    private Long versionId;  // ?좏깮?? ?ㅻ깄????μ쓣 ?꾪븳 踰꾩쟾 ID
    private String testName;  // ?좏깮?? ?뚯뒪???대쫫
}
