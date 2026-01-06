package com.knowlearnmap.prompt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowlearnmap.prompt.domain.*;
import com.knowlearnmap.prompt.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTestService {

    private final PromptVersionRepository versionRepository;
    private final PromptTestSnapshotRepository snapshotRepository;
    private final PromptLlmConfigRepository llmConfigRepository;
    private final DirectLlmCallService directLlmCallService;
    private final ObjectMapper objectMapper;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    /**
     * 테스트 실행 (단건)
     * 트랜잭션을 분리하여 LLM 호출 시 DB 커넥션을 점유하지 않도록 함
     */
    public TestExecutionResponse executeTest(String promptCode, ExecuteTestRequest request) {
        // 1. DB Read (짧은 트랜잭션 - Repository 기본 적용)
        PromptVersion version = versionRepository.findById(request.getVersionId())
                .orElseThrow(() -> new RuntimeException("버전을 찾을 수 없습니다."));

        String processedContent = resolveVariables(version.getContent(), request.getVariables());

        LlmConfigDto llmConfig = request.getLlmConfig();
        if (llmConfig == null) {
            llmConfig = llmConfigRepository.findByVersionId(version.getId())
                    .map(this::convertToLlmConfigDto)
                    .orElse(LlmConfigDto.builder()
                            .model("AISTUDIO")
                            .temperature(0.7)
                            .topP(0.95)
                            .maxOutputTokens(2000)
                            .topK(40)
                            .n(1)
                            .build());
        }

        // 2. LLM Call (Long duration - No Transaction)
        // 이 구간에서 DB 커넥션을 잡지 않으므로 Connection Leak 경고 방지
        TestResponseDto testResponse = callLlmDirect(processedContent, llmConfig);

        // 3. DB Write (짧은 트랜잭션)
        LlmConfigDto finalLlmConfig = llmConfig;
        return transactionTemplate.execute(status -> {
            PromptTestSnapshot snapshot = PromptTestSnapshot.builder()
                    .code(promptCode)
                    .versionId(version.getId())
                    .content(processedContent)
                    .variables(convertVariablesToJson(request.getVariables()))
                    .llmConfig(convertLlmConfigToJson(finalLlmConfig))
                    .response(convertTestResponseToJson(testResponse))
                    .build();

            llmConfigRepository.findByVersionId(version.getId())
                    .ifPresent(config -> snapshot.setLlmConfigId(config.getId()));

            PromptTestSnapshot savedSnapshot = snapshotRepository.save(snapshot);
            updateVersionStatistics(version.getId());

            return TestExecutionResponse.builder()
                    .snapshotId(savedSnapshot.getId())
                    .response(testResponse)
                    .build();
        });
    }

    @Transactional
    public TestSetResponse saveLlmConfig(String promptCode, CreateTestSetRequest request) {
        PromptVersion version = versionRepository.findById(request.getVersionId())
                .orElseThrow(() -> new RuntimeException("버전을 찾을 수 없습니다."));

        PromptLlmConfig llmConfig = llmConfigRepository.findByVersionId(request.getVersionId())
                .orElse(PromptLlmConfig.builder()
                        .versionId(request.getVersionId())
                        .build());

        llmConfig.setConfigName(request.getTestName());
        llmConfig.setVariables(convertVariablesToJson(request.getVariables()));
        llmConfig.setLlmConfig(convertLlmConfigToJson(request.getLlmConfig()));

        PromptLlmConfig savedConfig = llmConfigRepository.save(llmConfig);
        return convertToTestSetResponse(savedConfig);
    }

    public TestSetResponse getLlmConfig(Long versionId) {
        PromptLlmConfig llmConfig = llmConfigRepository.findByVersionId(versionId)
                .orElseThrow(() -> new RuntimeException("환경 설정이 없습니다."));
        return convertToTestSetResponse(llmConfig);
    }

    @Transactional
    public void deleteLlmConfig(Long llmConfigId) {
        PromptLlmConfig llmConfig = llmConfigRepository.findById(llmConfigId)
                .orElseThrow(() -> new RuntimeException("환경 설정을 찾을 수 없습니다: " + llmConfigId));
        llmConfigRepository.delete(llmConfig);
    }

    public Page<TestSnapshotResponse> getSnapshots(String promptCode, Long versionId, Integer minSatisfaction,
            Pageable pageable) {
        return snapshotRepository.findByVersionIdAndMinSatisfaction(versionId, minSatisfaction, pageable)
                .map(this::convertToTestSnapshotResponse);
    }

    public Page<TestSnapshotResponse> getAllSnapshots(String promptCode, Long versionId, String model,
            Pageable pageable) {
        return snapshotRepository.findAllSnapshots(promptCode, versionId, model, pageable)
                .map(this::convertToTestSnapshotResponse);
    }

    @Transactional
    public TestSnapshotResponse updateSatisfaction(Long snapshotId, UpdateSatisfactionRequest request) {
        PromptTestSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("테스트 결과를 찾을 수 없습니다."));

        snapshot.setSatisfaction(request.getSatisfaction());
        if (request.getNotes() != null) {
            snapshot.setNotes(request.getNotes());
        }
        PromptTestSnapshot savedSnapshot = snapshotRepository.save(snapshot);
        updateVersionStatistics(snapshot.getVersionId());

        return convertToTestSnapshotResponse(savedSnapshot);
    }

    public TestSnapshotResponse getSnapshot(Long snapshotId) {
        PromptTestSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("스냅샷을 찾을 수 없습니다."));
        return convertToTestSnapshotResponse(snapshot);
    }

    @Transactional
    public void deleteSnapshot(Long snapshotId) {
        PromptTestSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("스냅샷을 찾을 수 없습니다."));
        Long versionId = snapshot.getVersionId();
        snapshotRepository.delete(snapshot);
        updateVersionStatistics(versionId);
    }

    public LlmDirectCallResponse callLlmDirect(LlmDirectCallRequest request) {
        LlmConfigDto config = LlmConfigDto.builder()
                .model(request.getLlmModel())
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .maxOutputTokens(request.getMaxOutputTokens())
                .topK(request.getTopK())
                .n(request.getN())
                .build();

        TestResponseDto response = callLlmDirect(request.getContent(), config);

        return LlmDirectCallResponse.builder()
                .text(response.getText())
                .tokensUsed(response.getTokensUsed())
                .latencyMs(response.getLatencyMs())
                .build();
    }

    public LlmDirectCallResponse callLlmWithPublishedPrompt(String code, SimpleLlmCallRequest request) {
        return callLlmWithPublishedPrompt(code, request, null);
    }

    public LlmDirectCallResponse callLlmWithPublishedPrompt(String code, SimpleLlmCallRequest request,
            LlmConfigDto configOverride) {
        PromptVersion version = versionRepository.findByPromptCodeAndIsActive(code, true)
                .orElseThrow(() -> new RuntimeException("배포된 프롬프트 버전을 찾을 수 없습니다: " + code));

        LlmConfigDto config;

        if (version.getLlmConfig() != null && !version.getLlmConfig().isEmpty()) {
            config = convertJsonToLlmConfig(version.getLlmConfig());
            // 버전 생성 시점의 스냅샷 Config를 최우선 사용 (Production Logic)
        } else {
            // 스냅샷이 없는 경우에만 환경 설정(Test Config) 조회
            config = llmConfigRepository.findByVersionId(version.getId())
                    .map(this::convertToLlmConfigDto)
                    .orElse(null);

            if (config == null) {
                config = LlmConfigDto.builder()
                        .model("AISTUDIO")
                        .temperature(0.7)
                        .build();
            }
        }

        // Apply Override
        if (configOverride != null) {
            if (configOverride.getModel() != null)
                config.setModel(configOverride.getModel());
            if (configOverride.getTemperature() != null)
                config.setTemperature(configOverride.getTemperature());
            if (configOverride.getTopP() != null)
                config.setTopP(configOverride.getTopP());
            if (configOverride.getMaxOutputTokens() != null)
                config.setMaxOutputTokens(configOverride.getMaxOutputTokens());
        }

        String processedContent = resolveVariables(version.getContent(), request.getVariables());
        TestResponseDto response = callLlmDirect(processedContent, config);

        return LlmDirectCallResponse.builder()
                .success(true)
                .text(response.getText())
                .tokensUsed(response.getTokensUsed())
                .latencyMs(response.getLatencyMs())
                .build();
    }

    // Private helpers
    private String resolveVariables(String content, Map<String, Object> variables) {
        if (content == null)
            return "";
        String resolved = content;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                resolved = resolved.replaceAll("\\{\\{\\s*" + key + "\\s*\\}\\}",
                        Matcher.quoteReplacement(String.valueOf(value)));
            }
        }
        return resolved;
    }

    private TestResponseDto callLlmDirect(String content, LlmConfigDto config) {
        try {
            long startTime = System.currentTimeMillis();

            String responseText = directLlmCallService.callLlm(
                    config.getModel() != null ? config.getModel() : "AISTUDIO",
                    content,
                    config.getTemperature() != null ? config.getTemperature() : 0.7,
                    config.getTopP() != null ? config.getTopP() : 0.95,
                    config.getMaxOutputTokens() != null ? config.getMaxOutputTokens() : 2000,
                    config.getTopK() != null ? config.getTopK() : 40,
                    config.getN() != null ? config.getN() : 1);

            long latency = System.currentTimeMillis() - startTime;

            return TestResponseDto.builder()
                    .text(responseText)
                    .tokensUsed(0)
                    .latencyMs(latency)
                    .build();
        } catch (Exception e) {
            log.error("LLM Call Failed", e);
            throw new RuntimeException("LLM 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void updateVersionStatistics(Long versionId) {
        PromptVersion version = versionRepository.findById(versionId).orElse(null);
        if (version == null)
            return;

        Integer count = snapshotRepository.countByVersionId(versionId);
        BigDecimal avg = snapshotRepository.calculateAvgSatisfactionByVersionId(versionId);
        LocalDateTime lastTested = snapshotRepository.findLastTestedAtByVersionId(versionId);

        version.setTestCount(count);
        version.setAvgSatisfaction(avg);
        version.setLastTestedAt(lastTested);

        versionRepository.save(version);
    }

    private String convertVariablesToJson(Map<String, Object> variables) {
        try {
            return objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String convertLlmConfigToJson(LlmConfigDto config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String convertTestResponseToJson(TestResponseDto response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> convertJsonToVariables(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private LlmConfigDto convertJsonToLlmConfig(String json) {
        try {
            return objectMapper.readValue(json, LlmConfigDto.class);
        } catch (Exception e) {
            return new LlmConfigDto();
        }
    }

    private LlmConfigDto convertToLlmConfigDto(PromptLlmConfig entity) {
        return convertJsonToLlmConfig(entity.getLlmConfig());
    }

    private TestSnapshotResponse convertToTestSnapshotResponse(PromptTestSnapshot snapshot) {
        TestResponseDto responseDto = null;
        if (snapshot.getResponse() != null) {
            try {
                responseDto = objectMapper.readValue(snapshot.getResponse(), TestResponseDto.class);
            } catch (JsonProcessingException e) {
                // ignore
            }
        }

        return TestSnapshotResponse.builder()
                .id(snapshot.getId())
                .versionId(snapshot.getVersionId())
                .testName(snapshot.getTestName())
                .content(snapshot.getContent())
                .variables(convertJsonToVariables(snapshot.getVariables()))
                .llmConfig(convertJsonToLlmConfig(snapshot.getLlmConfig()))
                .response(responseDto)
                .satisfaction(snapshot.getSatisfaction())
                .notes(snapshot.getNotes())
                .createdAt(snapshot.getCreatedDatetime())
                .build();
    }

    private TestSetResponse convertToTestSetResponse(PromptLlmConfig entity) {
        Integer snapshotCount = snapshotRepository.countByLlmConfigId(entity.getId());

        return TestSetResponse.builder()
                .id(entity.getId())
                .versionId(entity.getVersionId())
                .testName(entity.getConfigName())
                .variables(entity.getVariables())
                .llmConfig(convertJsonToLlmConfig(entity.getLlmConfig()))
                .snapshotCount(snapshotCount)
                .createdDatetime(entity.getCreatedDatetime())
                .build();
    }
}
