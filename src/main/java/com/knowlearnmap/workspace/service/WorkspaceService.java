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
     * 모든 활성 워크스페이스 조회
     */
    List<WorkspaceResponseDto> getAllWorkspaces();

    /**
     * ID로 워크스페이스 조회
     */
    WorkspaceResponseDto getWorkspaceById(Long id);

    /**
     * 새 워크스페이스 생성
     */
    WorkspaceResponseDto createWorkspace(WorkspaceRequestDto requestDto);

    /**
     * 워크스페이스 수정
     */
    WorkspaceResponseDto updateWorkspace(Long id, WorkspaceRequestDto requestDto);

    /**
     * 워크스페이스 삭제 (soft delete)
     */
    void deleteWorkspace(Long id);
}
