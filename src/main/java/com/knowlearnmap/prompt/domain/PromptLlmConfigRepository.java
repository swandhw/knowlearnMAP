package com.knowlearnmap.prompt.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromptLlmConfigRepository extends JpaRepository<PromptLlmConfig, Long> {

    /**
     * 버전 ID로 LLM 설정 조회 (Singleton)
     */
    Optional<PromptLlmConfig> findByVersionId(Long versionId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM PromptLlmConfig c WHERE c.versionId IN (SELECT v.id FROM PromptVersion v WHERE v.promptCode = :promptCode)")
    void deleteByPromptCode(@org.springframework.data.repository.query.Param("promptCode") String promptCode);
}
