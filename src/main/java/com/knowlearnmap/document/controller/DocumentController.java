package com.knowlearnmap.document.controller;

import com.knowlearnmap.common.dto.ApiResponse;
import com.knowlearnmap.document.dto.DocumentPageDto;
import com.knowlearnmap.document.dto.DocumentResponseDto;
import com.knowlearnmap.document.dto.DocumentUpdateRequest;
import com.knowlearnmap.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Document API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 파일 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("workspaceId") Long workspaceId,
            org.springframework.security.core.Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "anonymous";
        if (authentication == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        log.debug("POST /api/documents/upload - filename={}, workspaceId={}, username={}",
                file.getOriginalFilename(), workspaceId, username);

        try {
            DocumentResponseDto response = documentService.uploadFile(file, workspaceId, username);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.error("파일 업로드 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("파일 업로드 중 서버 오류", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("파일 업로드 중 오류가 발생했습니다"));
        }
    }

    /**
     * Document 조회
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> getDocument(
            @PathVariable Long documentId) {

        log.debug("GET /api/documents/{}", documentId);

        try {
            DocumentResponseDto response = documentService.getDocument(documentId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.error("Document 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 워크스페이스의 Document 목록 조회
     */
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> getDocumentsByWorkspace(
            @PathVariable Long workspaceId) {

        log.debug("GET /api/documents/workspace/{}", workspaceId);

        try {
            List<DocumentResponseDto> response = documentService.getDocumentsByWorkspace(workspaceId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Document 목록 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Document 목록 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * Document 삭제
     */
    /**
     * Document 삭제
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long documentId,
            org.springframework.security.core.Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "anonymous";
        if (authentication == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        log.debug("DELETE /api/documents/{} - user={}", documentId, username);

        try {
            documentService.deleteDocument(documentId, username);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalArgumentException e) {
            log.error("Document 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Document 정보 수정 (제목)
     */
    /**
     * Document 정보 수정 (제목)
     */
    @PutMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> updateDocument(
            @PathVariable Long documentId,
            @RequestBody DocumentUpdateRequest request,
            org.springframework.security.core.Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "anonymous";
        if (authentication == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        log.debug("PUT /api/documents/{} - filename={}, user={}", documentId, request.getFilename(), username);

        try {
            DocumentResponseDto response = documentService.updateDocument(documentId, request, username);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.error("Document 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Document의 모든 페이지 조회
     */
    @GetMapping("/{documentId}/pages")
    public ResponseEntity<ApiResponse<List<DocumentPageDto>>> getDocumentPages(
            @PathVariable Long documentId) {

        log.debug("GET /api/documents/{}/pages", documentId);

        try {
            List<DocumentPageDto> pages = documentService.getDocumentPages(documentId);
            return ResponseEntity.ok(ApiResponse.success(pages));
        } catch (IllegalArgumentException e) {
            log.error("Document 페이지 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Document 페이지 조회 중 서버 오류", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("페이지 조회 중 오류가 발생했습니다"));
        }
    }
}
