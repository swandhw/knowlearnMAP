package com.knowlearnmap.document.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Document 업로드 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {

    private Long workspaceId;
    private String sourceType; // FILE, URL, YOUTUBE
}
