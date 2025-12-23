package com.knowlearnmap.workspace.controller;

import com.knowlearnmap.common.dto.ApiResponse;
import com.knowlearnmap.workspace.dto.WorkspaceRequestDto;
import com.knowlearnmap.workspace.dto.WorkspaceResponseDto;
import com.knowlearnmap.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Workspace REST API Controller
 * 
 * 워크스페이스 관리 API 엔드포인트
 * - React 프론트엔드와 통신
 * - CRUD 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /**
     * 워크스페이스 목록 조회
     * GET /api/workspaces
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponseDto>>> getAllWorkspaces() {
        log.debug("GET /api/workspaces - 워크스페이스 목록 조회");

        List<WorkspaceResponseDto> workspaces = workspaceService.getAllWorkspaces();

        return ResponseEntity.ok(
                ApiResponse.success("워크스페이스 목록 조회 성공", workspaces));
    }

    /**
     * 워크스페이스 단건 조회
     * GET /api/workspaces/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponseDto>> getWorkspaceById(@PathVariable Long id) {
        log.debug("GET /api/workspaces/{} - 워크스페이스 조회", id);

        WorkspaceResponseDto workspace = workspaceService.getWorkspaceById(id);

        return ResponseEntity.ok(
                ApiResponse.success("워크스페이스 조회 성공", workspace));
    }

    /**
     * 워크스페이스 생성
     * POST /api/workspaces
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponseDto>> createWorkspace(
            @Valid @RequestBody WorkspaceRequestDto requestDto) {
        log.debug("POST /api/workspaces - 워크스페이스 생성: name={}", requestDto.getName());

        WorkspaceResponseDto workspace = workspaceService.createWorkspace(requestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("워크스페이스 생성 성공", workspace));
    }

    /**
     * 워크스페이스 수정
     * PUT /api/workspaces/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponseDto>> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceRequestDto requestDto) {
        log.debug("PUT /api/workspaces/{} - 워크스페이스 수정", id);

        WorkspaceResponseDto workspace = workspaceService.updateWorkspace(id, requestDto);

        return ResponseEntity.ok(
                ApiResponse.success("워크스페이스 수정 성공", workspace));
    }

    /**
     * 워크스페이스 삭제
     * DELETE /api/workspaces/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(@PathVariable Long id) {
        log.debug("DELETE /api/workspaces/{} - 워크스페이스 삭제", id);

        workspaceService.deleteWorkspace(id);

        return ResponseEntity.ok(
                ApiResponse.success("워크스페이스 삭제 성공", null));
    }

    /**
     * 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("잘못된 요청: {}", e.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("서버 오류 발생", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
}
