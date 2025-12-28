package com.knowlearnmap.llmToOntology.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.knowlearnmap.llmToOntology.domain.OntologyObjectDict;
import com.knowlearnmap.llmToOntology.domain.OntologyRelationDict;
import com.knowlearnmap.llmToOntology.dto.OntologyDto;
import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LLM 결과 → Ontology 생성 서비스
 * 
 * <h3>처리 흐름</h3>
 * 
 * <pre>
 * 1. Chunk 테이블에서 llm_status = 'COMPLETED' 조회
 * 2. chunk.llm_result JSON 파싱
 * 3. Ontology 엔티티 생성 및 저장
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmToOntologyService {

    private final DocumentChunkRepository chunkRepository;
    private final OntologyPersistenceService ontologyPersistenceService;

    // Self-injection for Transactional method calls within loop
    @Lazy
    @Autowired
    private LlmToOntologyService self;

    /**
     * Chunk의 LLM 결과로 Ontology 생성
     */
    @Transactional
    public void createOntologyFromLlmResult(Long workspaceId, DocumentChunk chunk) {
        if (!"COMPLETED".equals(chunk.getLlmStatus())) {
            throw new RuntimeException("LLM 처리가 완료되지 않음: " + chunk.getId());
        }

        try {
            // 진행 상태: PROCESSING 업데이트
            chunk.setOntologyStatus("PROCESSING");
            chunkRepository.saveAndFlush(chunk);

            log.info("Ontology 생성 시작 - Chunk ID: {}", chunk.getId());

            // JSON 파싱
            JsonObject rootObj = JsonParser.parseString(chunk.getLlmResult()).getAsJsonObject();

            // Ontology 처리
            processOntologies(workspaceId, chunk, rootObj);

            // 성공 처리
            chunk.setOntologyStatus("COMPLETED");
            chunk.setOntologyProcessedAt(LocalDateTime.now());
            chunk.setOntologyErrorMessage(null);
            chunkRepository.save(chunk);

            log.info("Ontology 생성 완료 - Chunk ID: {}", chunk.getId());

        } catch (JsonSyntaxException e) {
            // JSON 파싱 실패
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }

            chunk.setOntologyStatus("FAILED");
            chunk.setOntologyErrorMessage("JSON Parsing Error: " + errorMsg);
            chunkRepository.save(chunk);

            // stack trace 없이 간략한 로그만 남기고 다음으로 진행 (Exception rethrow 안함)
            log.warn("JSON 파싱 실패 (건너뜀) - Chunk ID: {}. Error: {}", chunk.getId(), e.getMessage());

        } catch (Exception e) {
            // 기타 실패
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }

            chunk.setOntologyStatus("FAILED");
            chunk.setOntologyErrorMessage(errorMsg);
            chunkRepository.save(chunk);

            log.error("Ontology 생성 실패 - Chunk ID: {}", chunk.getId(), e);
            // 여기서 throw 하면 Transaction이 롤백되어 FAILED 상태 저장이 취소될 수 있으므로,
            // 개별 트랜잭션에서는 throw 하지 않고 상태만 저장하고 종료하는 것이 배치 처리에 유리함.
        }
    }

    private void processOntologies(Long workspaceId, DocumentChunk chunk, JsonObject rootObj) {
        if (rootObj.has("chunks") && rootObj.get("chunks").isJsonArray()) {
            JsonArray chunksArray = rootObj.getAsJsonArray("chunks");
            for (JsonElement chunkElement : chunksArray) {
                processSingleChunkJson(workspaceId, chunk, chunkElement.getAsJsonObject());
            }
        } else {
            // 단일 객체인 경우 (구조에 따라 다름)
            processSingleChunkJson(workspaceId, chunk, rootObj);
        }
    }

    private void processSingleChunkJson(Long workspaceId, DocumentChunk chunk, JsonObject chunkObj) {
        // 1. Objects 처리
        if (chunkObj.has("objects_to_add") && chunkObj.get("objects_to_add").isJsonArray()) {
            JsonArray objectsArray = chunkObj.getAsJsonArray("objects_to_add");
            for (JsonElement element : objectsArray) {
                processObject(workspaceId, chunk, element.getAsJsonObject());
            }
        }

        // 2. Relations 처리
        if (chunkObj.has("relations_to_add") && chunkObj.get("relations_to_add").isJsonArray()) {
            JsonArray relationsArray = chunkObj.getAsJsonArray("relations_to_add");
            for (JsonElement element : relationsArray) {
                processRelation(workspaceId, chunk, element.getAsJsonObject());
            }
        }

        // 3. Knowlearns (Triples) 처리
        if (chunkObj.has("knowlearns_to_add") && chunkObj.get("knowlearns_to_add").isJsonArray()) {
            JsonArray knowlearnsArray = chunkObj.getAsJsonArray("knowlearns_to_add");
            for (JsonElement element : knowlearnsArray) {
                processKnowlearn(workspaceId, chunk, element.getAsJsonObject());
            }
        }
    }

    private void processObject(Long workspaceId, DocumentChunk chunk, JsonObject obj) {
        OntologyObjectDict dict = new OntologyObjectDict();
        dict.setCategory(getJsonString(obj, "category"));

        String termEn = getJsonString(obj, "term_en");
        String termKo = getJsonString(obj, "term_ko");

        // Fallback logic for term_ko
        if (termKo == null || termKo.trim().isEmpty()) {
            termKo = termEn;
        }
        if (termKo == null || termKo.trim().isEmpty()) {
            termKo = "Unknown";
        }

        dict.setTermEn(termEn);
        dict.setTermKo(termKo);
        dict.setDescription(getJsonString(obj, "description_ko"));

        ontologyPersistenceService.findAndSaveObjectDict(workspaceId, chunk, dict);
    }

    private void processRelation(Long workspaceId, DocumentChunk chunk, JsonObject obj) {
        OntologyRelationDict dict = new OntologyRelationDict();
        dict.setCategory(getJsonString(obj, "category"));

        String relationEn = getJsonString(obj, "relation_en");
        String relationKo = getJsonString(obj, "relation_ko");

        // Fallback logic for relation_ko
        if (relationKo == null || relationKo.trim().isEmpty()) {
            relationKo = relationEn;
        }
        if (relationKo == null || relationKo.trim().isEmpty()) {
            relationKo = "Unknown";
        }

        dict.setRelationEn(relationEn);
        dict.setRelationKo(relationKo);
        dict.setDescription(getJsonString(obj, "description_ko"));

        ontologyPersistenceService.findAndSaveRelationDict(workspaceId, chunk, dict);
    }

    private void processKnowlearn(Long workspaceId, DocumentChunk chunk, JsonObject obj) {
        OntologyDto dto = new OntologyDto();
        dto.setSubjectCategory(getJsonString(obj, "subject_category"));
        dto.setSubjectTermEn(getJsonString(obj, "subject_term_en"));
        dto.setSubjectTermKo(getJsonString(obj, "subject_term_ko"));

        dto.setRelationCategory(getJsonString(obj, "relation_category"));
        dto.setRelationEn(getJsonString(obj, "relation_en"));
        dto.setRelationKo(getJsonString(obj, "relation_ko"));

        dto.setObjectCategory(getJsonString(obj, "object_category"));
        dto.setObjectTermEn(getJsonString(obj, "object_term_en"));
        dto.setObjectTermKo(getJsonString(obj, "object_term_ko"));

        if (obj.has("confidence_score")) {
            try {
                dto.setConfidenceScore(java.math.BigDecimal.valueOf(obj.get("confidence_score").getAsDouble()));
            } catch (Exception e) {
                dto.setConfidenceScore(java.math.BigDecimal.ZERO);
            }
        }
        dto.setEvidenceLevel(getJsonString(obj, "evidence_level"));

        ontologyPersistenceService.findAndSaveKnowlearnType(workspaceId, chunk, dto);
    }

    private String getJsonString(JsonObject obj, String memberName) {
        if (obj.has(memberName) && !obj.get(memberName).isJsonNull()) {
            return obj.get(memberName).getAsString();
        }
        return null;
    }

    /**
     * 특정 문서의 모든 Chunk에 대해 Ontology 생성
     * 
     * @return 처리된 청크 개수
     */
    // @Transactional 제거: 각 청크별로 트랜잭션을 분리하기 위함
    public int createOntologyFromDocument(Long workspaceId, Long documentId) {
        // 1. 해당 문서의 LLM 완료된 청크 조회 (Repository 최적화)
        // TODO: ontology_status가 'COMPLETED'가 아닌 것만 조회하는 것이 효율적일 수 있음.
        // 하지만 요구사항에 따라 전체 스캔 혹은 재처리 로직이 필요할 수 있음.
        List<DocumentChunk> targetChunks = chunkRepository.findByDocumentIdAndLlmStatus(documentId, "COMPLETED");

        log.info("Found {} completed chunks for document {}", targetChunks.size(), documentId);

        // 3. 각 청크에 대해 Ontology 생성 수행
        int successCount = 0;
        for (DocumentChunk chunk : targetChunks) {
            try {
                // 이미 성공한 것은 건너뛸 수도 있으나, 여기서는 상태가 없는 경우도 있으므로 강제 재실행을 막지는 않음
                // 필요 시 if ("COMPLETED".equals(chunk.getOntologyStatus())) continue; 추가 가능

                // self 참조를 통해 프록시를 경유하여 트랜잭션 적용
                self.createOntologyFromLlmResult(workspaceId, chunk);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to create ontology for chunk {}", chunk.getId(), e);
                // 개별 실패는 로그 남기고 계속 진행
            }
        }

        return successCount;
    }

    /**
     * 처리 대기 중인 청크 목록 조회 (LLM 완료됨)
     */
    public List<DocumentChunk> getPendingChunks() {
        return chunkRepository.findByLlmStatus("COMPLETED");
    }
}
