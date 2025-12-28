package com.knowlearnmap.prompt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowlearnmap.prompt.dto.*;
import com.knowlearnmap.prompt.domain.Prompt;
import com.knowlearnmap.prompt.domain.PromptVersion;
import com.knowlearnmap.prompt.domain.PromptRepository;
import com.knowlearnmap.prompt.domain.PromptTestSnapshotRepository;
import com.knowlearnmap.prompt.domain.PromptVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRepository promptRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptTestSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    // ============================================
    // 1. 프롬프트 관리
    // ============================================

    /**
     * ID로 프롬프트 코드 조회
     * (Eclipse 인식 문제를 해결하기 위해 상단으로 이동)
     */
    public String getPromptCodeById(Long id) {
        return promptRepository.findById(id)
                .map(Prompt::getCode)
                .orElseThrow(() -> new RuntimeException("프롬프트를 찾을 수 없습니다: " + id));
    }

    /**
     * 프롬프트 목록 조회
     */
    public Page<PromptResponse> getPrompts(Boolean isActive, String search, Pageable pageable) {
        Page<Prompt> page = promptRepository.findByFilters(isActive, search, pageable);
        return page.map(this::convertToPromptResponse);
    }

    /**
     * 프롬프트 상세 조회
     */
    public PromptDetailResponse getPromptByCode(String code) {
        Prompt prompt = promptRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("프롬프트를 찾을 수 없습니다: " + code));

        return convertToPromptDetailResponse(prompt);
    }

    /**
     * 프롬프트 생성
     */
    @Transactional
    public PromptDetailResponse createPrompt(CreatePromptRequest request) {
        // 코드 중복 체크
        if (promptRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("이미 존재하는 프롬프트 코드입니다: " + request.getCode());
        }

        // 프롬프트 생성
        Prompt prompt = Prompt.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .tags(convertTagsToJson(request.getTags()))
                .isActive(true)
                .build();

        prompt = promptRepository.save(prompt);

        // 초기 버전 생성
        if (request.getPromptContent() != null && !request.getPromptContent().isEmpty()) {
            PromptVersion version = PromptVersion.builder()
                    .promptCode(request.getCode())
                    .version(1)
                    .content(request.getPromptContent())
                    .variableSchema(convertVariableSchemaToJson(request.getVariables()))
                    .status("published")
                    .isActive(true)
                    .notes("초기 버전")
                    .testCount(0)
                    .build();

            version = versionRepository.save(version);

            // publishVersionId 업데이트
            prompt.setPublishVersionId(version.getId());
            prompt = promptRepository.save(prompt);
        }

        return convertToPromptDetailResponse(prompt);
    }

    /**
     * 프롬프트 수정
     */
    @Transactional
    public PromptDetailResponse updatePrompt(String code, UpdatePromptRequest request) {
        Prompt prompt = promptRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("프롬프트를 찾을 수 없습니다: " + code));

        if (request.getName() != null) {
            prompt.setName(request.getName());
        }
        if (request.getDescription() != null) {
            prompt.setDescription(request.getDescription());
        }
        if (request.getTags() != null) {
            prompt.setTags(convertTagsToJson(request.getTags()));
        }

        prompt = promptRepository.save(prompt);
        return convertToPromptDetailResponse(prompt);
    }

    /**
     * 프롬프트 삭제
     */
    @Transactional
    public void deletePrompt(String code) {
        Prompt prompt = promptRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("프롬프트를 찾을 수 없습니다: " + code));

        promptRepository.delete(prompt);
    }

    /**
     * 프롬프트 코드 중복 체크
     */
    public CodeAvailabilityResponse checkCodeAvailability(String code) {
        boolean available = !promptRepository.existsByCode(code);
        return CodeAvailabilityResponse.builder()
                .available(available)
                .build();
    }

    /**
     * 프롬프트 통계
     */
    public StatisticsResponse getStatistics(String code) {
        // 프롬프트 존재 확인
        promptRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("프롬프트를 찾을 수 없습니다: " + code));

        Integer totalVersions = versionRepository.countByPromptCode(code);
        Integer publishedVersions = versionRepository.countByPromptCodeAndStatus(code, "published");
        Integer draftVersions = versionRepository.countByPromptCodeAndStatus(code, "draft");
        Integer totalTests = snapshotRepository.countByPromptCode(code);
        BigDecimal avgSatisfaction = snapshotRepository.calculateAvgSatisfactionByPromptCode(code);
        LocalDateTime lastTestDate = snapshotRepository.findLastTestDateByPromptCode(code);

        return StatisticsResponse.builder()
                .totalVersions(totalVersions != null ? totalVersions : 0)
                .publishedVersions(publishedVersions != null ? publishedVersions : 0)
                .draftVersions(draftVersions != null ? draftVersions : 0)
                .totalTests(totalTests != null ? totalTests : 0)
                .avgSatisfaction(avgSatisfaction)
                .lastTestDate(lastTestDate)
                .build();
    }

    /**
     * 버전별 만족도 추이
     */
    public List<SatisfactionTrendResponse> getSatisfactionTrend(String code) {
        // 프롬프트 존재 확인
        promptRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("프롬프트를 찾을 수 없습니다: " + code));

        return versionRepository.findSatisfactionTrendByPromptCode(code);
    }

    /**
     * 배포된 프롬프트 content 조회
     */
    public PublishedPromptResponse getPublishedContent(String code) {
        // 프롬프트 조회
        Prompt prompt = promptRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("프롬프트를 찾을 수 없습니다: " + code));

        // publishVersionId 확인
        if (prompt.getPublishVersionId() == null) {
            throw new RuntimeException("배포된 버전이 없습니다: " + code);
        }

        // 배포된 버전 조회
        PromptVersion version = versionRepository.findById(prompt.getPublishVersionId())
                .orElseThrow(() -> new RuntimeException("배포된 버전을 찾을 수 없습니다: " + prompt.getPublishVersionId()));

        // LlmConfig 변환
        LlmConfigDto llmConfig = null;
        if (version.getLlmConfig() != null && !version.getLlmConfig().isEmpty()) {
            try {
                llmConfig = objectMapper.readValue(version.getLlmConfig(), LlmConfigDto.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert llmConfig from JSON", e);
            }
        }

        return PublishedPromptResponse.builder()
                .content(version.getContent())
                .llmConfig(llmConfig)
                .build();
    }

    // ============================================
    // Helper Methods
    // ============================================

    private PromptResponse convertToPromptResponse(Prompt entity) {
        Integer activeVersion = null;
        if (entity.getPublishVersionId() != null) {
            PromptVersion version = versionRepository.findById(entity.getPublishVersionId()).orElse(null);
            if (version != null) {
                activeVersion = version.getVersion();
            }
        }

        // 버전 개수 조회
        Integer versionCount = versionRepository.countByPromptCode(entity.getCode());

        return PromptResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .tags(convertJsonToTags(entity.getTags()))
                .isActive(entity.getIsActive())
                .activeVersion(activeVersion)
                .versionCount(versionCount != null ? versionCount : 0)
                .createdAt(entity.getCreatedDatetime())
                .updatedAt(entity.getUpdatedDatetime())
                .build();
    }

    private PromptDetailResponse convertToPromptDetailResponse(Prompt entity) {
        Integer activeVersion = null;
        if (entity.getPublishVersionId() != null) {
            PromptVersion version = versionRepository.findById(entity.getPublishVersionId()).orElse(null);
            if (version != null) {
                activeVersion = version.getVersion();
            }
        }

        return PromptDetailResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .tags(convertJsonToTags(entity.getTags()))
                .isActive(entity.getIsActive())
                .publishVersionId(entity.getPublishVersionId())
                .activeVersion(activeVersion)
                .createdBy(entity.getCreatedId())
                .createdAt(entity.getCreatedDatetime())
                .updatedBy(entity.getUpdatedId())
                .updatedAt(entity.getUpdatedDatetime())
                .build();
    }

    private String convertTagsToJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert tags to JSON", e);
            return "[]";
        }
    }

    private List<String> convertJsonToTags(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to tags", e);
            return new ArrayList<>();
        }
    }

    private String convertVariableSchemaToJson(List<VariableSchemaDto> schemas) {
        if (schemas == null || schemas.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(schemas);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert variable schema to JSON", e);
            return "[]";
        }
    }
}
