package com.knowlearnmap.pipeline.parser;

import com.knowlearnmap.document.domain.DocumentEntity;
import com.knowlearnmap.document.domain.DocumentPage;
import com.knowlearnmap.document.repository.DocumentPageRepository;
import com.knowlearnmap.document.repository.DocumentRepository;
import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import com.knowlearnmap.pipeline.core.PipelineStage;
import com.knowlearnmap.pipeline.core.StageProcessor;
import com.knowlearnmap.rag.service.PdfParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PDF parsing stage processor.
 * 
 * <p>
 * Extracts text and structure from PDF documents.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfParserProcessor implements StageProcessor {

    private final DocumentRepository documentRepository;
    private final DocumentPageRepository documentPageRepository;
    private final PdfParsingService pdfParsingService;

    @Override
    @Transactional
    public void process(PipelineContext context) throws PipelineException {
        log.info("Processing PDF parsing for document={}", context.getDocumentId());

        try {
            // 문서 조회
            DocumentEntity document = documentRepository.findById(context.getDocumentId())
                    .orElseThrow(() -> new PipelineException(
                            "Document not found: " + context.getDocumentId(),
                            getStage(),
                            null,
                            false));

            // PDF 파싱
            List<DocumentPage> pages = pdfParsingService.extractPages(document);

            // 페이지 저장
            documentPageRepository.saveAll(pages);

            // 컨텍스트 업데이트
            context.addMetadata("pdf_parsed", true);
            context.addMetadata("page_count", pages.size());
            context.setProgress(25);

            log.info("PDF parsing completed successfully: {} pages extracted", pages.size());

        } catch (Exception e) {
            throw new PipelineException("PDF parsing failed", getStage(), e, true);
        }
    }

    @Override
    public PipelineStage getStage() {
        return PipelineStage.PARSE;
    }
}
