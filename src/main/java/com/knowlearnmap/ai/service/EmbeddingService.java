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
}
