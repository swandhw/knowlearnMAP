package com.knowlearnmap.workspace.service;

import com.knowlearnmap.workspace.domain.WorkspaceEntity;
import com.knowlearnmap.workspace.dto.WorkspaceRequestDto;
import com.knowlearnmap.workspace.dto.WorkspaceResponseDto;
import com.knowlearnmap.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Workspace Service 구현체
 * 
 * 워크스페이스 비즈니스 로직 처리
 * - 생성, 조회, 수정, 삭제 (CRUD)
 * - DTO 변환
 * - 비즈니스 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    // TODO: DocumentRepository 추가 후 문서 개수 조회 기능 구현

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponseDto> getAllWorkspaces() {
        log.debug("모든 활성 워크스페이스 조회");

        List<WorkspaceEntity> workspaces = workspaceRepository.findAllByIsActiveTrueOrderByCreatedAtDesc();

        return workspaces.stream()
                .map(entity -> {
                    // TODO: 실제 문서 개수 조회로 변경
                    int documentCount = 0;
                    return WorkspaceResponseDto.from(entity, documentCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponseDto getWorkspaceById(Long id) {
        log.debug("워크스페이스 조회: id={}", id);

        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + id));

        // TODO: 실제 문서 개수 조회로 변경
        int documentCount = 0;

        return WorkspaceResponseDto.from(workspace, documentCount);
    }

    @Override
    @Transactional
    public WorkspaceResponseDto createWorkspace(WorkspaceRequestDto requestDto) {
        log.debug("워크스페이스 생성: name={}", requestDto.getName());

        // TODO: 사용자 인증 구현 후 사용자별 이름 중복 체크로 변경
        // 현재는 로그인별로 등록하므로 이름 중복 허용

        // DTO -> Entity 변환
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setName(requestDto.getName());
        workspace.setDescription(requestDto.getDescription());
        workspace.setIcon(requestDto.getIcon() != null ? requestDto.getIcon() : "📄");
        workspace.setColor(requestDto.getColor() != null ? requestDto.getColor() : "default");
        workspace.setWorkspaceType(requestDto.getWorkspaceType());
        workspace.setArangoDbName(requestDto.getArangoDbName());
        workspace.setFolderName(requestDto.getFolderName());
        workspace.setPromptCode(requestDto.getPromptCode());
        // TODO: 사용자 인증 구현 후 createdBy 설정

        WorkspaceEntity savedWorkspace = workspaceRepository.save(workspace);

        log.info("워크스페이스 생성 완료: id={}, name={}", savedWorkspace.getId(), savedWorkspace.getName());

        return WorkspaceResponseDto.from(savedWorkspace, 0);
    }

    @Override
    @Transactional
    public WorkspaceResponseDto updateWorkspace(Long id, WorkspaceRequestDto requestDto) {
        log.debug("워크스페이스 수정: id={}", id);

        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + id));

        // TODO: 사용자 인증 구현 후 사용자별 이름 중복 체크로 변경
        // 현재는 로그인별로 등록하므로 이름 중복 허용

        // 엔티티 업데이트
        workspace.setName(requestDto.getName());
        workspace.setDescription(requestDto.getDescription());
        workspace.setIcon(requestDto.getIcon());
        workspace.setColor(requestDto.getColor());
        workspace.setWorkspaceType(requestDto.getWorkspaceType());
        workspace.setArangoDbName(requestDto.getArangoDbName());
        workspace.setPromptCode(requestDto.getPromptCode());

        // folderName 업데이트 (제공된 경우에만)
        if (requestDto.getFolderName() != null && !requestDto.getFolderName().isEmpty()) {
            workspace.setFolderName(requestDto.getFolderName());
        }

        WorkspaceEntity updatedWorkspace = workspaceRepository.save(workspace);

        log.info("워크스페이스 수정 완료: id={}, name={}", updatedWorkspace.getId(), updatedWorkspace.getName());

        // TODO: 실제 문서 개수 조회로 변경
        return WorkspaceResponseDto.from(updatedWorkspace, 0);
    }

    @Override
    @Transactional
    public void deleteWorkspace(Long id) {
        log.debug("워크스페이스 삭제: id={}", id);

        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + id));

        // Soft delete
        workspace.setIsActive(false);
        workspaceRepository.save(workspace);

        log.info("워크스페이스 삭제 완료 (soft delete): id={}, name={}", workspace.getId(), workspace.getName());
    }
}
