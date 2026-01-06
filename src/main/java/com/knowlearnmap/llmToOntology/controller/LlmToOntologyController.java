package com.knowlearnmap.llmToOntology.controller;

import com.knowlearnmap.llmToOntology.service.LlmToOntologyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ontology")
@RequiredArgsConstructor
@Slf4j
public class LlmToOntologyController {

    private final LlmToOntologyService llmToOntologyService;

    /**
     * 특정 문서의 모든 Chunk에 대해 Ontology 생성 (LLM 완료된 것만)
     */
    @PostMapping("/documents/{documentId}")
    public ResponseEntity<String> createOntologyFromDocument(
            @RequestParam Long workspaceId,
            @PathVariable Long documentId,
            org.springframework.security.core.Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "anonymous";
        if (authentication == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        log.info("Ontology generation requested for document: {}, workspace: {}, user: {}", documentId, workspaceId,
                username);

        try {
            int count = llmToOntologyService.createOntologyFromDocument(workspaceId, documentId, username);
            return ResponseEntity.ok("Ontology generation started for " + count + " chunks.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
