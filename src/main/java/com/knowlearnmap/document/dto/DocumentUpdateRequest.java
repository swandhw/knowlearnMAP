package com.knowlearnmap.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Document 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdateRequest {
    private String filename;
}
