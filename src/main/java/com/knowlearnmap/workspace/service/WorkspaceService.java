package com.knowlearnmap.workspace.service;

import com.knowlearnmap.workspace.dto.WorkspaceRequestDto;
import com.knowlearnmap.workspace.dto.WorkspaceResponseDto;

import java.util.List;

/**
 * Workspace Service 인터페이스
 * 
 * 워크스페이스 비즈니스 로직 정의
 */
public interface WorkspaceService {

    /**
     * 사용자의 도메인에 따른 모든 활성 워크스페이스 조회
     */
    /**
     * 사용자의 도메인에 따른 모든 활성 워크스페이스 조회
     * 
     * @param filter "ALL" (전체+공유) or "MY" (내 것만)
     */
    List<WorkspaceResponseDto> getAllWorkspaces(String username, Long domainId, String filter);

    /**
     * ID로 워크스페이스 조회
     */
    WorkspaceResponseDto getWorkspaceById(Long id);

    /**
     * 새 워크스페이스 생성
     */
    WorkspaceResponseDto createWorkspace(WorkspaceRequestDto requestDto, String username);

    /**
     * 워크스페이스 수정
     */
    WorkspaceResponseDto updateWorkspace(Long id, WorkspaceRequestDto requestDto, String username);

    /**
     * 워크스페이스 삭제 (soft delete)
     */
    void deleteWorkspace(Long id, String username);

    /**
     * 워크스페이스를 동기화 필요 상태로 변경
     */
    void markSyncNeeded(Long workspaceId);

    /**
     * 워크스페이스를 동기화 중 상태로 변경
     */
    void markSyncing(Long workspaceId);

    /**
     * 워크스페이스를 동기화 완료 상태로 변경
     */
    void markSynced(Long workspaceId);
}
