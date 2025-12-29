package com.knowlearnmap.llmToOntology.service;

import com.knowlearnmap.llmToOntology.domain.*;
import com.knowlearnmap.llmToOntology.dto.OntologyDto;
import com.knowlearnmap.document.domain.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 정규화된 테이블 구조(Reference Tables)를 사용하여 Document 및 Chunk 참조를 관리합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OntologyPersistenceService {

    private final OntologyObjectDictRepository objectDictRepository;
    private final OntologyRelationDictRepository relationDictRepository;
    private final OntologyKnowlearnTypeRepository knowlearnTypeRepository;

    // New Reference Repositories (Must be created or inject EntityManager if
    // preferred, but usually Repos needed)
    // NOTE: You need to create these repositories first. I will assume they exist
    // or you will create them.
    // Since I cannot create repository files in this single step, I will use
    // EntityManager or assume custom implementation?
    // Wait, I should have created Repository interfaces. I missed that in the plan.
    // I will write the code assuming the repositories exist, and then I will create
    // the repository files in the next step.
    private final OntologyObjectReferenceRepository objectReferenceRepository;
    private final OntologyRelationReferenceRepository relationReferenceRepository;
    private final OntologyKnowlearnReferenceRepository knowlearnReferenceRepository;

    private final OntologyObjectSynonymsRepository objectSynonymsRepository;
    private final OntologyRelationSynonymsRepository relationSynonymsRepository;

    private final OntologySynonymService synonymService;

    public String SpaceRemover(String str) {
        if (str == null)
            return "";
        return str.replaceAll("\\s", "");
    }

    /**
     * Object Dictionary 저장 또는 조회
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OntologyObjectDict findAndSaveObjectDict(Long workspaceId, DocumentChunk chunk, OntologyObjectDict dict) {
        Long docId = chunk.getDocument().getId();
        Long chunkId = chunk.getId();
        String category = this.SpaceRemover(dict.getCategory());
        String termEn = this.SpaceRemover(dict.getTermEn());
        String termKo = this.SpaceRemover(dict.getTermKo());

        try {
            OntologyObjectDict foundDict = findExistingObjectDict(workspaceId, category, dict.getTermEn(),
                    dict.getTermKo());

            if (foundDict != null) {
                saveObjectReference(foundDict, docId, chunkId);
                return foundDict;
            }

            // 없으면 새로 저장
            dict.setWorkspaceId(workspaceId);
            dict.setCategory(category);

            // Ensure NOT NULL columns have values
            if (dict.getTermEn() == null || dict.getTermEn().trim().isEmpty()) {
                dict.setTermEn(termEn.isEmpty() ? "Unknown" : termEn);
            }
            if (dict.getTermKo() == null || dict.getTermKo().trim().isEmpty()) {
                dict.setTermKo(termKo.isEmpty() ? "Unknown" : termKo);
            }

            OntologyObjectDict newDict = objectDictRepository.save(dict);
            saveObjectReference(newDict, docId, chunkId);

            // 띄어쓰기 제거해서 Sym 에 추가
            synonymService.saveObjectSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getTermEn(),
                    termEn, "en");
            synonymService.saveObjectSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getTermKo(),
                    termKo, "ko");

            return newDict;
        } catch (Exception e) {
            log.error("ObjectDict 저장 실패: {}", dict.getTermEn(), e);
            // 동시성 문제로 이미 저장되었을 수 있으므로 다시 조회 시도
            // Re-attempt to find the existing dictionary if save failed due to concurrency
            OntologyObjectDict existing = findExistingObjectDict(workspaceId, category, dict.getTermEn(),
                    dict.getTermKo());
            if (existing != null) {
                saveObjectReference(existing, docId, chunkId);
                return existing;
            }
            throw new RuntimeException("ObjectDict 저장 및 조회 실패", e);
        }
    }

    private OntologyObjectDict findExistingObjectDict(Long workspaceId, String category, String termEn, String termKo) {
        // 1. TermEn Search
        Optional<OntologyObjectDict> existing = objectDictRepository
                .findByWorkspaceIdAndCategoryAndTermEn(workspaceId, category, termEn);
        if (existing.isPresent())
            return existing.get();

        // 2. SynEn Search
        Optional<OntologyObjectSynonyms> existingSym = objectSynonymsRepository
                .findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, termEn);
        if (existingSym.isPresent()) {
            return objectDictRepository.findById(existingSym.get().getObjectId()).orElse(null);
        }

        String safeTermEn = SpaceRemover(termEn);
        if (!safeTermEn.equals(termEn)) {
            existing = objectDictRepository.findByWorkspaceIdAndCategoryAndTermEn(workspaceId, category, safeTermEn);
            if (existing.isPresent())
                return existing.get();

            existingSym = objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                    safeTermEn);
            if (existingSym.isPresent()) {
                return objectDictRepository.findById(existingSym.get().getObjectId()).orElse(null);
            }
        }

        // 3. TermKo Search
        existing = objectDictRepository.findByWorkspaceIdAndCategoryAndTermKo(workspaceId, category, termKo);
        if (existing.isPresent())
            return existing.get();

        // 4. SynKo Search
        existingSym = objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, termKo);
        if (existingSym.isPresent()) {
            return objectDictRepository.findById(existingSym.get().getObjectId()).orElse(null);
        }

        String safeTermKo = SpaceRemover(termKo);
        if (!safeTermKo.equals(termKo)) {
            existing = objectDictRepository.findByWorkspaceIdAndCategoryAndTermKo(workspaceId, category, safeTermKo);
            if (existing.isPresent())
                return existing.get();

            existingSym = objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                    safeTermKo);
            if (existingSym.isPresent()) {
                return objectDictRepository.findById(existingSym.get().getObjectId()).orElse(null);
            }
        }

        return null;
    }

    private void saveObjectReference(OntologyObjectDict dict, Long docId, Long chunkId) {
        // Check for duplicate reference to avoid unique constraint violations if we had
        // one
        // Ideally we should just save. If index exists, use ignore or check.
        // Assuming we check first or just save. Simple check:
        if (!objectReferenceRepository.existsByOntologyObjectDictAndDocumentIdAndChunkId(dict, docId, chunkId)) {
            OntologyObjectReference ref = OntologyObjectReference.builder()
                    .ontologyObjectDict(dict)
                    .documentId(docId)
                    .chunkId(chunkId)
                    .build();
            objectReferenceRepository.save(ref);
        }
    }

    /**
     * Relation Dictionary 저장 또는 조회
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OntologyRelationDict findAndSaveRelationDict(Long workspaceId, DocumentChunk chunk,
            OntologyRelationDict dict) {
        Long docId = chunk.getDocument().getId();
        Long chunkId = chunk.getId();
        String category = this.SpaceRemover(dict.getCategory());
        String relationEn = this.SpaceRemover(dict.getRelationEn());
        String relationKo = this.SpaceRemover(dict.getRelationKo());

        try {
            OntologyRelationDict foundDict = findExistingRelationDict(workspaceId, category, dict.getRelationEn(),
                    dict.getRelationKo());

            if (foundDict != null) {
                saveRelationReference(foundDict, docId, chunkId);
                return foundDict;
            }

            // 없으면 새로 저장
            dict.setWorkspaceId(workspaceId);
            dict.setCategory(category);

            // Ensure NOT NULL columns have values
            if (dict.getRelationEn() == null || dict.getRelationEn().trim().isEmpty()) {
                dict.setRelationEn(relationEn.isEmpty() ? "Unknown" : relationEn);
            }
            if (dict.getRelationKo() == null || dict.getRelationKo().trim().isEmpty()) {
                dict.setRelationKo(relationKo.isEmpty() ? "Unknown" : relationKo);
            }

            OntologyRelationDict newDict = relationDictRepository.save(dict);
            saveRelationReference(newDict, docId, chunkId);

            // 띠어쓰기 제거해서 Sym 에 추가
            synonymService.saveRelationSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getRelationEn(),
                    relationEn, "en");
            synonymService.saveRelationSynonymIfNecessary(workspaceId, category, newDict.getId(), dict.getRelationKo(),
                    relationKo, "ko");

            return newDict;

        } catch (Exception e) {
            log.error("RelationDict 저장 실패: {}", dict.getRelationEn(), e);
            // Re-attempt to find the existing dictionary if save failed due to concurrency
            OntologyRelationDict existing = findExistingRelationDict(workspaceId, category, dict.getRelationEn(),
                    dict.getRelationKo());
            if (existing != null) {
                saveRelationReference(existing, docId, chunkId);
                return existing;
            }
            throw new RuntimeException("RelationDict 저장 및 조회 실패", e);
        }
    }

    private OntologyRelationDict findExistingRelationDict(Long workspaceId, String category, String relationEn,
            String relationKo) {
        // Similar Logic to ObjectDict but for Relations
        Optional<OntologyRelationDict> existing = relationDictRepository
                .findByWorkspaceIdAndCategoryAndRelationEn(workspaceId, category, relationEn);
        if (existing.isPresent())
            return existing.get();

        Optional<OntologyRelationSynonyms> existingSym = relationSynonymsRepository
                .findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, relationEn);
        if (existingSym.isPresent()) {
            return relationDictRepository.findById(existingSym.get().getRelationId()).orElse(null);
        }

        String safeRelEn = SpaceRemover(relationEn);
        if (!safeRelEn.equals(relationEn)) {
            existing = relationDictRepository.findByWorkspaceIdAndCategoryAndRelationEn(workspaceId, category,
                    safeRelEn);
            if (existing.isPresent())
                return existing.get();

            existingSym = relationSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                    safeRelEn);
            if (existingSym.isPresent()) {
                return relationDictRepository.findById(existingSym.get().getRelationId()).orElse(null);
            }
        }

        existing = relationDictRepository.findByWorkspaceIdAndCategoryAndRelationKo(workspaceId, category, relationKo);
        if (existing.isPresent())
            return existing.get();

        existingSym = relationSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                relationKo);
        if (existingSym.isPresent()) {
            return relationDictRepository.findById(existingSym.get().getRelationId()).orElse(null);
        }

        String safeRelKo = SpaceRemover(relationKo);
        if (!safeRelKo.equals(relationKo)) {
            existing = relationDictRepository.findByWorkspaceIdAndCategoryAndRelationKo(workspaceId, category,
                    safeRelKo);
            if (existing.isPresent())
                return existing.get();

            existingSym = relationSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category,
                    safeRelKo);
            if (existingSym.isPresent()) {
                return relationDictRepository.findById(existingSym.get().getRelationId()).orElse(null);
            }
        }

        return null;
    }

    private void saveRelationReference(OntologyRelationDict dict, Long docId, Long chunkId) {
        if (!relationReferenceRepository.existsByOntologyRelationDictAndDocumentIdAndChunkId(dict, docId, chunkId)) {
            OntologyRelationReference ref = OntologyRelationReference.builder()
                    .ontologyRelationDict(dict)
                    .documentId(docId)
                    .chunkId(chunkId)
                    .build();
            relationReferenceRepository.save(ref);
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

            String subjectTermKo = dto.getSubjectTermKo();
            if (subjectTermKo == null || subjectTermKo.trim().isEmpty()) {
                subjectTermKo = dto.getSubjectTermEn();
            }
            if (subjectTermKo == null || subjectTermKo.trim().isEmpty()) {
                subjectTermKo = "Unknown";
            }
            subjectDict.setTermKo(subjectTermKo);

            subjectDict = findAndSaveObjectDict(workspaceId, chunk, subjectDict);

            // 2. Object 처리
            OntologyObjectDict objectDict = new OntologyObjectDict();
            objectDict.setCategory(dto.getObjectCategory());
            objectDict.setTermEn(dto.getObjectTermEn());

            String objectTermKo = dto.getObjectTermKo();
            if (objectTermKo == null || objectTermKo.trim().isEmpty()) {
                objectTermKo = dto.getObjectTermEn();
            }
            if (objectTermKo == null || objectTermKo.trim().isEmpty()) {
                objectTermKo = "Unknown";
            }
            objectDict.setTermKo(objectTermKo);

            objectDict = findAndSaveObjectDict(workspaceId, chunk, objectDict);

            // 3. Relation 처리
            OntologyRelationDict relationDict = new OntologyRelationDict();
            relationDict.setCategory(dto.getRelationCategory());
            relationDict.setRelationEn(dto.getRelationEn());
            relationDict.setRelationKo(dto.getRelationKo());
            relationDict = findAndSaveRelationDict(workspaceId, chunk, relationDict);

            // 4. Triple 저장
            Optional<OntologyKnowlearnType> existing = knowlearnTypeRepository
                    .findByWorkspaceIdAndSubjectIdAndRelationIdAndObjectId(
                            workspaceId, subjectDict.getId(), relationDict.getId(), objectDict.getId());

            OntologyKnowlearnType triple;
            if (existing.isEmpty()) {
                triple = OntologyKnowlearnType.builder()
                        .workspaceId(workspaceId)
                        .subjectId(subjectDict.getId())
                        .relationId(relationDict.getId())
                        .objectId(objectDict.getId())
                        .confidenceScore(dto.getConfidenceScore())
                        .evidenceLevel(dto.getEvidenceLevel())
                        .build();

                triple = knowlearnTypeRepository.save(triple);
            } else {
                triple = existing.get();
                // Update score/evidence if needed? For now we just keep existing.
            }

            saveKnowlearnReference(triple, chunk.getDocument().getId(), chunk.getId());

        } catch (Exception e) {
            log.error("KnowlearnType 저장 실패: {} - {} - {}",
                    dto.getSubjectTermEn(), dto.getRelationEn(), dto.getObjectTermEn(), e);
        }
    }

    private void saveKnowlearnReference(OntologyKnowlearnType triple, Long docId, Long chunkId) {
        if (!knowlearnReferenceRepository.existsByOntologyKnowlearnTypeAndDocumentIdAndChunkId(triple, docId,
                chunkId)) {
            OntologyKnowlearnReference ref = OntologyKnowlearnReference.builder()
                    .ontologyKnowlearnType(triple)
                    .documentId(docId)
                    .chunkId(chunkId)
                    .build();
            knowlearnReferenceRepository.save(ref);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeDocumentSource(Long documentId, List<Long> chunkIds) {
        // 1. Delete References by Document ID
        // Note: Using documentId is sufficient to remove all references for that
        // document.
        // ChunkIDs are technically redundant if we delete by DocumentID, BUT
        // if the user wants to delete specific chunks, we would use chunkIds.
        // Here we are deleting the entire document, so deleting by documentId is
        // correct and faster.

        long kRefDeleted = knowlearnReferenceRepository.deleteByDocumentId(documentId);
        long oRefDeleted = objectReferenceRepository.deleteByDocumentId(documentId);
        long rRefDeleted = relationReferenceRepository.deleteByDocumentId(documentId);

        log.info("Deleted References for Document {}: Triples={}, Objects={}, Relations={}",
                documentId, kRefDeleted, oRefDeleted, rRefDeleted);

        // 2. Cleanup Orphans (Masters with no References)
        // This can be heavy if strict consistency is needed instantly.
        // A common strategy is to do this asynchronously or periodically.
        // However, user logic requested "remove if no source remains".

        // Strategy: We can't easily find *which* objects were affected solely by this
        // delete without keeping a list.
        // But we know we just deleted references.
        // A simple brute-force cleanup for now: Delete where references is empty.
        // OR better: Delete only those that we know might be affected?
        // Let's implement a 'Delete Orphaned' query in the repository and call it.

        knowlearnTypeRepository.deleteOrphans();
        objectDictRepository.deleteOrphans();
        relationDictRepository.deleteOrphans();
    }
}
