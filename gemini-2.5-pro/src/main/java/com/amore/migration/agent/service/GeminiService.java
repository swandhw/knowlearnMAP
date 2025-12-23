package com.amore.migration.agent.service;

import com.amore.migration.agent.config.MigrationConfig;
import com.amore.migration.agent.dto.GeminiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

  private final MigrationConfig config;
  private final ObjectMapper objectMapper;
  private GoogleAiGeminiChatModel model;

  @PostConstruct
  public void init() {
    String apiKey = System.getenv(config.getGemini().getApiKeyEnvVar());
    if (apiKey == null || apiKey.isEmpty()) {
      apiKey = config.getGemini().getApiKeyEnvVar(); // Fallback to raw value
    }

    if (apiKey != null && apiKey.length() > 8) {
      log.info("Using API Key starting with: {}...", apiKey.substring(0, 8));
    }

    model = GoogleAiGeminiChatModel.builder()
        .apiKey(apiKey)
        .modelName(config.getGemini().getModelName())
        .timeout(java.time.Duration.ofMinutes(5)) // Increase to 5 minutes
        .build();
  }

  public GeminiResponse generateMigrationCode(String prompt) {
    log.info("Sending prompt to Gemini...");
    String responseText = model.generate(prompt);
    log.info("Received response from Gemini (truncated): {}...",
        responseText.substring(0, Math.min(responseText.length(), 500)));

    return parseJsonResponse(responseText);
  }

  private GeminiResponse parseJsonResponse(String responseText) {
    try {
      String jsonStr;
      Pattern pattern = Pattern.compile("```json\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
      Matcher matcher = pattern.matcher(responseText);

      if (matcher.find()) {
        jsonStr = matcher.group(1);
      } else {
        int start = responseText.indexOf("{");
        int end = responseText.lastIndexOf("}");
        if (start != -1 && end != -1) {
          jsonStr = responseText.substring(start, end + 1);
        } else {
          throw new RuntimeException("No JSON found in Gemini response");
        }
      }

      return objectMapper.readValue(jsonStr, GeminiResponse.class);
    } catch (Exception e) {
      log.error("Failed to parse Gemini JSON response", e);
      throw new RuntimeException("JSON parsing error", e);
    }
  }
}
