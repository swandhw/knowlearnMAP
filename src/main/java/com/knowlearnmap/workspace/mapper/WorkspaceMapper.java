package com.knowlearnmap.workspace.mapper;

import com.knowlearnmap.common.annotation.ConnMapperFirst;
import com.knowlearnmap.workspace.domain.WorkspaceEntity;

import java.util.List;

/**
 * Workspace MyBatis Mapper
 * 
 * 복잡한 쿼리나 동적 쿼리가 필요한 경우 사용
 * 현재는 기본 JPA로 충분하지만 향후 확장을 위해 준비
 */
@ConnMapperFirst
public interface WorkspaceMapper {

    /**
     * 워크스페이스 목록 조회 (동적 쿼리 지원)
     */
    List<WorkspaceEntity> selectWorkspaceList(WorkspaceEntity condition);

    /**
     * 워크스페이스별 문서 개수 조회
     */
    int selectDocumentCountByWorkspaceId(Long workspaceId);
}
