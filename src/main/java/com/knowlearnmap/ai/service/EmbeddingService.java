package com.knowlearnmap.ai.service;

import java.util.List;

/**
 * 벡터 임베딩 서비스 인터페이스
 * - Strategy Pattern 적용: OpenAI, Gemini 등 다양한 구현체 교체 가능
 */
public interface EmbeddingService {

    /**
     * 텍스트를 벡터로 변환 (임베딩)
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 벡터 (List<Double>)
     */
    List<Double> embed(String text);

    /**
     * 여러 텍스트를 한 번에 벡터로 변환 (배치 임베딩)
     * API 호출 횟수를 줄여 성능을 크게 향상시킵니다.
     *
     * @param texts 임베딩할 텍스트 리스트
     * @return 각 텍스트에 대한 임베딩 벡터 리스트
     */
    List<List<Double>> embedBatch(List<String> texts);
}
