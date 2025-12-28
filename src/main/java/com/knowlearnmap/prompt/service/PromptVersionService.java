package com.knowlearnmap.prompt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowlearnmap.prompt.dto.*;
import com.knowlearnmap.prompt.domain.Prompt;
import com.knowlearnmap.prompt.domain.PromptVersion;
import com.knowlearnmap.prompt.domain.PromptRepository;
import com.knowlearnmap.prompt.domain.PromptVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptVersionService {

    private final PromptRepository promptRepository;
    private final PromptVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 踰꾩쟾 紐⑸줉 議고쉶
     */
    public Page<VersionResponse> getVersions(String promptCode, String status, Pageable pageable) {
        // ?꾨＼?꾪듃 議댁옱 ?뺤씤
        promptRepository.findByCode(promptCode)
                .orElseThrow(() -> new RuntimeException("?꾨＼?꾪듃瑜?李얠쓣 ???놁뒿?덈떎: " + promptCode));

        Page<PromptVersion> page = versionRepository.findByPromptCodeAndStatus(promptCode, status, pageable);
        return page.map(this::convertToVersionResponse);
    }

    /**
     * 踰꾩쟾 ?곸꽭 議고쉶
     */
    public VersionResponse getVersionById(String promptCode, Long versionId) {
        PromptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("踰꾩쟾??李얠쓣 ???놁뒿?덈떎: " + versionId));

        if (!version.getPromptCode().equals(promptCode)) {
            throw new RuntimeException("?대떦 ?꾨＼?꾪듃??踰꾩쟾???꾨떃?덈떎");
        }

        return convertToVersionResponse(version);
    }

    /**
     * 踰꾩쟾 ?앹꽦 (Draft)
     */
    @Transactional
    public VersionResponse createVersion(String promptCode, CreateVersionRequest request) {
        // ?꾨＼?꾪듃 議댁옱 ?뺤씤
        promptRepository.findByCode(promptCode)
                .orElseThrow(() -> new RuntimeException("?꾨＼?꾪듃瑜?李얠쓣 ???놁뒿?덈떎: " + promptCode));

        // ?ㅼ쓬 踰꾩쟾 踰덊샇 議고쉶
        Integer maxVersion = versionRepository.findMaxVersionByPromptCode(promptCode);
        Integer nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

        // 踰꾩쟾 ?앹꽦
        PromptVersion version = PromptVersion.builder()
                .promptCode(promptCode)
                .version(nextVersion)
                .content(request.getContent())
                .variableSchema(convertVariableSchemaToJson(request.getVariableSchema()))
                .status(request.getStatus() != null ? request.getStatus() : "draft")
                .isActive(false)
                .notes(request.getNotes())
                .testCount(0)
                .build();

        version = versionRepository.save(version);
        return convertToVersionResponse(version);
    }

    /**
     * 踰꾩쟾 ?섏젙
     */
    @Transactional
    public VersionResponse updateVersion(String promptCode, Long versionId, UpdateVersionRequest request) {
        PromptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("踰꾩쟾??李얠쓣 ???놁뒿?덈떎: " + versionId));

        if (!version.getPromptCode().equals(promptCode)) {
            throw new RuntimeException("?대떦 ?꾨＼?꾪듃??踰꾩쟾???꾨떃?덈떎");
        }

        // ?쒖꽦?붾맂 踰꾩쟾? ?섏젙 遺덇? (?꾩슂??二쇱꽍 ?댁젣)
        // if (version.getIsActive()) {
        //     throw new RuntimeException("?쒖꽦?붾맂 踰꾩쟾? ?섏젙?????놁뒿?덈떎");
        // }

        // ?꾨뱶 ?섏젙
        if (request.getContent() != null) {
            version.setContent(request.getContent());
        }
        if (request.getVariableSchema() != null) {
            version.setVariableSchema(convertVariableSchemaToJson(request.getVariableSchema()));
        }
        if (request.getNotes() != null) {
            version.setNotes(request.getNotes());
        }
        if (request.getStatus() != null) {
            version.setStatus(request.getStatus());
        }

        version = versionRepository.save(version);
        return convertToVersionResponse(version);
    }

    /**
     * 踰꾩쟾 諛고룷 (Publish)
     */
    @Transactional
    public VersionResponse publishVersion(String promptCode, Long versionId, PublishVersionRequest request) {
        Prompt prompt = promptRepository.findByCode(promptCode)
                .orElseThrow(() -> new RuntimeException("?꾨＼?꾪듃瑜?李얠쓣 ???놁뒿?덈떎: " + promptCode));

        PromptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("踰꾩쟾??李얠쓣 ???놁뒿?덈떎: " + versionId));

        if (!version.getPromptCode().equals(promptCode)) {
            throw new RuntimeException("?대떦 ?꾨＼?꾪듃??踰꾩쟾???꾨떃?덈떎");
        }

        // 湲곗〈 ?쒖꽦 踰꾩쟾 鍮꾪솢?깊솕
        PromptVersion activeVersion = versionRepository.findByPromptCodeAndIsActive(promptCode, true).orElse(null);
        if (activeVersion != null) {
            activeVersion.setIsActive(false);
            activeVersion.setStatus("deprecated");
            versionRepository.save(activeVersion);
        }

        // ??踰꾩쟾 ?쒖꽦??
        version.setStatus("published");
        version.setIsActive(true);
        if (request.getNotes() != null) {
            version.setNotes(request.getNotes());
        }
        version = versionRepository.save(version);

        // ?꾨＼?꾪듃??publishVersionId ?낅뜲?댄듃
        prompt.setPublishVersionId(version.getId());
        promptRepository.save(prompt);

        return convertToVersionResponse(version);
    }

    /**
     * 踰꾩쟾 ??젣
     */
    @Transactional
    public void deleteVersion(String promptCode, Long versionId) {
        PromptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("踰꾩쟾??李얠쓣 ???놁뒿?덈떎: " + versionId));

        if (!version.getPromptCode().equals(promptCode)) {
            throw new RuntimeException("?대떦 ?꾨＼?꾪듃??踰꾩쟾???꾨떃?덈떎");
        }

        // ?꾩껜 踰꾩쟾 媛쒖닔 ?뺤씤
        Integer totalVersionCount = versionRepository.countByPromptCode(promptCode);

        // ?쒖꽦?붾맂 踰꾩쟾??寃쎌슦
        if (version.getIsActive()) {
            if (totalVersionCount != null && totalVersionCount > 1) {
                // 踰꾩쟾??2媛??댁긽?대㈃ ??젣 遺덇?
                throw new RuntimeException("?쒖꽦?붾맂 踰꾩쟾???덈뒗 ?꾨＼?꾪듃???ㅻⅨ 踰꾩쟾??議댁옱?섎㈃ ??젣?????놁뒿?덈떎. 癒쇱? ?ㅻⅨ 踰꾩쟾???쒖꽦?뷀븳 ????젣?댁＜?몄슂.");
            }
        }

        // 踰꾩쟾 ??젣
        versionRepository.delete(version);

        // 踰꾩쟾??1媛쒕쭔 ?덉뿀?ㅻ㈃ ?꾨＼?꾪듃????젣
        if (totalVersionCount != null && totalVersionCount == 1) {
            Prompt prompt = promptRepository.findByCode(promptCode)
                    .orElseThrow(() -> new RuntimeException("?꾨＼?꾪듃瑜?李얠쓣 ???놁뒿?덈떎: " + promptCode));
            promptRepository.delete(prompt);
        }
    }

    /**
     * 踰꾩쟾 蹂듭궗
     */
    @Transactional
    public VersionResponse copyVersion(String promptCode, Long versionId, CopyVersionRequest request) {
        // ?먮낯 踰꾩쟾 議고쉶
        PromptVersion sourceVersion = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("踰꾩쟾??李얠쓣 ???놁뒿?덈떎: " + versionId));

        if (!sourceVersion.getPromptCode().equals(promptCode)) {
            throw new RuntimeException("?대떦 ?꾨＼?꾪듃??踰꾩쟾???꾨떃?덈떎");
        }

        // ????꾨＼?꾪듃 議고쉶
        String targetPromptCode = request.getTargetPromptCode() != null ? request.getTargetPromptCode() : promptCode;
        promptRepository.findByCode(targetPromptCode)
                .orElseThrow(() -> new RuntimeException("????꾨＼?꾪듃瑜?李얠쓣 ???놁뒿?덈떎: " + targetPromptCode));

        // ?ㅼ쓬 踰꾩쟾 踰덊샇 議고쉶
        Integer maxVersion = versionRepository.findMaxVersionByPromptCode(targetPromptCode);
        Integer nextVersion = (maxVersion != null ? maxVersion : 0) + 1;

        // ??踰꾩쟾 ?앹꽦
        PromptVersion newVersion = PromptVersion.builder()
                .promptCode(targetPromptCode)
                .version(nextVersion)
                .content(sourceVersion.getContent())
                .variableSchema(sourceVersion.getVariableSchema())
                .status("draft")
                .isActive(false)
                .notes(request.getNotes() != null ? request.getNotes() : "蹂듭궗??踰꾩쟾")
                .testCount(0)
                .build();

        newVersion = versionRepository.save(newVersion);
        return convertToVersionResponse(newVersion);
    }

    /**
     * 踰꾩쟾 醫낇빀 ?됯? ?섏젙
     */
    @Transactional
    public VersionResponse updateRating(String promptCode, Long versionId, UpdateRatingRequest request) {
        PromptVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("踰꾩쟾??李얠쓣 ???놁뒿?덈떎: " + versionId));

        if (!version.getPromptCode().equals(promptCode)) {
            throw new RuntimeException("?대떦 ?꾨＼?꾪듃??踰꾩쟾???꾨떃?덈떎");
        }

        version.setOverallRating(request.getOverallRating());
        version.setOverallNotes(request.getOverallNotes());

        version = versionRepository.save(version);
        return convertToVersionResponse(version);
    }

    /**
     * 蹂???ㅽ궎留?議고쉶
     */
    public List<VariableSchemaDto> getVariableSchemas(String promptCode) {
        // ?쒖꽦 踰꾩쟾??蹂???ㅽ궎留?議고쉶
        PromptVersion activeVersion = versionRepository.findByPromptCodeAndIsActive(promptCode, true)
                .orElseThrow(() -> new RuntimeException("?쒖꽦?붾맂 踰꾩쟾???놁뒿?덈떎"));

        return convertJsonToVariableSchema(activeVersion.getVariableSchema());
    }

    /**
     * 蹂???ㅽ궎留?????섏젙
     */
    @Transactional
    public List<VariableSchemaDto> updateVariableSchemas(String promptCode, UpdateVariableSchemasRequest request) {
        // ?쒖꽦 踰꾩쟾 議고쉶
        PromptVersion activeVersion = versionRepository.findByPromptCodeAndIsActive(promptCode, true)
                .orElseThrow(() -> new RuntimeException("?쒖꽦?붾맂 踰꾩쟾???놁뒿?덈떎"));

        activeVersion.setVariableSchema(convertVariableSchemaToJson(request.getSchemas()));
        activeVersion = versionRepository.save(activeVersion);

        return convertJsonToVariableSchema(activeVersion.getVariableSchema());
    }

    // ============================================
    // Helper Methods
    // ============================================

    private VersionResponse convertToVersionResponse(PromptVersion entity) {
        return VersionResponse.builder()
                .id(entity.getId())
                .promptCode(entity.getPromptCode())
                .version(entity.getVersion())
                .content(entity.getContent())
                .variableSchema(convertJsonToVariableSchema(entity.getVariableSchema()))
                .llmConfig(convertJsonToLlmConfig(entity.getLlmConfig()))
                .status(entity.getStatus())
                .isActive(entity.getIsActive())
                .notes(entity.getNotes())
                .avgSatisfaction(entity.getAvgSatisfaction())
                .testCount(entity.getTestCount())
                .lastTestedAt(entity.getLastTestedAt())
                .overallRating(entity.getOverallRating())
                .overallNotes(entity.getOverallNotes())
                .createdBy(entity.getCreatedId())
                .createdAt(entity.getCreatedDatetime())
                .build();
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

    private List<VariableSchemaDto> convertJsonToVariableSchema(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, VariableSchemaDto.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to variable schema", e);
            return new ArrayList<>();
        }
    }

    private LlmConfigDto convertJsonToLlmConfig(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LlmConfigDto.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to LLM config", e);
            return null;
        }
    }
}
