package com.knowlearnmap.ai.service.impl;

import com.knowlearnmap.ai.service.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI 기반 임베딩 서비스 구현체
 * app.ai.embedding.provider=openai 설정 시 활성화
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.embedding.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiEmbeddingService implements EmbeddingService {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private OpenAiEmbeddingModel embeddingModel;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API 키가 설정되지 않았습니다. spring.ai.openai.api-key를 확인하세요.");
            return;
        }

        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-3-small") // 최신, 가성비 모델
                .timeout(Duration.ofSeconds(60))
                .build();

        log.info("OpenAiEmbeddingService 초기화 완료 (Model: text-embedding-3-small)");
    }

    @Override
    public List<Double> embed(String text) {
        if (embeddingModel == null) {
            throw new IllegalStateException("OpenAI Embedding Model이 초기화되지 않았습니다. API Key를 확인하세요.");
        }

        try {
            Response<Embedding> response = embeddingModel.embed(text);
            List<Float> floatVector = response.content().vectorAsList();

            // Convert Float to Double (Java List<Double> is common for JPA/Calculation)
            return floatVector.stream()
                    .map(Float::doubleValue)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("OpenAI 임베딩 생성 실패", e);
            throw new RuntimeException("임베딩 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        if (embeddingModel == null) {
            throw new IllegalStateException("OpenAI Embedding Model이 초기화되지 않았습니다. API Key를 확인하세요.");
        }

        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        try {
            log.debug("Generating batch embeddings for {} texts", texts.size());

            // Convert strings to TextSegments
            List<TextSegment> segments = texts.stream()
                    .map(TextSegment::from)
                    .collect(Collectors.toList());

            // Single API call for all texts
            Response<List<Embedding>> response = embeddingModel.embedAll(segments);

            // Convert Float to Double for each embedding
            List<List<Double>> result = response.content().stream()
                    .map(embedding -> embedding.vectorAsList().stream()
                            .map(Float::doubleValue)
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());

            log.debug("Successfully generated {} embeddings in batch", result.size());
            return result;

        } catch (Exception e) {
            log.error("OpenAI 배치 임베딩 생성 실패 (size: {})", texts.size(), e);
            throw new RuntimeException("배치 임베딩 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
