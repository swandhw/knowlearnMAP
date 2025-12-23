package com.knowlearnmap.prompt.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.knowlearnmap.prompt.dto.*;
import com.knowlearnmap.prompt.service.PromptService;
import com.knowlearnmap.prompt.service.PromptTestService;
import com.knowlearnmap.prompt.service.PromptVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/prompts")
@RequiredArgsConstructor
@Tag(name = "PROMPTS", description = "Prompt 관리")
public class PromptController {

    private final PromptService promptService;
    private final PromptVersionService versionService;
    private final PromptTestService testService;
    // ============================================
    // 1. 프롬프트 관리 API
    // ============================================

    @GetMapping
    @Operation(summary = "프롬프트 목록 조회", description = "프롬프트 목록을 페이지네이션하여 조회합니다")
    public ResponseEntity<ApiResponse<Page<PromptResponse>>> getPrompts(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "활성 상태 필터") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "검색어") @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size);
        Page<PromptResponse> prompts = promptService.getPrompts(isActive, search, pageable);

        return ResponseEntity.ok(ApiResponse.success("프롬프트 목록 조회 성공", prompts));
    }

    @GetMapping("/{code}")
    @Operation(summary = "프롬프트 상세 조회", description = "프롬프트 코드로 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<PromptDetailResponse>> getPrompt(
            @Parameter(description = "프롬프트 코드") @PathVariable String code) {

        PromptDetailResponse prompt = promptService.getPromptByCode(code);
        return ResponseEntity.ok(ApiResponse.success("프롬프트 상세 조회 성공", prompt));
    }

    @GetMapping("/{code}/content")
    @Operation(summary = "배포된 프롬프트 content 조회", description = "배포된 프롬프트의 실제 내용과 LLM 설정을 조회합니다")
    public ResponseEntity<ApiResponse<PublishedPromptResponse>> getPublishedContent(
            @Parameter(description = "프롬프트 코드") @PathVariable String code) {

        PublishedPromptResponse response = promptService.getPublishedContent(code);
        return ResponseEntity.ok(ApiResponse.success("배포된 프롬프트 조회 성공", response));
    }

    @PostMapping
    @Operation(summary = "프롬프트 생성", description = "새로운 프롬프트를 생성합니다")
    public ResponseEntity<ApiResponse<PromptDetailResponse>> createPrompt(
            @RequestBody CreatePromptRequest request) {

        PromptDetailResponse prompt = promptService.createPrompt(request);
        return ResponseEntity.ok(ApiResponse.success("프롬프트 생성 성공", prompt));
    }

    @PutMapping("/{code}")
    @Operation(summary = "프롬프트 수정", description = "프롬프트 정보를 수정합니다")
    public ResponseEntity<ApiResponse<PromptDetailResponse>> updatePrompt(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @RequestBody UpdatePromptRequest request) {

        PromptDetailResponse prompt = promptService.updatePrompt(code, request);
        return ResponseEntity.ok(ApiResponse.success("프롬프트 수정 성공", prompt));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "프롬프트 삭제", description = "프롬프트를 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> deletePrompt(
            @Parameter(description = "프롬프트 코드") @PathVariable String code) {

        promptService.deletePrompt(code);
        return ResponseEntity.ok(ApiResponse.success("프롬프트 삭제 성공", null));
    }

    @GetMapping("/check-code/{code}")
    @Operation(summary = "프롬프트 코드 중복 체크", description = "프롬프트 코드의 사용 가능 여부를 확인합니다")
    public ResponseEntity<ApiResponse<CodeAvailabilityResponse>> checkCode(
            @Parameter(description = "프롬프트 코드") @PathVariable String code) {

        CodeAvailabilityResponse result = promptService.checkCodeAvailability(code);
        return ResponseEntity.ok(ApiResponse.success("코드 사용 가능", result));
    }

    @GetMapping("/{code}/versions")
    @Operation(summary = "버전 목록 조회", description = "프롬프트의 버전 목록을 조회합니다")
    public ResponseEntity<ApiResponse<Page<VersionResponse>>> getVersions(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "상태 필터") @RequestParam(required = false) String status,
            @Parameter(description = "페이지 정보") Pageable pageable) {

        String resolvedCode = resolvePromptCode(code);
        Page<VersionResponse> versions = versionService.getVersions(resolvedCode, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("버전 목록 조회 성공", versions));
    }

    @GetMapping("/{code}/versions/{versionId}")
    @Operation(summary = "버전 상세 조회", description = "버전 ID로 상세 정보를 조회합니다")
    public ResponseEntity<ApiResponse<VersionResponse>> getVersion(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @PathVariable Long versionId) {

        VersionResponse version = versionService.getVersionById(code, versionId);
        return ResponseEntity.ok(ApiResponse.success("버전 상세 조회 성공", version));
    }

    @PostMapping("/{code}/versions")
    @Operation(summary = "버전 생성", description = "새로운 버전을 생성합니다 (Draft)")
    public ResponseEntity<ApiResponse<VersionResponse>> createVersion(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @RequestBody CreateVersionRequest request) {

        VersionResponse version = versionService.createVersion(code, request);
        return ResponseEntity.ok(ApiResponse.success("버전 저장 성공", version));
    }

    @PutMapping("/{code}/versions/{versionId}")
    @Operation(summary = "버전 수정", description = "버전 정보를 수정합니다")
    public ResponseEntity<ApiResponse<VersionResponse>> updateVersion(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @PathVariable Long versionId,
            @RequestBody UpdateVersionRequest request) {

        VersionResponse version = versionService.updateVersion(code, versionId, request);
        return ResponseEntity.ok(ApiResponse.success("버전 수정 성공", version));
    }

    @PostMapping("/{code}/versions/{versionId}/publish")
    @Operation(summary = "버전 배포", description = "버전을 배포합니다 (Publish)")
    public ResponseEntity<ApiResponse<VersionResponse>> publishVersion(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @PathVariable Long versionId,
            @RequestBody PublishVersionRequest request) {

        VersionResponse version = versionService.publishVersion(code, versionId, request);
        return ResponseEntity.ok(ApiResponse.success("버전 배포 성공", version));
    }

    @DeleteMapping("/{code}/versions/{versionId}")
    @Operation(summary = "버전 삭제", description = "버전을 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> deleteVersion(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @PathVariable Long versionId) {

        versionService.deleteVersion(code, versionId);
        return ResponseEntity.ok(ApiResponse.success("버전 삭제 성공", null));
    }

    @PostMapping("/{code}/versions/{versionId}/copy")
    @Operation(summary = "버전 복사", description = "버전을 복사합니다")
    public ResponseEntity<ApiResponse<VersionResponse>> copyVersion(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @PathVariable Long versionId,
            @RequestBody CopyVersionRequest request) {

        VersionResponse version = versionService.copyVersion(code, versionId, request);
        return ResponseEntity.ok(ApiResponse.success("버전 복사 성공", version));
    }

    @PutMapping("/{code}/versions/{versionId}/rating")
    @Operation(summary = "버전 종합 평가 수정", description = "버전의 종합 평가를 수정합니다")
    public ResponseEntity<ApiResponse<VersionResponse>> updateRating(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @PathVariable Long versionId,
            @RequestBody UpdateRatingRequest request) {

        VersionResponse version = versionService.updateRating(code, versionId, request);
        return ResponseEntity.ok(ApiResponse.success("종합 평가 저장 성공", version));
    }

    // ============================================
    // 3. 변수 스키마 관리 API
    // ============================================

    @GetMapping("/{code}/variable-schemas")
    @Operation(summary = "변수 스키마 조회", description = "프롬프트의 변수 스키마를 조회합니다")
    public ResponseEntity<ApiResponse<List<VariableSchemaDto>>> getVariableSchemas(
            @Parameter(description = "프롬프트 코드") @PathVariable String code) {

        List<VariableSchemaDto> schemas = versionService.getVariableSchemas(code);
        return ResponseEntity.ok(ApiResponse.success("변수 스키마 조회 성공", schemas));
    }

    @PutMapping("/{code}/variable-schemas")
    @Operation(summary = "변수 스키마 저장/수정", description = "변수 스키마를 저장하거나 수정합니다")
    public ResponseEntity<ApiResponse<List<VariableSchemaDto>>> updateVariableSchemas(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @RequestBody UpdateVariableSchemasRequest request) {

        List<VariableSchemaDto> schemas = versionService.updateVariableSchemas(code, request);
        return ResponseEntity.ok(ApiResponse.success("변수 스키마 저장 성공", schemas));
    }

    // ============================================
    // 4. 테스트 API
    // ============================================

    @PostMapping("/call-llm")
    @Operation(summary = "LLM 직접 호출", description = "프롬프트 관리와 무관하게 LLM API를 직접 호출합니다")
    public ResponseEntity<ApiResponse<LlmDirectCallResponse>> callLlmDirect(
            @RequestBody LlmDirectCallRequest request) {

        LlmDirectCallResponse result = testService.callLlmDirect(request);
        return ResponseEntity.ok(ApiResponse.success("LLM 호출 완료", result));
    }

    @PostMapping("/{code}/call")
    @Operation(summary = "배포된 프롬프트로 LLM 간단 호출", description = "프롬프트 코드와 사용자 입력만으로 배포된 프롬프트의 LLM 설정을 사용하여 호출합니다")
    public ResponseEntity<ApiResponse<LlmDirectCallResponse>> callLlmWithPublishedPrompt(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @RequestBody SimpleLlmCallRequest request) {

        LlmDirectCallResponse result = testService.callLlmWithPublishedPrompt(code, request);
        return ResponseEntity.ok(ApiResponse.success("배포된 프롬프트로 LLM 호출 완료", result));
    }

    @PostMapping("/{code}/test")
    @Operation(summary = "테스트 실행", description = "프롬프트 테스트를 실행합니다")
    public ResponseEntity<ApiResponse<TestExecutionResponse>> executeTest(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @RequestBody ExecuteTestRequest request) {

        TestExecutionResponse result = testService.executeTest(code, request);
        return ResponseEntity.ok(ApiResponse.success("테스트 실행 성공", result));
    }

    @PostMapping("/{code}/versions/{versionId}/llm-config")
    @Operation(summary = "환경 설정 저장", description = "버전의 환경 설정(LLM 설정)을 저장합니다 (Upsert)")
    public ResponseEntity<ApiResponse<TestSetResponse>> saveLlmConfig(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @PathVariable Long versionId,
            @RequestBody CreateTestSetRequest request) {

        // URL의 versionId와 Body의 versionId 일치 여부 확인 (선택사항)
        request.setVersionId(versionId);
        TestSetResponse result = testService.saveLlmConfig(code, request);
        return ResponseEntity.ok(ApiResponse.success("환경 설정 저장 성공", result));
    }

    // executeTestSet 제거 (executeTest로 통합)

    @GetMapping("/{code}/versions/{versionId}/llm-config")
    @Operation(summary = "환경 설정 조회", description = "버전의 환경 설정(LLM 설정)을 조회합니다")
    public ResponseEntity<ApiResponse<TestSetResponse>> getLlmConfig(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @PathVariable Long versionId) {

        TestSetResponse testSet = testService.getLlmConfig(versionId);
        return ResponseEntity.ok(ApiResponse.success("환경 설정 조회 성공", testSet));
    }

    // deleteTestSet 제거 (필요 시 버전 ID로 삭제 구현)

    @GetMapping("/{code}/versions/{versionId}/snapshots")
    @Operation(summary = "테스트 스냅샷 목록 조회", description = "버전의 테스트 스냅샷 목록을 조회합니다")
    public ResponseEntity<ApiResponse<Page<TestSnapshotResponse>>> getSnapshots(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @PathVariable Long versionId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "최소 만족도 필터") @RequestParam(required = false) Integer minSatisfaction) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TestSnapshotResponse> snapshots = testService.getSnapshots(code, versionId, minSatisfaction, pageable);

        return ResponseEntity.ok(ApiResponse.success("테스트 스냅샷 목록 조회 성공", snapshots));
    }

    @GetMapping("/{code}/all-snapshots")
    @Operation(summary = "프롬프트의 모든 테스트 스냅샷 조회", description = "프롬프트의 모든 테스트 스냅샷 목록을 조회합니다")
    public ResponseEntity<ApiResponse<Page<TestSnapshotResponse>>> getAllSnapshots(
            @Parameter(description = "프롬프트 코드") @PathVariable String code,
            @Parameter(description = "버전 ID") @RequestParam(required = false) Long versionId,
            @Parameter(description = "모델") @RequestParam(required = false) String model,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TestSnapshotResponse> snapshots = testService.getAllSnapshots(code, versionId, model, pageable);

        return ResponseEntity.ok(ApiResponse.success("테스트 스냅샷 목록 조회 성공", snapshots));
    }

    @PutMapping("/snapshots/{snapshotId}/satisfaction")
    @Operation(summary = "테스트 만족도 저장", description = "테스트 스냅샷의 만족도를 저장합니다")
    public ResponseEntity<ApiResponse<TestSnapshotResponse>> updateSatisfaction(
            @Parameter(description = "스냅샷 ID") @PathVariable Long snapshotId,
            @RequestBody UpdateSatisfactionRequest request) {

        TestSnapshotResponse snapshot = testService.updateSatisfaction(snapshotId, request);
        return ResponseEntity.ok(ApiResponse.success("만족도 저장 성공", snapshot));
    }

    @DeleteMapping("/snapshots/{snapshotId}")
    @Operation(summary = "테스트 스냅샷 삭제", description = "테스트 스냅샷을 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> deleteSnapshot(
            @Parameter(description = "스냅샷 ID") @PathVariable Long snapshotId) {

        testService.deleteSnapshot(snapshotId);
        return ResponseEntity.ok(ApiResponse.success("테스트 스냅샷 삭제 성공", null));
    }

    // ============================================
    // 5. 통계 및 대시보드 API
    // ============================================

    @GetMapping("/{code}/statistics")
    @Operation(summary = "프롬프트 통계", description = "프롬프트의 통계 정보를 조회합니다")
    public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics(
            @Parameter(description = "프롬프트 코드") @PathVariable String code) {

        StatisticsResponse statistics = promptService.getStatistics(code);
        return ResponseEntity.ok(ApiResponse.success("통계 조회 성공", statistics));
    }

    @GetMapping("/{code}/satisfaction-trend")
    @Operation(summary = "버전별 만족도 추이", description = "버전별 만족도 추이를 조회합니다")
    public ResponseEntity<ApiResponse<List<SatisfactionTrendResponse>>> getSatisfactionTrend(
            @Parameter(description = "프롬프트 코드") @PathVariable String code) {

        List<SatisfactionTrendResponse> trend = promptService.getSatisfactionTrend(code);
        return ResponseEntity.ok(ApiResponse.success("만족도 추이 조회 성공", trend));
    }
    // ============================================
    // Helper Methods
    // ============================================

    private String resolvePromptCode(String codeOrId) {
        // 1. 코드로 먼저 조회 (우선순위)
        // checkCodeAvailability는 중복 체크용이라 available=false면 이미 존재하는 코드임
        if (!promptService.checkCodeAvailability(codeOrId).getAvailable()) {
            return codeOrId;
        }

        // 2. 숫자로만 구성된 경우 ID로 간주
        if (codeOrId.matches("\\d+")) {
            try {
                Long id = Long.parseLong(codeOrId);
                // PromptService의 getPromptCodeById 메서드 호출
                return promptService.getPromptCodeById(id);
            } catch (Exception e) {
                // 숫자가 너무 크거나 형식이 잘못된 경우, 또는 ID가 없는 경우 원래 문자열 반환
                return codeOrId;
            }
        }
        return codeOrId;
    }
}
