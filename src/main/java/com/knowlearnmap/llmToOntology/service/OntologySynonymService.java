package com.knowlearnmap.llmToOntology.service;

import com.knowlearnmap.llmToOntology.domain.OntologyObjectSynonyms;
import com.knowlearnmap.llmToOntology.domain.OntologyObjectSynonymsRepository;
import com.knowlearnmap.llmToOntology.domain.OntologyRelationSynonyms;
import com.knowlearnmap.llmToOntology.domain.OntologyRelationSynonymsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OntologySynonymService {

    private final OntologyObjectSynonymsRepository objectSynonymsRepository;
    private final OntologyRelationSynonymsRepository relationSynonymsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveObjectSynonymIfNecessary(Long workspaceId, String category, Long objectId, String originalTerm,
            String normalizedTerm, String language) {
        if (originalTerm != null && !normalizedTerm.equals(originalTerm)) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveRelationSynonymIfNecessary(Long workspaceId, String category, Long relationId, String originalTerm,
            String normalizedTerm, String language) {
        if (originalTerm != null && !normalizedTerm.equals(originalTerm)) {
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
}
