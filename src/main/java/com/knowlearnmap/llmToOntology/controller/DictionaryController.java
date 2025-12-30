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
    public ResponseEntity<org.springframework.data.domain.Page<DictionaryDto>> getConcepts(
            @RequestParam Long workspaceId,
            @RequestParam(required = false) List<Long> documentIds,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(dictionaryService.getConcepts(workspaceId, documentIds, pageable));
    }

    @GetMapping("/relations")
    public ResponseEntity<org.springframework.data.domain.Page<DictionaryDto>> getRelations(
            @RequestParam Long workspaceId,
            @RequestParam(required = false) List<Long> documentIds,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(dictionaryService.getRelations(workspaceId, documentIds, pageable));
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
    public ResponseEntity<List<String>> getCategories(@RequestParam String type, @RequestParam Long workspaceId,
            @RequestParam(required = false) List<Long> documentIds) {
        if ("concept".equals(type)) {
            return ResponseEntity.ok(dictionaryService.getConceptCategories(workspaceId, documentIds));
        } else {
            return ResponseEntity.ok(dictionaryService.getRelationCategories(workspaceId, documentIds));
        }
    }

    @PostMapping("/concepts/merge")
    public ResponseEntity<Void> mergeConcepts(@RequestBody java.util.Map<String, Object> request) {
        Long sourceId = Long.valueOf(request.get("sourceId").toString());
        Long targetId = Long.valueOf(request.get("targetId").toString());
        Long workspaceId = Long.valueOf(request.get("workspaceId").toString());
        String mode = request.getOrDefault("mode", "move").toString(); // default to move for safety

        boolean keepSourceAsSynonym = "move".equalsIgnoreCase(mode);

        dictionaryService.mergeConcepts(sourceId, targetId, workspaceId, keepSourceAsSynonym);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
