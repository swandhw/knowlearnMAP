package com.knowlearnmap.document.service;

import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.domain.DocumentEntity;
import com.knowlearnmap.document.dto.DocumentPageDto;
import com.knowlearnmap.document.dto.DocumentResponseDto;
import com.knowlearnmap.document.dto.DocumentUpdateRequest;
import com.knowlearnmap.document.repository.DocumentChunkRepository;
import com.knowlearnmap.document.repository.DocumentPageRepository;
import com.knowlearnmap.document.repository.DocumentRepository;
import com.knowlearnmap.pipeline.orchestration.PipelineOrchestrator;
import com.knowlearnmap.workspace.domain.WorkspaceEntity;
import com.knowlearnmap.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Document 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentPageRepository documentPageRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PipelineOrchestrator pipelineOrchestrator;
    private final com.knowlearnmap.llmToOntology.service.OntologyPersistenceService ontologyPersistenceService;

    @Value("${app.document.upload-directory:./uploads}")
    private String uploadDirectory;

    /**
     * 파일 업로드 및 Document 생성
     */
    @Transactional
    public DocumentResponseDto uploadFile(MultipartFile file, Long workspaceId) {
        log.info("파일 업로드 시작: filename={}, workspaceId={}", file.getOriginalFilename(), workspaceId);

        // 1. Validation
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다");
        }

        // PDF만 허용 (데모)
        if (!filename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("PDF 파일만 업로드 가능합니다");
        }

        // 2. Workspace 조회
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + workspaceId));

        // 3. 저장 경로 생성
        Path uploadDir = Paths.get(uploadDirectory, workspace.getFolderName());
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("업로드 디렉토리 생성: {}", uploadDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉토리 생성 실패", e);
        }

        // 4. 파일 저장
        Path filePath = uploadDir.resolve(filename);
        try {
            file.transferTo(filePath.toFile());
            log.info("파일 저장 완료: {}", filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + e.getMessage(), e);
        }

        // 5. Document 엔티티 생성
        DocumentEntity document = new DocumentEntity();
        document.setFilename(filename);
        document.setFilePath(filePath.toString());
        document.setWorkspace(workspace);
        document.setSourceType(DocumentEntity.SourceType.FILE);
        document.setStatus(DocumentEntity.IngestionStatus.PENDING);
        document.setPipelineStatus(DocumentEntity.PipelineStatus.PENDING);

        DocumentEntity savedDocument = documentRepository.save(document);
        log.info("Document 생성 완료: id={}", savedDocument.getId());

        // 파이프라인 자동 시작 (비동기)
        try {
            log.info("파이프라인 실행 시작: workspaceId={}, documentId={}", workspaceId, savedDocument.getId());
            pipelineOrchestrator.executeAsync(workspaceId, savedDocument.getId());
            log.info("파이프라인 실행 요청 완료");
        } catch (Exception e) {
            log.error("파이프라인 시작 실패 (문서는 저장됨): documentId={}", savedDocument.getId(), e);
            // 파이프라인 시작 실패 시에도 문서는 저장된 상태이므로 계속 진행
        }

        return DocumentResponseDto.from(savedDocument, 0L, 0L);
    }

    /**
     * Document 조회
     */
    @Transactional(readOnly = true)
    public DocumentResponseDto getDocument(Long documentId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다: " + documentId));

        Long pageCount = documentPageRepository.countByDocumentId(documentId);
        Long chunkCount = documentChunkRepository.countByDocumentId(documentId);

        return DocumentResponseDto.from(document, pageCount, chunkCount);
    }

    /**
     * 워크스페이스의 Document 목록 조회
     */
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getDocumentsByWorkspace(Long workspaceId) {
        List<DocumentEntity> documents = documentRepository
                .findByWorkspaceIdAndIsActiveTrueOrderByCreatedAtDesc(workspaceId);

        return documents.stream()
                .map(doc -> {
                    Long pageCount = documentPageRepository.countByDocumentId(doc.getId());
                    Long chunkCount = documentChunkRepository.countByDocumentId(doc.getId());
                    return DocumentResponseDto.from(doc, pageCount, chunkCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Document 삭제 (Hard Delete - cascade로 pages, chunks도 삭제됨)
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다: " + documentId));

        // Mark sync needed
        WorkspaceEntity workspace = document.getWorkspace();
        workspace.setNeedsArangoSync(true);
        workspaceRepository.save(workspace);

        // Fetch Chunk IDs for Ontology Cleanup before deletion
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
        List<Long> chunkIds = chunks.stream().map(DocumentChunk::getId).toList();

        // Hard delete - JPA cascade 설정에 따라 document_page, document_chunk도 삭제됨
        documentRepository.delete(document);
        log.info("Document 삭제 완료 (hard delete): id={}", documentId);

        // Ontology Cleanup (Reference Counting)
        ontologyPersistenceService.removeDocumentSource(documentId, chunkIds);
        log.info("Ontology source references verified/removed for documentId={}", documentId);
    }

    /**
     * Document 정보 수정 (제목)
     */
    @Transactional
    public DocumentResponseDto updateDocument(Long documentId,
            com.knowlearnmap.document.dto.DocumentUpdateRequest request) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다: " + documentId));

        if (request.getFilename() != null && !request.getFilename().isEmpty()) {
            document.setFilename(request.getFilename());
        }

        DocumentEntity updated = documentRepository.save(document);
        log.info("Document 수정 완료: id={}, newFilename={}", documentId, request.getFilename());

        return DocumentResponseDto.from(updated);
    }

    /**
     * Document의 모든 페이지 조회
     */
    @Transactional(readOnly = true)
    public List<DocumentPageDto> getDocumentPages(Long documentId) {
        // Document 존재 확인
        if (!documentRepository.existsById(documentId)) {
            throw new IllegalArgumentException("문서를 찾을 수 없습니다: " + documentId);
        }

        // 페이지 조회 및 DTO 변환
        return documentPageRepository.findByDocumentIdOrderByPageNumber(documentId).stream()
                .map(page -> {
                    // content 기반으로 간단한 단어 수 계산
                    int wordCount = 0;
                    if (page.getContent() != null && !page.getContent().isEmpty()) {
                        wordCount = page.getContent().split("\\s+").length;
                    }

                    return DocumentPageDto.builder()
                            .id(page.getId())
                            .pageNumber(page.getPageNumber())
                            .content(page.getContent())
                            .wordCount(wordCount)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
