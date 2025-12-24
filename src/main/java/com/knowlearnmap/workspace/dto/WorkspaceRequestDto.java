package com.knowlearnmap.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Workspace 생성/수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceRequestDto {

    @NotBlank(message = "워크스페이스 이름은 필수입니다")
    @Size(max = 255, message = "이름은 255자를 초과할 수 없습니다")
    private String name;

    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    private String description;

    private String icon;

    private String color;

    private String workspaceType;

    private Long domainId;

    private String folderName;

    private String promptCode;
}
