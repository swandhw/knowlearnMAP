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
     * 도메인 ID로 활성 워크스페이스 조회
     */
    List<WorkspaceEntity> findByDomainIdAndIsActiveTrueOrderByCreatedAtDesc(Long domainId);

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

    /**
     * 도메인별 활성 워크스페이스 개수 조회
     */
    /**
     * 도메인별 활성 워크스페이스 개수 조회
     */
    int countByDomainIdAndIsActiveTrue(Long domainId);

    /**
     * 공유된 워크스페이스 포함 조회 (내 것 + 공유된 것)
     */
    @Query("SELECT w FROM WorkspaceEntity w WHERE w.domain.id = :domainId AND w.isActive = true AND (w.createdBy = :username OR w.isShared = true) ORDER BY w.createdAt DESC")
    List<WorkspaceEntity> findSharedAndOwnedWorkspaces(
            @org.springframework.data.repository.query.Param("domainId") Long domainId,
            @org.springframework.data.repository.query.Param("username") String username);

    /**
     * 내 워크스페이스만 조회
     */
    List<WorkspaceEntity> findByDomainIdAndCreatedByAndIsActiveTrueOrderByCreatedAtDesc(Long domainId,
            String createdBy);
}
