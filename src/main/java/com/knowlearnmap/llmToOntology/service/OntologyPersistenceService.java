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

    @Lazy
    @Autowired
    private OntologyPersistenceService self;

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
        String category = this.SpaceRemover(dict.getCategory());
        String termEn = this.SpaceRemover(dict.getTermEn());
        String termKo = this.SpaceRemover(dict.getTermKo());
        try {
            // 영문 용어로 먼저 검색
            Optional<OntologyObjectDict> existing = objectDictRepository
                    .findByWorkspaceIdAndCategoryAndTermEn(workspaceId, category, dict.getTermEn());
            if (existing.isPresent()) {
                return existing.get();
            }

            Optional<OntologyObjectSynonyms> existingSym = objectSynonymsRepository
                    .findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, dict.getTermEn());
            if (existingSym.isPresent()) {
                existing = objectDictRepository.findById(existingSym.get().getObjectId());
                return existing.get();
            }

            if (!(dict.getTermEn().equals(termEn))) {
                existing = objectDictRepository
                        .findByWorkspaceIdAndCategoryAndTermEn(workspaceId, category, termEn);
                if (existing.isPresent()) {
                    return existing.get();
                }

                existingSym = objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                        termEn);
                if (existingSym.isPresent()) {
                    existing = objectDictRepository.findById(existingSym.get().getObjectId());
                    return existing.get();
                }
            }

            // 한글 용어로 검색
            existing = objectDictRepository
                    .findByWorkspaceIdAndCategoryAndTermKo(workspaceId, category, dict.getTermKo());
            if (existing.isPresent()) {
                return existing.get();
            }

            existingSym = objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                    dict.getTermKo());
            if (existingSym.isPresent()) {
                existing = objectDictRepository.findById(existingSym.get().getObjectId());
                return existing.get();
            }

            if (!(dict.getTermKo().equals(termKo))) {
                existing = objectDictRepository
                        .findByWorkspaceIdAndCategoryAndTermKo(workspaceId, category, termKo);
                if (existing.isPresent()) {
                    return existing.get();
                }

                existingSym = objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                        termKo);
                if (existingSym.isPresent()) {
                    existing = objectDictRepository.findById(existingSym.get().getObjectId());
                    return existing.get();
                }
            }

            // 없으면 새로 저장
            dict.setWorkspaceId(workspaceId);
            dict.setCategory(category);
            OntologyObjectDict newDict = objectDictRepository.save(dict);

            // 띄어쓰기 제거해서 Sym 에 추가
            self.saveObjectSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getTermEn(), termEn, "en");
            self.saveObjectSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getTermKo(), termKo, "ko");

            return newDict;
        } catch (Exception e) {
            log.error("ObjectDict 저장 실패: {}", dict.getTermEn(), e);
            // 동시성 문제로 이미 저장되었을 수 있으므로 다시 조회 시도
            return objectDictRepository
                    .findByWorkspaceIdAndCategoryAndTermEn(workspaceId, dict.getCategory(), dict.getTermEn())
                    .orElseThrow(() -> new RuntimeException("ObjectDict 저장 및 조회 실패", e));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveObjectSynonymIfNecessary(Long workspaceId, String category, Long objectId, String originalTerm,
            String normalizedTerm, String language) {
        if (!(originalTerm.equals(normalizedTerm))) {
            try {
                if (objectSynonymsRepository
                        .findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, normalizedTerm).isEmpty()) {
                    OntologyObjectSynonyms sym = new OntologyObjectSynonyms();
                    sym.setWorkspaceId(workspaceId);
                    sym.setCategory(category);
                    sym.setSynonym(normalizedTerm);
                    sym.setObjectId(objectId);
                    sym.setLanguage(language);
                    objectSynonymsRepository.save(sym);
                }
            } catch (Exception e) {
                // 중복 저장 등 실패하더라도 전체 로직에는 영향 주지 않음 (로그만 기록)
                log.warn("Object Synonym 저장 실패 (무시됨): {} - {}", normalizedTerm, e.getMessage());
            }
        }
    }

    /**
     * Relation Dictionary 저장 또는 조회
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OntologyRelationDict findAndSaveRelationDict(Long workspaceId, DocumentChunk chunk,
            OntologyRelationDict dict) {
        String category = this.SpaceRemover(dict.getCategory());
        String relationEn = this.SpaceRemover(dict.getRelationEn());
        String relationKo = this.SpaceRemover(dict.getRelationKo());
        try {
            // 영문 용어로 먼저 검색
            Optional<OntologyRelationDict> existing = relationDictRepository
                    .findByWorkspaceIdAndCategoryAndRelationEn(workspaceId, category, dict.getRelationEn());
            if (existing.isPresent()) {
                return existing.get();
            }

            Optional<OntologyRelationSynonyms> existingSym = relationSynonymsRepository
                    .findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, dict.getRelationEn());
            if (existingSym.isPresent()) {
                existing = relationDictRepository.findById(existingSym.get().getRelationId());
                return existing.get();
            }

            if (!(dict.getRelationEn().equals(relationEn))) {
                existing = relationDictRepository
                        .findByWorkspaceIdAndCategoryAndRelationEn(workspaceId, category, relationEn);
                if (existing.isPresent()) {
                    return existing.get();
                }

                existingSym = relationSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                        relationEn);
                if (existingSym.isPresent()) {
                    existing = relationDictRepository.findById(existingSym.get().getRelationId());
                    return existing.get();
                }
            }

            // 한글 용어로 검색
            existing = relationDictRepository
                    .findByWorkspaceIdAndCategoryAndRelationKo(workspaceId, category, dict.getRelationKo());
            if (existing.isPresent()) {
                return existing.get();
            }

            existingSym = relationSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                    dict.getRelationKo());
            if (existingSym.isPresent()) {
                existing = relationDictRepository.findById(existingSym.get().getRelationId());
                return existing.get();
            }

            if (!(dict.getRelationKo().equals(relationKo))) {
                existing = relationDictRepository
                        .findByWorkspaceIdAndCategoryAndRelationKo(workspaceId, category, relationKo);
                if (existing.isPresent()) {
                    return existing.get();
                }

                existingSym = relationSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                        relationKo);
                if (existingSym.isPresent()) {
                    existing = relationDictRepository.findById(existingSym.get().getRelationId());
                    return existing.get();
                }
            }

            // 없으면 새로 저장
            dict.setWorkspaceId(workspaceId);
            dict.setCategory(category);
            OntologyRelationDict newDict = relationDictRepository.save(dict);

            // 띠어쓰기 제거해서 Sym 에 추가
            self.saveRelationSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getRelationEn(),
                    relationEn, "en");
            self.saveRelationSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getRelationKo(),
                    relationKo, "ko");

            return newDict;

        } catch (Exception e) {
            log.error("RelationDict 저장 실패: {}", dict.getRelationEn(), e);
            return relationDictRepository
                    .findByWorkspaceIdAndCategoryAndRelationEn(workspaceId, dict.getCategory(), dict.getRelationEn())
                    .orElseThrow(() -> new RuntimeException("RelationDict 저장 및 조회 실패", e));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRelationSynonymIfNecessary(Long workspaceId, String category, Long relationId, String originalTerm,
            String normalizedTerm, String language) {
        if (!(originalTerm.equals(normalizedTerm))) {
            try {
                if (relationSynonymsRepository
                        .findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, normalizedTerm).isEmpty()) {
                    OntologyRelationSynonyms sym = new OntologyRelationSynonyms();
                    sym.setWorkspaceId(workspaceId);
                    sym.setCategory(category);
                    sym.setSynonym(normalizedTerm);
                    sym.setRelationId(relationId);
                    sym.setLanguage(language);
                    relationSynonymsRepository.save(sym);
                }
            } catch (Exception e) {
                log.warn("Relation Synonym 저장 실패 (무시됨): {} - {}", normalizedTerm, e.getMessage());
            }
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
                        .source("[\"" + chunk.getId() + "\"]")
                        .build();

                knowlearnTypeRepository.save(triple);
            } else {
                // 이미 존재하면 소스 업데이트 등 수행 가능
                OntologyKnowlearnType triple = existing.get();
                // 소스 업데이트 로직 (생략)
            }

        } catch (Exception e) {
            log.error("KnowlearnType 저장 실패: {} - {} - {}",
                    dto.getSubjectTermEn(), dto.getRelationEn(), dto.getObjectTermEn(), e);
        }
    }

    // private void findAndSaveObjectType(Long workspaceId, OntologyObjectDict dict)
    // {
    // if (objectTypeRepository.findByWorkspaceIdAndObjectDictId(workspaceId,
    // dict.getId()).isEmpty()) {
    // OntologyObjectType type = OntologyObjectType.builder()
    // .workspaceId(workspaceId)
    // .objectDictId(dict.getId())
    // .build();
    // objectTypeRepository.save(type);
    // }
    // }
    //
    // private void findAndSaveRelationType(Long workspaceId, OntologyRelationDict
    // dict) {
    // if (relationTypeRepository.findByWorkspaceIdAndRelationDictId(workspaceId,
    // dict.getId()).isEmpty()) {
    // OntologyRelationType type = OntologyRelationType.builder()
    // .workspaceId(workspaceId)
    // .relationDictId(dict.getId())
    // .build();
    // relationTypeRepository.save(type);
    // }
    // }
}

