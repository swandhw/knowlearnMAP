package com.knowlearnmap.llmToOntology.service;

import com.knowlearnmap.llmToOntology.domain.*;
import com.knowlearnmap.llmToOntology.dto.DictionaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public List<DictionaryDto> getConcepts(Long workspaceId) {
        List<OntologyObjectDict> objects = objectDictRepository.findByWorkspaceId(workspaceId);

        return objects.stream().map(obj -> {
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
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DictionaryDto> getRelations(Long workspaceId) {
        List<OntologyRelationDict> relations = relationDictRepository.findByWorkspaceId(workspaceId);

        return relations.stream().map(rel -> {
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
        }).collect(Collectors.toList());
    }

    public DictionaryDto updateConcept(Long id, DictionaryDto dto) {
        OntologyObjectDict concept = objectDictRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Concept not found: " + id));

        concept.setTermKo(dto.getLabel());
        concept.setTermEn(dto.getLabelEn());
        concept.setDescription(dto.getDescription());
        // Note: Logic for updating synonyms would be more complex (delete all +
        // re-insert or diff).
        // For MVP, letting frontend display synonyms is enough, or we can tackle
        // synonym update if requested.
        // Assuming simple metadata update for now.

        OntologyObjectDict saved = objectDictRepository.save(concept);
        return dto; // simplified return
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
    public List<String> getConceptCategories(Long workspaceId) {
        return objectDictRepository.findDistinctCategoriesByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public List<String> getRelationCategories(Long workspaceId) {
        return relationDictRepository.findDistinctCategoriesByWorkspaceId(workspaceId);
    }
}
