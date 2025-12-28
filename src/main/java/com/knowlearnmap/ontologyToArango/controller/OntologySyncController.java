package com.knowlearnmap.ontologyToArango.controller;

import com.knowlearnmap.ontologyToArango.service.OntologyToArangoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ontology/sync")
@RequiredArgsConstructor
@Slf4j
public class OntologySyncController {

    private final OntologyToArangoService arangoService;

    /**
     * 특정 Workspace의 데이터를 ArangoDB로 동기화합니다.
     * 
     * @param workspaceId 동기화할 Workspace ID
     */
    @PostMapping("/{workspaceId}")
    public ResponseEntity<String> syncToArango(
            @PathVariable Long workspaceId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean dropExist) {

        try {
            log.info("Starting ArangoDB sync for Workspace ID: {}, dropExist: {}", workspaceId, dropExist);
            arangoService.syncOntologyToArango(workspaceId, dropExist);
            return ResponseEntity.ok("Workspace ID " + workspaceId + "의 데이터가 ArangoDB로 성공적으로 동기화되었습니다.");
        } catch (IllegalArgumentException e) {
            log.error("Workspace configuration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("ArangoDB 동기화 중 에러 발생 (Workspace ID: {})", workspaceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("ArangoDB 동기화 실패: 내부 서버 오류.");
        }
    }
}
