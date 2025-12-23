package com.knowlearnmap.document.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DocumentPage 엔티티
 * 
 * Document의 페이지/섹션별 정보
 * - PDF: 실제 페이지 번호
 * - URL: 섹션 번호 (헤더, 본문 등)
 * - YouTube: 타임스탬프 구간
 */
@Entity
@Table(name = "document_pages")
@Getter
@Setter
@NoArgsConstructor
public class DocumentPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private DocumentEntity document;

    /**
     * 범용 식별자
     * - PDF: 페이지 번호 (1, 2, 3...)
     * - URL: 섹션 번호
     * - YouTube: 타임스탬프 구간
     */
    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PageStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PageStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public DocumentPage(DocumentEntity document, Integer pageNumber, String content, PageStatus status) {
        this.document = document;
        this.pageNumber = pageNumber;
        this.content = content;
        this.status = status;
    }

    public enum PageStatus {
        PENDING,
        COMPLETED,
        FAILED
    }
}
