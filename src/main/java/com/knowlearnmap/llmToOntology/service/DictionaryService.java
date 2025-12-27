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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<DictionaryDto> getConcepts(Long workspaceId, List<Long> documentIds,
            org.springframework.data.domain.Pageable pageable) {
        if (documentIds == null || documentIds.isEmpty()) {
            // Efficient DB Pagination
            org.springframework.data.domain.Page<OntologyObjectDict> page = objectDictRepository
                    .findByWorkspaceId(workspaceId, pageable);
            return page.map(obj -> convertToDto(obj));
        } else {
            // Filtered (In-memory Pagination for now)
            List<OntologyObjectDict> objects = objectDictRepository.findByWorkspaceId(workspaceId);
            List<OntologyObjectDict> filtered = objects.stream()
                    .filter(obj -> isSourceInDocuments(obj.getSource(), documentIds))
                    .collect(Collectors.toList());

            List<OntologyObjectDict> pagedList;
            if (pageable.isUnpaged()) {
                pagedList = filtered;
            } else {
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), filtered.size());
                if (start <= end) {
                    pagedList = filtered.subList(start, end);
                } else {
                    pagedList = new ArrayList<>();
                }
            }

            List<DictionaryDto> dtos = pagedList.stream().map(this::convertToDto).collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(dtos, pageable, filtered.size());
        }
    }

    private DictionaryDto convertToDto(OntologyObjectDict obj) {
        List<OntologyObjectSynonyms> synonyms = objectSynonymsRepository.findByObjectId(obj.getId());
        String synStr = synonyms.stream().map(OntologyObjectSynonyms::getSynonym).collect(Collectors.joining(", "));

        return DictionaryDto.builder()
                .id(obj.getId())
                .workspaceId(obj.getWorkspaceId())
                .label(obj.getTermKo())
                .labelEn(obj.getTermEn())
                .category(obj.getCategory())
                .description(obj.getDescription())
                .status(obj.getStatus())
                .source(obj.getSource())
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
            // Filtered
            List<OntologyRelationDict> relations = relationDictRepository.findByWorkspaceId(workspaceId);
            List<OntologyRelationDict> filtered = relations.stream()
                    .filter(rel -> isSourceInDocuments(rel.getSource(), documentIds))
                    .collect(Collectors.toList());

            List<OntologyRelationDict> pagedList;
            if (pageable.isUnpaged()) {
                pagedList = filtered;
            } else {
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), filtered.size());
                if (start <= end) {
                    pagedList = filtered.subList(start, end);
                } else {
                    pagedList = new ArrayList<>();
                }
            }

            List<DictionaryDto> dtos = pagedList.stream().map(this::convertRelToDto).collect(Collectors.toList());
            return new org.springframework.data.domain.PageImpl<>(dtos, pageable, filtered.size());
        }
    }

    private DictionaryDto convertRelToDto(OntologyRelationDict rel) {
        List<OntologyRelationSynonyms> synonyms = relationSynonymsRepository.findByRelationId(rel.getId());
        String synStr = synonyms.stream().map(OntologyRelationSynonyms::getSynonym)
                .collect(Collectors.joining(", "));

        return DictionaryDto.builder()
                .id(rel.getId())
                .workspaceId(rel.getWorkspaceId())
                .label(rel.getRelationKo())
                .labelEn(rel.getRelationEn())
                .category(rel.getCategory())
                .description(rel.getDescription())
                .status(rel.getStatus())
                .source(rel.getSource())
                .synonym(synStr)
                .type("relation")
                .build();
    }

    private boolean isSourceInDocuments(String sourceJson, List<Long> documentIds) {
        if (sourceJson == null || sourceJson.isEmpty() || sourceJson.equals("initial_data")) {
            return false;
        }
        try {
            List<String> sourceIds = objectMapper.readValue(sourceJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });
            for (String sId : sourceIds) {
                try {
                    Long id = Long.valueOf(sId);
                    if (documentIds.contains(id)) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // ignore non-numeric IDs
                }
            }
        } catch (Exception e) {
            // parsing error, treat as not matching
            return false;
        }
        return false;
    }

    public DictionaryDto updateConcept(Long id, DictionaryDto dto) {
        OntologyObjectDict concept = objectDictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concept not found: " + id));

        concept.setTermKo(dto.getLabel());
        concept.setTermEn(dto.getLabelEn());
        concept.setDescription(dto.getDescription());

        OntologyObjectDict saved = objectDictRepository.save(concept);
        return dto;
    }

    public DictionaryDto updateRelation(Long id, DictionaryDto dto) {
        OntologyRelationDict relation = relationDictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Relation not found: " + id));

        relation.setRelationKo(dto.getLabel());
        relation.setRelationEn(dto.getLabelEn());
        relation.setDescription(dto.getDescription());

        OntologyRelationDict saved = relationDictRepository.save(relation);
        return dto;
    }

    public void deleteConcept(Long id) {
        objectDictRepository.deleteById(id);
    }

    public void deleteRelation(Long id) {
        relationDictRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<String> getConceptCategories(Long workspaceId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return objectDictRepository.findDistinctCategoriesByWorkspaceId(workspaceId);
        }
        return getConcepts(workspaceId, documentIds, org.springframework.data.domain.Pageable.unpaged()).stream()
                .map(DictionaryDto::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getRelationCategories(Long workspaceId, List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return relationDictRepository.findDistinctCategoriesByWorkspaceId(workspaceId);
        }
        return getRelations(workspaceId, documentIds, org.springframework.data.domain.Pageable.unpaged()).stream()
                .map(DictionaryDto::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public void mergeConcepts(Long sourceId, Long targetId, Long workspaceId) {
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot merge a concept into itself.");
        }

        OntologyObjectDict sourceConcept = objectDictRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source concept not found: " + sourceId));
        OntologyObjectDict targetConcept = objectDictRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Target concept not found: " + targetId));

        // 1. Merge Document Sources (JSON Array)
        mergeSourceFields(sourceConcept, targetConcept);

        // 2a. Add Source Name as Synonym to Target
        addAsSynonym(sourceConcept, targetConcept, workspaceId);

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
                knowlearnTypeRepository.delete(triple);
            } else {
                triple.setObjectId(targetId);
                knowlearnTypeRepository.save(triple);
            }
        }

        // 5. Delete Source Concept
        objectDictRepository.delete(sourceConcept);
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

    private void mergeSourceFields(OntologyObjectDict source, OntologyObjectDict target) {
        try {
            Set<String> mergedIds = new HashSet<>();

            if (target.getSource() != null && !target.getSource().equals("initial_data")
                    && !target.getSource().isEmpty()) {
                List<String> targetIds = objectMapper.readValue(target.getSource(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                        });
                mergedIds.addAll(targetIds);
            }

            if (source.getSource() != null && !source.getSource().equals("initial_data")
                    && !source.getSource().isEmpty()) {
                List<String> sourceIds = objectMapper.readValue(source.getSource(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                        });
                mergedIds.addAll(sourceIds);
            }

            if (!mergedIds.isEmpty()) {
                target.setSource(objectMapper.writeValueAsString(new ArrayList<>(mergedIds)));
                objectDictRepository.save(target);
            }
        } catch (Exception e) {
            log.error("Failed to merge source fields", e);
        }
    }
}
