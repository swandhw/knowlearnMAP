package com.knowlearnmap.pipeline.upload;

import com.knowlearnmap.pipeline.core.PipelineContext;
import com.knowlearnmap.pipeline.core.PipelineException;
import com.knowlearnmap.pipeline.core.PipelineStage;
import com.knowlearnmap.pipeline.core.StageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Document upload stage processor.
 * 
 * <p>
 * This processor handles document upload and validation.
 * Initially supports PDF files only.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUploadProcessor implements StageProcessor {

    @Override
    public void process(PipelineContext context) throws PipelineException {
        log.info("Processing document upload for document={}", context.getDocumentId());

        // Stage 1: Upload is typically done via API before pipeline starts
        // This processor validates that the document exists and is ready for processing

        try {
            // Validate document exists
            if (context.getDocumentId() == null) {
                throw new PipelineException("Document ID is null", getStage());
            }

            // Add metadata
            context.addMetadata("upload_validated", true);
            context.setProgress(10);

            log.info("Document upload validated successfully");

        } catch (Exception e) {
            throw new PipelineException("Document upload validation failed", getStage(), e);
        }
    }

    @Override
    public PipelineStage getStage() {
        return PipelineStage.UPLOAD;
    }
}
