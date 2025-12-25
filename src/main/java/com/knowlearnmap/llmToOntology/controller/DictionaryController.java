package com.knowlearnmap.llmToOntology.controller;

import com.knowlearnmap.llmToOntology.dto.DictionaryDto;
import com.knowlearnmap.llmToOntology.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
@Slf4j
public class DictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/concepts")
    public ResponseEntity<List<DictionaryDto>> getConcepts(@RequestParam Long workspaceId) {
        return ResponseEntity.ok(dictionaryService.getConcepts(workspaceId));
    }

    @GetMapping("/relations")
    public ResponseEntity<List<DictionaryDto>> getRelations(@RequestParam Long workspaceId) {
        return ResponseEntity.ok(dictionaryService.getRelations(workspaceId));
    }

    @PutMapping("/concepts/{id}")
    public ResponseEntity<DictionaryDto> updateConcept(@PathVariable Long id, @RequestBody DictionaryDto dto) {
        return ResponseEntity.ok(dictionaryService.updateConcept(id, dto));
    }

    @PutMapping("/relations/{id}")
    public ResponseEntity<DictionaryDto> updateRelation(@PathVariable Long id, @RequestBody DictionaryDto dto) {
        return ResponseEntity.ok(dictionaryService.updateRelation(id, dto));
    }

    @DeleteMapping("/concepts/{id}")
    public ResponseEntity<Void> deleteConcept(@PathVariable Long id) {
        dictionaryService.deleteConcept(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/relations/{id}")
    public ResponseEntity<Void> deleteRelation(@PathVariable Long id) {
        dictionaryService.deleteRelation(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(@RequestParam String type, @RequestParam Long workspaceId) {
        if ("concept".equals(type)) {
            return ResponseEntity.ok(dictionaryService.getConceptCategories(workspaceId));
        } else {
            return ResponseEntity.ok(dictionaryService.getRelationCategories(workspaceId));
        }
    }
}
