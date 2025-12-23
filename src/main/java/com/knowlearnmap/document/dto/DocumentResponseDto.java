package com.knowlearnmap.document.dto;

import com.knowlearnmap.document.domain.DocumentEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Document 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDto {

    private Long id;
    private String filename;
    private String filePath;
    private Long workspaceId;
    private String sourceType;
    private String status;
    private String pipelineStatus;
    private String pipelineStep;
    private Integer version;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 통계 정보
    private Long pageCount;
    private Long chunkCount;

    public static DocumentResponseDto from(DocumentEntity entity) {
        DocumentResponseDto dto = new DocumentResponseDto();
        dto.setId(entity.getId());
        dto.setFilename(entity.getFilename());
        dto.setFilePath(entity.getFilePath());
        dto.setWorkspaceId(entity.getWorkspace().getId());
        dto.setSourceType(entity.getSourceType() != null ? entity.getSourceType().name() : null);
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setPipelineStatus(entity.getPipelineStatus() != null ? entity.getPipelineStatus().name() : null);
        dto.setPipelineStep(entity.getPipelineStep());
        dto.setVersion(entity.getVersion());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public static DocumentResponseDto from(DocumentEntity entity, Long pageCount, Long chunkCount) {
        DocumentResponseDto dto = from(entity);
        dto.setPageCount(pageCount);
        dto.setChunkCount(chunkCount);
        return dto;
    }
}
