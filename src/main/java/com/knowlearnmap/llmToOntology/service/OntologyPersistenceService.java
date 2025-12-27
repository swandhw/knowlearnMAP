package com.knowlearnmap.llmToOntology.service;

import com.knowlearnmap.llmToOntology.domain.*;
import com.knowlearnmap.llmToOntology.dto.OntologyDto;
import com.knowlearnmap.document.domain.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Ontology 저장 서비스
 * 
 * <p>
 * Ontology 관련 엔티티들의 저장 및 조회를 담당합니다.
 * 각 메서드는 독립적인 트랜잭션(REQUIRES_NEW)으로 실행되어
 * 대량 처리 시 일부 실패가 전체 롤백으로 이어지지 않도록 합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OntologyPersistenceService {

    private final OntologyObjectDictRepository objectDictRepository;
    private final OntologyObjectTypeRepository objectTypeRepository;
    private final OntologyRelationDictRepository relationDictRepository;
    private final OntologyRelationTypeRepository relationTypeRepository;
    private final OntologyKnowlearnTypeRepository knowlearnTypeRepository;
    private final OntologyObjectSynonymsRepository objectSynonymsRepository;
    private final OntologyRelationSynonymsRepository relationSynonymsRepository;

    private final OntologySynonymService synonymService;

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private String addSource(String currentSource, String newId) {
        try {
            java.util.Set<String> sources = new java.util.HashSet<>();
            if (currentSource != null && !currentSource.trim().isEmpty()) {
                try {
                    // Try parsing as JSON Array
                    java.util.List<String> parsed = objectMapper.readValue(currentSource,
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                            });
                    sources.addAll(parsed);
                } catch (Exception e) {
                    // Fallback: Treat as single string (legacy data like "initial_data")
                    // Remove potential JSON brackets if malformed? No, just raw value.
                    // If it looks like JSON array but failed, it's risky. But safe assumption is:
                    // If it's "initial_data", add it.
                    // If it's "[\"123\"]" and failed? Unlikely if ObjectMapper works.
                    // We assume valid JSON or plain string.
                    String clean = currentSource.trim();
                    if (clean.startsWith("[") && clean.endsWith("]")) {
                        // It was a JSON array but parse failed (maybe malformed quotes).
                        // Check logs? For now, we might reset it or try to keep it.
                        // Let's just treat it as a string to be safe, though "['123']" as a string is
                        // weird.
                        // Better policy: Just ignore legacy if it fails parsing?
                        // User said "initial_data".
                    } else {
                        sources.add(clean);
                    }
                }
            }
            sources.add(newId);
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            log.error("Error updating source for ID: {}", newId, e);
            return "[\"" + newId + "\"]";
        }
    }

    private OntologyObjectDict updateSourceAndReturn(OntologyObjectDict dict, String docId) {
        dict.setSource(addSource(dict.getSource(), docId));
        return dict;
    }

    private OntologyRelationDict updateSourceAndReturn(OntologyRelationDict dict, String docId) {
        dict.setSource(addSource(dict.getSource(), docId));
        return dict;
    }

    public String SpaceRemover(String str) {
        if (str == null)
            return "";
        return str.replaceAll("\\s", "");
    }

    /**
     * Object Dictionary 저장 또는 조회
     * 
     * 조회 순서 : 영어 Dict, 영어 Sym, 공백없는 영어 Dict, 공백없는 Sym, 한글도 같은 순서로
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OntologyObjectDict findAndSaveObjectDict(Long workspaceId, DocumentChunk chunk, OntologyObjectDict dict) {
        String docId = String.valueOf(chunk.getDocument().getId());
        String category = this.SpaceRemover(dict.getCategory());
        String termEn = this.SpaceRemover(dict.getTermEn());
        String termKo = this.SpaceRemover(dict.getTermKo());
        try {
            // 영문 용어로 먼저 검색
            Optional<OntologyObjectDict> existing = objectDictRepository
                    .findByWorkspaceIdAndCategoryAndTermEn(workspaceId, category, dict.getTermEn());
            if (existing.isPresent()) {
                return updateSourceAndReturn(existing.get(), docId);
            }

            Optional<OntologyObjectSynonyms> existingSym = objectSynonymsRepository
                    .findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, dict.getTermEn());
            if (existingSym.isPresent()) {
                existing = objectDictRepository.findById(existingSym.get().getObjectId());
                return updateSourceAndReturn(existing.get(), docId);
            }

            if (!termEn.equals(dict.getTermEn())) {
                existing = objectDictRepository
                        .findByWorkspaceIdAndCategoryAndTermEn(workspaceId, category, termEn);
                if (existing.isPresent()) {
                    return updateSourceAndReturn(existing.get(), docId);
                }

                existingSym = objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                        termEn);
                if (existingSym.isPresent()) {
                    existing = objectDictRepository.findById(existingSym.get().getObjectId());
                    return updateSourceAndReturn(existing.get(), docId);
                }
            }

            // 한글 용어로 검색
            existing = objectDictRepository
                    .findByWorkspaceIdAndCategoryAndTermKo(workspaceId, category, dict.getTermKo());
            if (existing.isPresent()) {
                return updateSourceAndReturn(existing.get(), docId);
            }

            existingSym = objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                    dict.getTermKo());
            if (existingSym.isPresent()) {
                existing = objectDictRepository.findById(existingSym.get().getObjectId());
                return updateSourceAndReturn(existing.get(), docId);
            }

            if (!termKo.equals(dict.getTermKo())) {
                existing = objectDictRepository
                        .findByWorkspaceIdAndCategoryAndTermKo(workspaceId, category, termKo);
                if (existing.isPresent()) {
                    return updateSourceAndReturn(existing.get(), docId);
                }

                existingSym = objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                        termKo);
                if (existingSym.isPresent()) {
                    existing = objectDictRepository.findById(existingSym.get().getObjectId());
                    return updateSourceAndReturn(existing.get(), docId);
                }
            }

            // 없으면 새로 저장
            dict.setWorkspaceId(workspaceId);
            dict.setCategory(category);
            dict.setSource(addSource(null, docId));
            OntologyObjectDict newDict = objectDictRepository.save(dict);

            // 띄어쓰기 제거해서 Sym 에 추가
            synonymService.saveObjectSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getTermEn(),
                    termEn, "en");
            synonymService.saveObjectSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getTermKo(),
                    termKo, "ko");

            return newDict;
        } catch (Exception e) {
            log.error("ObjectDict 저장 실패: {}", dict.getTermEn(), e);
            // 동시성 문제로 이미 저장되었을 수 있으므로 다시 조회 시도
            return objectDictRepository
                    .findByWorkspaceIdAndCategoryAndTermEn(workspaceId, dict.getCategory(), dict.getTermEn())
                    .orElseThrow(() -> new RuntimeException("ObjectDict 저장 및 조회 실패", e));
        }
    }

    /**
     * Relation Dictionary 저장 또는 조회
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OntologyRelationDict findAndSaveRelationDict(Long workspaceId, DocumentChunk chunk,
            OntologyRelationDict dict) {
        String docId = String.valueOf(chunk.getDocument().getId());
        String category = this.SpaceRemover(dict.getCategory());
        String relationEn = this.SpaceRemover(dict.getRelationEn());
        String relationKo = this.SpaceRemover(dict.getRelationKo());
        try {
            // 영문 용어로 먼저 검색
            Optional<OntologyRelationDict> existing = relationDictRepository
                    .findByWorkspaceIdAndCategoryAndRelationEn(workspaceId, category, dict.getRelationEn());
            if (existing.isPresent()) {
                return updateSourceAndReturn(existing.get(), docId);
            }

            Optional<OntologyRelationSynonyms> existingSym = relationSynonymsRepository
                    .findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, dict.getRelationEn());
            if (existingSym.isPresent()) {
                existing = relationDictRepository.findById(existingSym.get().getRelationId());
                return updateSourceAndReturn(existing.get(), docId);
            }

            if (!relationEn.equals(dict.getRelationEn())) {
                existing = relationDictRepository
                        .findByWorkspaceIdAndCategoryAndRelationEn(workspaceId, category, relationEn);
                if (existing.isPresent()) {
                    return updateSourceAndReturn(existing.get(), docId);
                }

                existingSym = relationSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                        relationEn);
                if (existingSym.isPresent()) {
                    existing = relationDictRepository.findById(existingSym.get().getRelationId());
                    return updateSourceAndReturn(existing.get(), docId);
                }
            }

            // 한글 용어로 검색
            existing = relationDictRepository
                    .findByWorkspaceIdAndCategoryAndRelationKo(workspaceId, category, dict.getRelationKo());
            if (existing.isPresent()) {
                return updateSourceAndReturn(existing.get(), docId);
            }

            existingSym = relationSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                    dict.getRelationKo());
            if (existingSym.isPresent()) {
                existing = relationDictRepository.findById(existingSym.get().getRelationId());
                return updateSourceAndReturn(existing.get(), docId);
            }

            if (!relationKo.equals(dict.getRelationKo())) {
                existing = relationDictRepository
                        .findByWorkspaceIdAndCategoryAndRelationKo(workspaceId, category, relationKo);
                if (existing.isPresent()) {
                    return updateSourceAndReturn(existing.get(), docId);
                }

                existingSym = relationSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                        relationKo);
                if (existingSym.isPresent()) {
                    existing = relationDictRepository.findById(existingSym.get().getRelationId());
                    return updateSourceAndReturn(existing.get(), docId);
                }
            }

            // 없으면 새로 저장
            dict.setWorkspaceId(workspaceId);
            dict.setCategory(category);
            dict.setSource(addSource(null, docId));
            OntologyRelationDict newDict = relationDictRepository.save(dict);

            // 띠어쓰기 제거해서 Sym 에 추가
            synonymService.saveRelationSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getRelationEn(),
                    relationEn, "en");
            synonymService.saveRelationSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getRelationKo(),
                    relationKo, "ko");

            return newDict;

        } catch (Exception e) {
            log.error("RelationDict 저장 실패: {}", dict.getRelationEn(), e);
            return relationDictRepository
                    .findByWorkspaceIdAndCategoryAndRelationEn(workspaceId, dict.getCategory(), dict.getRelationEn())
                    .orElseThrow(() -> new RuntimeException("RelationDict 저장 및 조회 실패", e));
        }
    }

    /**
     * Knowlearn Type (Triple) 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void findAndSaveKnowlearnType(Long workspaceId, DocumentChunk chunk, OntologyDto dto) {
        try {
            // 1. Subject 처리
            OntologyObjectDict subjectDict = new OntologyObjectDict();
            subjectDict.setCategory(dto.getSubjectCategory());
            subjectDict.setTermEn(dto.getSubjectTermEn());
            subjectDict.setTermKo(dto.getSubjectTermKo());
            subjectDict = findAndSaveObjectDict(workspaceId, chunk, subjectDict);

            // Subject Instance 생성 (없으면)
            // findAndSaveObjectType(workspaceId, subjectDict);

            // 2. Object 처리
            OntologyObjectDict objectDict = new OntologyObjectDict();
            objectDict.setCategory(dto.getObjectCategory());
            objectDict.setTermEn(dto.getObjectTermEn());
            objectDict.setTermKo(dto.getObjectTermKo());
            objectDict = findAndSaveObjectDict(workspaceId, chunk, objectDict);

            // Object Instance 생성 (없으면)
            // findAndSaveObjectType(workspaceId, objectDict);

            // 3. Relation 처리
            OntologyRelationDict relationDict = new OntologyRelationDict();
            relationDict.setCategory(dto.getRelationCategory());
            relationDict.setRelationEn(dto.getRelationEn());
            relationDict.setRelationKo(dto.getRelationKo());
            relationDict = findAndSaveRelationDict(workspaceId, chunk, relationDict);

            // Relation Instance 생성 (없으면)
            // findAndSaveRelationType(workspaceId, relationDict);

            // 4. Triple 저장
            Optional<OntologyKnowlearnType> existing = knowlearnTypeRepository
                    .findByWorkspaceIdAndSubjectIdAndRelationIdAndObjectId(
                            workspaceId, subjectDict.getId(), relationDict.getId(), objectDict.getId());

            if (existing.isEmpty()) {
                OntologyKnowlearnType triple = OntologyKnowlearnType.builder()
                        .workspaceId(workspaceId)
                        .subjectId(subjectDict.getId())
                        .relationId(relationDict.getId())
                        .objectId(objectDict.getId())
                        .confidenceScore(dto.getConfidenceScore())
                        .evidenceLevel(dto.getEvidenceLevel())
                        .source(addSource(null, String.valueOf(chunk.getDocument().getId())))
                        .build();

                knowlearnTypeRepository.save(triple);
            } else {
                OntologyKnowlearnType triple = existing.get();
                triple.setSource(addSource(triple.getSource(), String.valueOf(chunk.getDocument().getId())));
            }

        } catch (Exception e) {
            log.error("KnowlearnType 저장 실패: {} - {} - {}",
                    dto.getSubjectTermEn(), dto.getRelationEn(), dto.getObjectTermEn(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeDocumentSource(Long documentId) {
        String docIdStr = String.valueOf(documentId);
        // Pattern to find JSON arrays containing "docId"
        String pattern = "%\"" + docIdStr + "\"%";

        // 1. Clean Triples
        List<OntologyKnowlearnType> triples = knowlearnTypeRepository.findBySourceContaining(pattern);
        for (OntologyKnowlearnType triple : triples) {
            String newSource = removeSourceId(triple.getSource(), docIdStr);
            if (isSourceEmpty(newSource)) {
                knowlearnTypeRepository.delete(triple);
            } else {
                triple.setSource(newSource);
                knowlearnTypeRepository.save(triple);
            }
        }

        // 2. Clean Objects
        List<OntologyObjectDict> objects = objectDictRepository.findBySourceContaining(pattern);
        for (OntologyObjectDict obj : objects) {
            String newSource = removeSourceId(obj.getSource(), docIdStr);
            if (isSourceEmpty(newSource)) {
                objectDictRepository.delete(obj);
            } else {
                obj.setSource(newSource);
                objectDictRepository.save(obj);
            }
        }

        // 3. Clean Relations
        List<OntologyRelationDict> relations = relationDictRepository.findBySourceContaining(pattern);
        for (OntologyRelationDict rel : relations) {
            String newSource = removeSourceId(rel.getSource(), docIdStr);
            if (isSourceEmpty(newSource)) {
                relationDictRepository.delete(rel);
            } else {
                rel.setSource(newSource);
                relationDictRepository.save(rel);
            }
        }
    }

    private String removeSourceId(String currentSource, String targetId) {
        try {
            if (currentSource == null || currentSource.isEmpty())
                return "[]";

            java.util.List<String> list;
            try {
                list = objectMapper.readValue(currentSource,
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                        });
            } catch (Exception e) {
                // Not a JSON list? If it matches targetId exact (legacy), return empty
                if (currentSource.trim().equals(targetId))
                    return "[]";
                // Else return as is
                return currentSource;
            }

            if (list == null)
                list = new java.util.ArrayList<>();
            else
                list = new java.util.ArrayList<>(list);

            list.remove(targetId);
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Error removing source ID: {}", targetId, e);
            return currentSource;
        }
    }

    private boolean isSourceEmpty(String sourceJson) {
        try {
            if (sourceJson == null || sourceJson.isEmpty() || "[]".equals(sourceJson.trim()))
                return true;
            java.util.List<String> list = objectMapper.readValue(sourceJson,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {
                    });
            return list == null || list.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
