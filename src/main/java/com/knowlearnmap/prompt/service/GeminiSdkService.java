package com.knowlearnmap.prompt.service;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Gemini SDK를 사용한 LLM 호출 서비스
 * LangChain4j의 GoogleAiGeminiChatModel을 활용
 */
@Slf4j
@Service
public class GeminiSdkService {

    @Value("${spring.ai.aistudio.api-key}")
    private String apiKey;

    /**
     * Gemini 2.5 Pro 모델 호출
     *
     * @param prompt      프롬프트 텍스트
     * @param temperature 온도 (0.0 ~ 2.0, 기본값 1.0)
     * @param topP        Top P (0.0 ~ 1.0)
     * @param maxTokens   최대 출력 토큰 수
     * @param topK        Top K (양수)
     * @return LLM 응답 텍스트
     */
    public String callGemini25Pro(String prompt, double temperature, double topP,
            int maxTokens, int topK) {
        try {
            log.info("Gemini 2.5 Pro 호출 시작 - temperature: {}, topP: {}, maxTokens: {}, topK: {}",
                    temperature, topP, maxTokens, topK);

            // 파라미터를 반영한 모델 생성
            GoogleAiGeminiChatModel model = GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gemini-2.5-pro") // 또는 "gemini-2.5-pro" (사용 가능한 모델명 확인 필요)
                    .temperature(temperature)
                    .topP(topP)
                    .topK(topK)
                    .maxOutputTokens(maxTokens)
                    .timeout(Duration.ofMinutes(5))
                    .build();

            String response = model.generate(prompt);

            log.info("Gemini 2.5 Pro 응답 수신 완료 (length: {})", response.length());

            return response;

        } catch (Exception e) {
            log.error("Gemini 2.5 Pro 호출 실패", e);
            throw new RuntimeException("Gemini 2.5 Pro 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * API 키 확인 (초기화 시)
     */
    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API 키가 설정되지 않았습니다. spring.ai.aistudio.api-key를 확인하세요.");
        } else {
            log.info("Gemini SDK 초기화 완료 - API Key: {}...",
                    apiKey.substring(0, Math.min(8, apiKey.length())));
        }
    }
}
