package com.knowlearnmap.llmToOntology.service;

import com.knowlearnmap.llmToOntology.domain.*;
import com.knowlearnmap.llmToOntology.dto.DictionaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DictionaryService {

    private final OntologyObjectDictRepository objectDictRepository;
    private final OntologyRelationDictRepository relationDictRepository;
    private final OntologyObjectSynonymsRepository objectSynonymsRepository;
    private final OntologyRelationSynonymsRepository relationSynonymsRepository;
    private final OntologyKnowlearnTypeRepository knowlearnTypeRepository;

    // New Reference Repositories
    private final OntologyObjectReferenceRepository objectReferenceRepository;
    private final OntologyRelationReferenceRepository relationReferenceRepository;
    private final OntologyKnowlearnReferenceRepository knowlearnReferenceRepository;

    private final com.knowlearnmap.workspace.repository.WorkspaceRepository workspaceRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final com.knowlearnmap.member.repository.MemberRepository memberRepository;

    private void checkPermission(Long workspaceId, String username) {
        com.knowlearnmap.workspace.domain.WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        if (!workspace.getCreatedBy().equals(username)) {
            com.knowlearnmap.member.domain.Member member = memberRepository.findByEmail(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            if (member.getRole() != com.knowlearnmap.member.domain.Member.Role.ADMIN) {
                throw new IllegalArgumentException("Permission denied. Only workspace owner can modify dictionary.");
            }
        }
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DictionaryDto> getConcepts(Long workspaceId, List<Long> documentIds,
            org.springframework.data.domain.Pageable pageable) {
        if (documentIds == null || documentIds.isEmpty()) {
            // Efficient DB Pagination
            org.springframework.data.domain.Page<OntologyObjectDict> page = objectDictRepository
                    .findByWorkspaceId(workspaceId, pageable);
            return page.map(this::convertToDto);
        } else {
            // Database Filtering using Reference Tables
            org.springframework.data.domain.Page<OntologyObjectDict> page = objectDictRepository
                    .findByWorkspaceIdAndDocumentIds(workspaceId, documentIds, pageable);
            return page.map(this::convertToDto);
        }
    }

    private DictionaryDto convertToDto(OntologyObjectDict obj) {
        List<OntologyObjectSynonyms> synonyms = objectSynonymsRepository.findByObjectId(obj.getId());
        String synStr = synonyms.stream().map(OntologyObjectSynonyms::getSynonym).collect(Collectors.joining(", "));

        // Reconstruct source JSON for backward compatibility
        String sourceJson = "[]";
        try {
            List<Long> docIds = objectReferenceRepository.findDistinctDocumentIdByOntologyObjectDictId(obj.getId());
            sourceJson = objectMapper
                    .writeValueAsString(docIds.stream().map(String::valueOf).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error serializing source IDs for object {}", obj.getId(), e);
        }

        return DictionaryDto.builder()
                .id(obj.getId())
                .workspaceId(obj.getWorkspaceId())
                .label(obj.getTermKo())
                .labelEn(obj.getTermEn())
                .category(obj.getCategory())
                .description(obj.getDescription())
                .status(obj.getStatus())
                .source(sourceJson)
                .synonym(synStr)
                .type("concept")
                .build();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DictionaryDto> getRelations(Long workspaceId, List<Long> documentIds,
            org.springframework.data.domain.Pageable pageable) {
        if (documentIds == null || documentIds.isEmpty()) {
            // Efficient DB Pagination
            org.springframework.data.domain.Page<OntologyRelationDict> page = relationDictRepository
                    .findByWorkspaceId(workspaceId, pageable);
            return page.map(this::convertRelToDto);
        } else {
            // Database Filtering using Reference Tables
            org.springframework.data.domain.Page<OntologyRelationDict> page = relationDictRepository
                    .findByWorkspaceIdAndDocumentIds(workspaceId, documentIds, pageable);
            return page.map(this::convertRelToDto);
        }
    }

    private DictionaryDto convertRelToDto(OntologyRelationDict rel) {
        List<OntologyRelationSynonyms> synonyms = relationSynonymsRepository.findByRelationId(rel.getId());
        String synStr = synonyms.stream().map(OntologyRelationSynonyms::getSynonym)
                .collect(Collectors.joining(", "));

        // Reconstruct source JSON for backward compatibility
        String sourceJson = "[]";
        try {
            List<Long> docIds = relationReferenceRepository.findDistinctDocumentIdByOntologyRelationDictId(rel.getId());
            sourceJson = objectMapper
                    .writeValueAsString(docIds.stream().map(String::valueOf).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Error serializing source IDs for relation {}", rel.getId(), e);
        }

        return DictionaryDto.builder()
                .id(rel.getId())
                .workspaceId(rel.getWorkspaceId())
                .label(rel.getRelationKo())
                .labelEn(rel.getRelationEn())
                .category(rel.getCategory())
                .description(rel.getDescription())
                .status(rel.getStatus())
                .source(sourceJson)
                .synonym(synStr)
                .type("relation")
                .build();
    }

    public DictionaryDto updateConcept(Long id, DictionaryDto dto, String username) {
        OntologyObjectDict concept = objectDictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concept not found: " + id));

        checkPermission(concept.getWorkspaceId(), username);

        // Validation: Check for duplicates
        validateUniqueConcept(concept.getWorkspaceId(), concept.getCategory(), dto.getLabelEn(), dto.getLabel(), id);

        concept.setTermKo(dto.getLabel());
        concept.setTermEn(dto.getLabelEn());
        concept.setDescription(dto.getDescription());

        OntologyObjectDict saved = objectDictRepository.save(concept);

        // Update Sync Status
        markWorkspaceSyncNeeded(concept.getWorkspaceId());

        return dto;
    }

    public DictionaryDto updateRelation(Long id, DictionaryDto dto, String username) {
        OntologyRelationDict relation = relationDictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Relation not found: " + id));

        checkPermission(relation.getWorkspaceId(), username);

        // Note: Relation validation can be added here similarly if needed, currently
        // focusing on Concept as per request.

        relation.setRelationKo(dto.getLabel());
        relation.setRelationEn(dto.getLabelEn());
        relation.setDescription(dto.getDescription());

        OntologyRelationDict saved = relationDictRepository.save(relation);

        // Update Sync Status
        markWorkspaceSyncNeeded(relation.getWorkspaceId());

        return dto;
    }

    private void validateUniqueConcept(Long workspaceId, String category, String termEn, String termKo,
            Long excludeId) {
        // 1. Check Object Dict (Term EN)
        objectDictRepository.findByWorkspaceIdAndCategoryAndTermEn(workspaceId, category, termEn)
                .ifPresent(dup -> {
                    if (excludeId == null || !dup.getId().equals(excludeId)) {
                        throw new IllegalArgumentException("이미 존재하는 영문 용어입니다: " + termEn);
                    }
                });

        // 2. Check Object Dict (Term KO)
        objectDictRepository.findByWorkspaceIdAndCategoryAndTermKo(workspaceId, category, termKo)
                .ifPresent(dup -> {
                    if (excludeId == null || !dup.getId().equals(excludeId)) {
                        throw new IllegalArgumentException("이미 존재하는 한글 용어입니다: " + termKo);
                    }
                });

        // 3. Check Synonyms (Term EN)
        objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, termEn)
                .ifPresent(dup -> {
                    throw new IllegalArgumentException("이미 동의어로 존재하는 용어입니다 (영문): " + termEn);
                });

        // 4. Check Synonyms (Term KO)
        objectSynonymsRepository.findByWorkspaceIdAndCategoryAndSynonym(workspaceId, category, termKo)
                .ifPresent(dup -> {
                    throw new IllegalArgumentException("이미 동의어로 존재하는 용어입니다 (한글): " + termKo);
                });
    }

    public void deleteConcept(Long id, String username) {
        // Fetch ID before delete to get workspace
        OntologyObjectDict obj = objectDictRepository.findById(id).orElse(null);
        if (obj != null) {
            checkPermission(obj.getWorkspaceId(), username);
            Long wsId = obj.getWorkspaceId();

            // 1. Delete Triples using this concept (Subject or Object)
            List<OntologyKnowlearnType> triples = knowlearnTypeRepository.findByWorkspaceIdAndSubjectIdOrObjectId(wsId,
                    id, id);
            for (OntologyKnowlearnType triple : triples) {
                // Delete Triple References
                knowlearnReferenceRepository.deleteByOntologyKnowlearnType(triple);
                // Delete Triple
                knowlearnTypeRepository.delete(triple);
            }

            // 2. Delete Synonyms
            objectSynonymsRepository.deleteByObjectId(id);

            // 3. Delete Document References
            objectReferenceRepository.deleteByOntologyObjectDictId(id);

            // 4. Delete Concept
            objectDictRepository.deleteById(id);

            markWorkspaceSyncNeeded(wsId);
        }
    }

    public void deleteRelation(Long id, String username) {
        OntologyRelationDict rel = relationDictRepository.findById(id).orElse(null);
        if (rel != null) {
            checkPermission(rel.getWorkspaceId(), username);
            Long wsId = rel.getWorkspaceId();

            // 1. Delete Triples using this relation
            List<OntologyKnowlearnType> triples = knowlearnTypeRepository.findByWorkspaceIdAndRelationId(wsId, id);
            for (OntologyKnowlearnType triple : triples) {
                // Delete Triple References
                knowlearnReferenceRepository.deleteByOntologyKnowlearnType(triple);
                // Delete Triple
                knowlearnTypeRepository.delete(triple);
            }

            // 2. Delete Synonyms
            relationSynonymsRepository.deleteByRelationId(id);

            // 3. Delete Document References
            relationReferenceRepository.deleteByOntologyRelationDictId(id);

            // 4. Delete Relation
            relationDictRepository.deleteById(id);

            markWorkspaceSyncNeeded(wsId);
        }
    }

    @Transactional(readOnly = true)
    public List<String> getConceptCategories(Long workspaceId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return objectDictRepository.findDistinctCategoriesByWorkspaceId(workspaceId);
        }
        return objectDictRepository.findDistinctCategoriesByWorkspaceIdAndDocumentIds(workspaceId, documentIds);
    }

    @Transactional(readOnly = true)
    public List<String> getRelationCategories(Long workspaceId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return relationDictRepository.findDistinctCategoriesByWorkspaceId(workspaceId);
        }
        return relationDictRepository.findDistinctCategoriesByWorkspaceIdAndDocumentIds(workspaceId, documentIds);
    }

    @Transactional
    public void mergeConcepts(Long sourceId, Long targetId, Long workspaceId, boolean keepSourceAsSynonym,
            String username) {
        checkPermission(workspaceId, username);
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot merge a concept into itself.");
        }

        OntologyObjectDict sourceConcept = objectDictRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source concept not found: " + sourceId));
        OntologyObjectDict targetConcept = objectDictRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Target concept not found: " + targetId));

        // 1. Merge Document References
        mergeDocumentReferences(sourceConcept, targetConcept);

        // 2a. Add Source Name as Synonym to Target (Conditional: MOVE vs MERGE)
        if (keepSourceAsSynonym) {
            addAsSynonym(sourceConcept, targetConcept, workspaceId);
        }

        // 2b. Merge Existing Synonyms
        List<OntologyObjectSynonyms> sourceSynonyms = objectSynonymsRepository.findByObjectId(sourceId);
        for (OntologyObjectSynonyms syn : sourceSynonyms) {
            // Check for duplicate in target
            boolean exists = objectSynonymsRepository.findByObjectId(targetId).stream()
                    .anyMatch(ts -> ts.getSynonym().equalsIgnoreCase(syn.getSynonym()));

            if (!exists) {
                syn.setObjectId(targetId);
                objectSynonymsRepository.save(syn);
            } else {
                objectSynonymsRepository.delete(syn);
            }
        }

        // 3. Relink Triples (As Subject)
        List<OntologyKnowlearnType> sourceAsSubject = knowlearnTypeRepository.findByWorkspaceIdAndSubjectId(workspaceId,
                sourceId);
        for (OntologyKnowlearnType triple : sourceAsSubject) {
            Optional<OntologyKnowlearnType> duplicate = knowlearnTypeRepository
                    .findByWorkspaceIdAndSubjectIdAndRelationIdAndObjectId(
                            workspaceId, targetId, triple.getRelationId(), triple.getObjectId());

            if (duplicate.isPresent()) {
                // Safe Merge: Move references from source triple to target triple before
                // deleting source
                mergeTripleReferences(triple, duplicate.get());
                knowlearnTypeRepository.delete(triple);
            } else {
                triple.setSubjectId(targetId);
                knowlearnTypeRepository.save(triple);
            }
        }

        // 4. Relink Triples (As Object)
        List<OntologyKnowlearnType> sourceAsObject = knowlearnTypeRepository.findByWorkspaceIdAndObjectId(workspaceId,
                sourceId);
        for (OntologyKnowlearnType triple : sourceAsObject) {
            Optional<OntologyKnowlearnType> duplicate = knowlearnTypeRepository
                    .findByWorkspaceIdAndSubjectIdAndRelationIdAndObjectId(
                            workspaceId, triple.getSubjectId(), triple.getRelationId(), targetId);

            if (duplicate.isPresent()) {
                // Safe Merge: Move references from source triple to target triple before
                // deleting source
                mergeTripleReferences(triple, duplicate.get());
                knowlearnTypeRepository.delete(triple);
            } else {
                triple.setObjectId(targetId);
                knowlearnTypeRepository.save(triple);
            }
        }

        // 5. Delete Source Concept
        objectDictRepository.delete(sourceConcept);

        markWorkspaceSyncNeeded(workspaceId);
    }

    private void mergeDocumentReferences(OntologyObjectDict source, OntologyObjectDict target) {
        List<OntologyObjectReference> sourceRefs = objectReferenceRepository.findByOntologyObjectDictId(source.getId());
        for (OntologyObjectReference sourceRef : sourceRefs) {
            boolean exists = objectReferenceRepository.existsByOntologyObjectDictAndDocumentIdAndChunkId(
                    target, sourceRef.getDocumentId(), sourceRef.getChunkId());

            if (exists) {
                objectReferenceRepository.delete(sourceRef);
            } else {
                sourceRef.setOntologyObjectDict(target);
                objectReferenceRepository.save(sourceRef);
            }
        }
    }

    private void addAsSynonym(OntologyObjectDict source, OntologyObjectDict target, Long workspaceId) {
        String sourceName = source.getTermKo();
        // Check if sourceName is already a synonym of target
        boolean alreadySynonym = objectSynonymsRepository.findByObjectId(target.getId()).stream()
                .anyMatch(s -> s.getSynonym().equalsIgnoreCase(sourceName));

        // Check if sourceName is same as targetName
        boolean sameName = sourceName.equalsIgnoreCase(target.getTermKo());

        if (!alreadySynonym && !sameName) {
            OntologyObjectSynonyms newSynonym = OntologyObjectSynonyms.builder()
                    .workspaceId(workspaceId)
                    .category(target.getCategory())
                    .synonym(sourceName)
                    .objectId(target.getId())
                    .language("ko") // Defaulting to KO as per context
                    .status("active")
                    .build();
            objectSynonymsRepository.save(newSynonym);
        }

        // Optionally do the same for English term if needed
        if (source.getTermEn() != null && !source.getTermEn().isEmpty()) {
            String sourceNameEn = source.getTermEn();
            boolean alreadySynonymEn = objectSynonymsRepository.findByObjectId(target.getId()).stream()
                    .anyMatch(s -> s.getSynonym().equalsIgnoreCase(sourceNameEn));
            boolean sameNameEn = sourceNameEn.equalsIgnoreCase(target.getTermEn());

            if (!alreadySynonymEn && !sameNameEn) {
                OntologyObjectSynonyms newSynonymEn = OntologyObjectSynonyms.builder()
                        .workspaceId(workspaceId)
                        .category(target.getCategory())
                        .synonym(sourceNameEn)
                        .objectId(target.getId())
                        .language("en")
                        .status("active")
                        .build();
                objectSynonymsRepository.save(newSynonymEn);
            }
        }
    }

    @Transactional
    public void mergeRelations(Long sourceId, Long targetId, Long workspaceId, String username) {
        checkPermission(workspaceId, username);
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot merge a relation into itself.");
        }

        OntologyRelationDict sourceRelation = relationDictRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source relation not found: " + sourceId));
        OntologyRelationDict targetRelation = relationDictRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Target relation not found: " + targetId));

        // 1. Merge Document References
        mergeRelationReferences(sourceRelation, targetRelation);

        // 2. Merge Synonyms
        List<OntologyRelationSynonyms> sourceSynonyms = relationSynonymsRepository.findByRelationId(sourceId);
        for (OntologyRelationSynonyms syn : sourceSynonyms) {
            boolean exists = relationSynonymsRepository.findByRelationId(targetId).stream()
                    .anyMatch(ts -> ts.getSynonym().equalsIgnoreCase(syn.getSynonym()));

            if (!exists) {
                syn.setRelationId(targetId);
                relationSynonymsRepository.save(syn);
            } else {
                relationSynonymsRepository.delete(syn);
            }
        }

        // Add Source Name as Synonym to Target
        addRelationAsSynonym(sourceRelation, targetRelation, workspaceId);

        // 3. Update Triples using this relation
        List<OntologyKnowlearnType> triplesUsingSource = knowlearnTypeRepository
                .findByWorkspaceIdAndRelationId(workspaceId, sourceId);
        for (OntologyKnowlearnType triple : triplesUsingSource) {
            Optional<OntologyKnowlearnType> duplicate = knowlearnTypeRepository
                    .findByWorkspaceIdAndSubjectIdAndRelationIdAndObjectId(
                            workspaceId, triple.getSubjectId(), targetId, triple.getObjectId());

            if (duplicate.isPresent()) {
                // Safe Merge References
                mergeTripleReferences(triple, duplicate.get());
                knowlearnTypeRepository.delete(triple);
            } else {
                triple.setRelationId(targetId);
                knowlearnTypeRepository.save(triple);
            }
        }

        // 4. Delete Source Relation
        relationDictRepository.delete(sourceRelation);

        markWorkspaceSyncNeeded(workspaceId);
    }

    private void mergeRelationReferences(OntologyRelationDict source, OntologyRelationDict target) {
        List<OntologyRelationReference> sourceRefs = relationReferenceRepository
                .findByOntologyRelationDictId(source.getId());
        for (OntologyRelationReference sourceRef : sourceRefs) {
            boolean exists = relationReferenceRepository.existsByOntologyRelationDictAndDocumentIdAndChunkId(
                    target, sourceRef.getDocumentId(), sourceRef.getChunkId());

            if (exists) {
                relationReferenceRepository.delete(sourceRef);
            } else {
                sourceRef.setOntologyRelationDict(target);
                relationReferenceRepository.save(sourceRef);
            }
        }
    }

    private void addRelationAsSynonym(OntologyRelationDict source, OntologyRelationDict target, Long workspaceId) {
        String sourceName = source.getRelationKo();
        boolean alreadySynonym = relationSynonymsRepository.findByRelationId(target.getId()).stream()
                .anyMatch(s -> s.getSynonym().equalsIgnoreCase(sourceName));
        boolean sameName = sourceName.equalsIgnoreCase(target.getRelationKo());

        if (!alreadySynonym && !sameName) {
            OntologyRelationSynonyms newSynonym = OntologyRelationSynonyms.builder()
                    .workspaceId(workspaceId)
                    .category(target.getCategory())
                    .synonym(sourceName)
                    .relationId(target.getId())
                    .language("ko")
                    .status("active")
                    .build();
            relationSynonymsRepository.save(newSynonym);
        }

        if (source.getRelationEn() != null && !source.getRelationEn().isEmpty()) {
            String sourceNameEn = source.getRelationEn();
            boolean alreadySynonymEn = relationSynonymsRepository.findByRelationId(target.getId()).stream()
                    .anyMatch(s -> s.getSynonym().equalsIgnoreCase(sourceNameEn));
            boolean sameNameEn = sourceNameEn.equalsIgnoreCase(target.getRelationEn());

            if (!alreadySynonymEn && !sameNameEn) {
                OntologyRelationSynonyms newSynonymEn = OntologyRelationSynonyms.builder()
                        .workspaceId(workspaceId)
                        .category(target.getCategory())
                        .synonym(sourceNameEn)
                        .relationId(target.getId())
                        .language("en")
                        .status("active")
                        .build();
                relationSynonymsRepository.save(newSynonymEn);
            }
        }
    }

    private void mergeTripleReferences(OntologyKnowlearnType sourceTriple, OntologyKnowlearnType targetTriple) {
        List<OntologyKnowlearnReference> sourceRefs = knowlearnReferenceRepository
                .findByOntologyKnowlearnType(sourceTriple);
        for (OntologyKnowlearnReference sourceRef : sourceRefs) {
            boolean exists = knowlearnReferenceRepository.existsByOntologyKnowlearnTypeAndDocumentIdAndChunkId(
                    targetTriple, sourceRef.getDocumentId(), sourceRef.getChunkId());

            if (exists) {
                knowlearnReferenceRepository.deleteByDocumentId(sourceRef.getDocumentId());
            } else {
                sourceRef.setOntologyKnowlearnType(targetTriple);
                knowlearnReferenceRepository.save(sourceRef);
            }
        }
    }

    private void markWorkspaceSyncNeeded(Long workspaceId) {
        com.knowlearnmap.workspace.domain.WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElse(null);
        if (workspace != null) {
            workspace.setNeedsArangoSync(true);
            workspaceRepository.save(workspace);
        }
    }
}
