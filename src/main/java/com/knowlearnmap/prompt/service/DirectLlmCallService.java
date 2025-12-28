package com.knowlearnmap.prompt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectLlmCallService {

    private final ObjectMapper objectMapper;
    private final GeminiSdkService geminiSdkService;

    // Google AI Studio (Gemini)
    @Value("${spring.ai.aistudio.api-key:}")
    private String aistudioApiKey;

    // OpenAI
    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    // Anthropic
    @Value("${spring.ai.anthropic.api-key:}")
    private String anthropicApiKey;

    private static final int MAX_RETRY = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * LLM 호출 (모델에 따라 자동 분기)
     */
    public String callLlm(String llmModel, String prompt, double temperature, double topP,
            int maxOutputTokens, int topK, int n) {
        if ("AISTUDIO".equalsIgnoreCase(llmModel)) {
            return callAistudioWithRetry(prompt, temperature, topP, maxOutputTokens, topK);
        } else if (llmModel.toUpperCase().startsWith("GEMINI")) {
            // LangChain4j SDK를 통한 Gemini 호출 (Generic)
            // GEMINI-2.5-PRO -> gemini-1.5-pro (자동 보정)
            String targetModel = llmModel.toLowerCase().replace("_", "-");
            if (targetModel.contains("2-5")) {
                targetModel = targetModel.replace("2-5", "2.5");
            }
            log.info("Gemini SDK 호출 -> 모델명: {}", targetModel);
            return geminiSdkService.callGemini25Pro(targetModel, prompt, temperature, topP, maxOutputTokens, topK);
        } else if ("OPENAI".equalsIgnoreCase(llmModel) || "GPT4".equalsIgnoreCase(llmModel)) {
            return callOpenAiWithRetry(prompt, temperature, maxOutputTokens, n);
        } else if ("ANTHROPIC".equalsIgnoreCase(llmModel)) {
            return callAnthropicWithRetry(prompt, temperature, maxOutputTokens);
        } else {
            throw new RuntimeException("지원하지 않는 LLM 모델입니다: " + llmModel);
        }
    }

    /**
     * AISTUDIO 호출 (재시도 포함)
     */
    private String callAistudioWithRetry(String prompt, double temperature, double topP,
            int maxOutputTokens, int topK) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                log.info("AISTUDIO 호출 시도 {}/{}", attempt, MAX_RETRY);
                return callAistudio(prompt, temperature, topP, maxOutputTokens, topK);
            } catch (Exception e) {
                log.warn("AISTUDIO 호출 실패 (시도 {}/{}): {}", attempt, MAX_RETRY, e.getMessage());
                lastException = e;

                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // 지수 백오프
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 대기 중 인터럽트 발생", ie);
                    }
                }
            }
        }

        throw new RuntimeException("AISTUDIO 호출 실패 (" + MAX_RETRY + "회 시도)", lastException);
    }

    /**
     * AISTUDIO 직접 호출
     */
    private String callAistudio(String prompt, double temperature, double topP,
            int maxOutputTokens, int topK) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // API 요청 바디 구성
        Map<String, Object> requestBody = new HashMap<>();

        Map<String, Object> contents = new HashMap<>();
        contents.put("role", "user");

        Map<String, String> parts = new HashMap<>();
        parts.put("text", prompt);
        contents.put("parts", new Map[] { parts });

        requestBody.put("contents", new Map[] { contents });

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("topP", topP);
        generationConfig.put("topK", topK);
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        requestBody.put("generationConfig", generationConfig);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // API 키 확인
        if (aistudioApiKey == null || aistudioApiKey.isEmpty()) {
            throw new IOException("AISTUDIO API 키가 설정되지 않았습니다. application.properties에 api.key.aistudio를 설정해주세요.");
        }

        // Google AI Studio API 엔드포인트 (v1 + gemini-1.5-flash)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                + aistudioApiKey;

        log.info("AISTUDIO 요청 URL: {}", url.replace(aistudioApiKey, "***"));
        log.info("AISTUDIO 요청 Body: {}", jsonBody);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("AISTUDIO API 호출 실패: {} - {} | Body: {}", response.code(), response.message(), errorBody);
                throw new IOException("AISTUDIO API 호출 실패: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            log.info("AISTUDIO 응답: {}", responseBody);

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 응답 파싱
            JsonNode candidatesNode = jsonNode.get("candidates");
            if (candidatesNode != null && candidatesNode.isArray() && candidatesNode.size() > 0) {
                JsonNode candidate = candidatesNode.get(0);

                // finishReason 확인
                JsonNode finishReasonNode = candidate.get("finishReason");
                if (finishReasonNode != null && "MAX_TOKENS".equals(finishReasonNode.asText())) {
                    log.warn("AISTUDIO 응답이 토큰 제한으로 잘렸습니다 (MAX_TOKENS). maxOutputTokens를 늘려보세요.");
                }

                JsonNode contentNode = candidate.get("content");
                if (contentNode != null) {
                    JsonNode partsNode = contentNode.get("parts");
                    if (partsNode != null && partsNode.isArray() && partsNode.size() > 0) {
                        JsonNode textNode = partsNode.get(0).get("text");
                        if (textNode != null) {
                            return textNode.asText();
                        }
                    } else {
                        // parts가 없는 경우 (예: MAX_TOKENS로 인해 텍스트 생성 전 중단)
                        log.warn("AISTUDIO 응답에 parts가 없습니다. finishReason: {}", finishReasonNode);
                        if (finishReasonNode != null && "MAX_TOKENS".equals(finishReasonNode.asText())) {
                            throw new IOException("AISTUDIO 응답 실패: 토큰 제한 초과 (MAX_TOKENS) - 텍스트가 생성되지 않았습니다.");
                        }
                    }
                }
            }

            log.error("AISTUDIO 응답 파싱 실패. JSON 구조: {}", jsonNode.toPrettyString());
            throw new IOException("AISTUDIO 응답 파싱 실패");
        }
    }

    /**
     * OPENAI 호출 (재시도 포함)
     */
    private String callOpenAiWithRetry(String prompt, double temperature, int maxTokens, int n) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                log.info("OPENAI 호출 시도 {}/{}", attempt, MAX_RETRY);
                return callOpenAi(prompt, temperature, maxTokens, n);
            } catch (Exception e) {
                log.warn("OPENAI 호출 실패 (시도 {}/{}): {}", attempt, MAX_RETRY, e.getMessage());
                lastException = e;

                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 대기 중 인터럽트 발생", ie);
                    }
                }
            }
        }

        throw new RuntimeException("OPENAI 호출 실패 (" + MAX_RETRY + "회 시도)", lastException);
    }

    /**
     * OPENAI 직접 호출
     */
    private String callOpenAi(String prompt, double temperature, int maxTokens, int n) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // API 요청 바디 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4");

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new Map[] { message });

        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("n", n);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // API 키 확인
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            throw new IOException("OPENAI API 키가 설정되지 않았습니다. application.properties에 api.key.openai를 설정해주세요.");
        }

        log.info("OPENAI 요청 Body: {}", jsonBody);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + openaiApiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("OPENAI API 호출 실패: {} - {} | Body: {}", response.code(), response.message(), errorBody);
                throw new IOException("OPENAI API 호출 실패: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            log.info("OPENAI 응답: {}", responseBody);

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 응답 파싱
            JsonNode choicesNode = jsonNode.get("choices");
            if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).get("message");
                if (messageNode != null) {
                    JsonNode contentNode = messageNode.get("content");
                    if (contentNode != null) {
                        return contentNode.asText();
                    }
                }
            }

            log.error("OPENAI 응답 파싱 실패. JSON 구조: {}", jsonNode.toPrettyString());
            throw new IOException("OPENAI 응답 파싱 실패");
        }
    }

    /**
     * ANTHROPIC 호출 (재시도 포함)
     */
    private String callAnthropicWithRetry(String prompt, double temperature, int maxTokens) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                log.info("ANTHROPIC 호출 시도 {}/{}", attempt, MAX_RETRY);
                return callAnthropic(prompt, temperature, maxTokens);
            } catch (Exception e) {
                log.warn("ANTHROPIC 호출 실패 (시도 {}/{}): {}", attempt, MAX_RETRY, e.getMessage());
                lastException = e;

                if (attempt < MAX_RETRY) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 대기 중 인터럽트 발생", ie);
                    }
                }
            }
        }

        throw new RuntimeException("ANTHROPIC 호출 실패 (" + MAX_RETRY + "회 시도)", lastException);
    }

    /**
     * ANTHROPIC 직접 호출
     */
    private String callAnthropic(String prompt, double temperature, int maxTokens) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // API 요청 바디 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-3-haiku-20240307");
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new Map[] { message });

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // API 키 확인
        if (anthropicApiKey == null || anthropicApiKey.isEmpty()) {
            throw new IOException("ANTHROPIC API 키가 설정되지 않았습니다. application.yml에 spring.ai.anthropic.api-key를 설정해주세요.");
        }

        log.info("ANTHROPIC 요청 Body: {}", jsonBody);

        Request request = new Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .post(RequestBody.create(jsonBody, JSON))
                .addHeader("Content-Type", "application/json")
                .addHeader("x-api-key", anthropicApiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                log.error("ANTHROPIC API 호출 실패: {} - {} | Body: {}", response.code(), response.message(), errorBody);
                throw new IOException("ANTHROPIC API 호출 실패: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            log.info("ANTHROPIC 응답: {}", responseBody);

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 응답 파싱
            JsonNode contentNode = jsonNode.get("content");
            if (contentNode != null && contentNode.isArray() && contentNode.size() > 0) {
                JsonNode textNode = contentNode.get(0).get("text");
                if (textNode != null) {
                    return textNode.asText();
                }
            }

            log.error("ANTHROPIC 응답 파싱 실패. JSON 구조: {}", jsonNode.toPrettyString());
            throw new IOException("ANTHROPIC 응답 파싱 실패");
        }
    }
}
