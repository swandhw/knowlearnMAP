package com.knowlearnmap.workspace.repository;

import com.knowlearnmap.workspace.domain.WorkspaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Workspace JPA Repository
 * 
 * 기본 CRUD 및 커스텀 쿼리 메서드
 */
@Repository
public interface WorkspaceRepository extends JpaRepository<WorkspaceEntity, Long> {

    /**
     * 이름으로 워크스페이스 조회
     */
    Optional<WorkspaceEntity> findByName(String name);

    /**
     * 생성자로 워크스페이스 목록 조회
     */
    List<WorkspaceEntity> findByCreatedBy(String createdBy);

    /**
     * 활성 상태의 워크스페이스 목록 조회
     */
    List<WorkspaceEntity> findAllByIsActiveTrueOrderByCreatedAtDesc();

    /**
     * 폴더명으로 워크스페이스 조회
     */
    Optional<WorkspaceEntity> findByFolderName(String folderName);

    /**
     * 프롬프트 코드로 워크스페이스 조회
     */
    Optional<WorkspaceEntity> findByPromptCode(String promptCode);

    /**
     * 이름 중복 체크
     */
    boolean existsByName(String name);
}
