package com.knowlearnmap.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DocumentPage DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentPageDto {

    private Long id;
    private Integer pageNumber;
    private String content;
    private Integer wordCount;
}
