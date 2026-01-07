package com.knowlearnmap.workspace.dto;

import com.knowlearnmap.workspace.domain.WorkspaceEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Workspace 조회 응답 DTO
 * 
 * 프론트엔드 mockData 형식과 호환
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponseDto {

    private Long id;
    private String title; // name과 동일 (프론트엔드 호환)
    private String name;
    private String description;
    private String icon;
    private String color;
    private String source; // "소스 N개" 형식
    private String date; // "2025. 12. 18." 형식
    private String role; // "Owner", "Reader" 등
    private Boolean isActive;
    private String workspaceType;
    private String arangoDbName;
    private Long domainId;
    private String folderName;
    private String promptCode;
    private Integer documentCount; // 문서 개수
    private String syncStatus; // Changed from needsArangoSync
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastModifiedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private Boolean isShared;

    /**
     * Entity -> DTO 변환
     */
    public static WorkspaceResponseDto from(WorkspaceEntity entity) {
        WorkspaceResponseDto dto = new WorkspaceResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setTitle(entity.getName()); // 프론트엔드 호환
        dto.setDescription(entity.getDescription());
        dto.setIcon(entity.getIcon() != null ? entity.getIcon() : "📄");
        dto.setColor(entity.getColor() != null ? entity.getColor() : "default");
        dto.setRole("Owner"); // 기본값 (향후 권한 관리 시 변경)
        dto.setIsActive(entity.getIsActive());
        dto.setWorkspaceType(entity.getWorkspaceType());
        if (entity.getDomain() != null) {
            dto.setArangoDbName(entity.getDomain().getArangoDbName());
            dto.setDomainId(entity.getDomain().getId());
        }
        dto.setFolderName(entity.getFolderName());
        dto.setPromptCode(entity.getPromptCode());
        dto.setSyncStatus(entity.getSyncStatus() != null ? entity.getSyncStatus().name() : "SYNCED");
        dto.setLastSyncedAt(entity.getLastSyncedAt());
        dto.setLastModifiedAt(entity.getLastModifiedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setIsShared(entity.getIsShared());

        // 날짜 포맷 변환
        if (entity.getCreatedAt() != null) {
            dto.setDate(formatDate(entity.getCreatedAt()));
        }

        return dto;
    }

    /**
     * Entity + documentCount -> DTO 변환
     */
    public static WorkspaceResponseDto from(WorkspaceEntity entity, int documentCount) {
        WorkspaceResponseDto dto = from(entity);
        dto.setDocumentCount(documentCount);
        dto.setSource("소스 " + documentCount + "개");
        return dto;
    }

    /**
     * 날짜를 "2025. 12. 18." 형식으로 변환
     */
    private static String formatDate(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy. MM. dd.");
        return dateTime.format(formatter);
    }
}
