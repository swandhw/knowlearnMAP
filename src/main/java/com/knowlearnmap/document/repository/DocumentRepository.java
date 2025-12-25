package com.knowlearnmap.document.repository;

import com.knowlearnmap.document.domain.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    /**
     * 워크스페이스의 활성 문서 목록 조회
     */
    List<DocumentEntity> findByWorkspaceIdAndIsActiveTrueOrderByCreatedAtDesc(Long workspaceId);

    /**
     * 워크스페이스의 모든 문서 조회
     */
    List<DocumentEntity> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);

    /**
     * 파일명으로 문서 조회 (워크스페이스 내)
     */
    List<DocumentEntity> findByWorkspaceIdAndFilename(Long workspaceId, String filename);

    /**
     * 워크스페이스의 활성 문서 개수 조회
     */
    int countByWorkspaceIdAndIsActiveTrue(Long workspaceId);
}
