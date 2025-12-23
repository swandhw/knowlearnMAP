package com.knowlearnmap.document.repository;

import com.knowlearnmap.document.domain.DocumentPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentPageRepository extends JpaRepository<DocumentPage, Long> {

    /**
     * 문서의 모든 페이지 조회 (페이지 번호 순)
     */
    List<DocumentPage> findByDocumentIdOrderByPageNumber(Long documentId);

    /**
     * 문서의 특정 페이지 조회
     */
    DocumentPage findByDocumentIdAndPageNumber(Long documentId, Integer pageNumber);

    /**
     * 문서의 페이지 개수 조회
     */
    long countByDocumentId(Long documentId);

    /**
     * 문서의 특정 상태 페이지 조회
     */
    List<DocumentPage> findByDocumentIdAndStatus(Long documentId, DocumentPage.PageStatus status);
}
