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
     * ?뱀젙 Workspace???⑦넧濡쒖? ?곗씠?곕? ArangoDB濡??숆린?뷀빀?덈떎.
     * 
     * @param workspaceId ?숆린?뷀븷 Workspace ID
     */
    @PostMapping("/{workspaceId}")
    public ResponseEntity<String> syncToArango(
            @PathVariable Long workspaceId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean dropExist) {

        // [二쇱쓽] ?꾩옱??4踰?Workspace留?泥섎━?섎룄濡?媛??
        if (!workspaceId.equals(4L)) {
            return ResponseEntity.badRequest().body("?꾩옱??Workspace ID 4踰덈쭔 ?숆린??媛?ν븯?꾨줉 ?ㅼ젙?섏뿀?듬땲??");
        }

        try {
            log.info("Starting ArangoDB sync for Workspace ID: {}, dropExist: {}", workspaceId, dropExist);
            arangoService.syncOntologyToArango(workspaceId, dropExist);
            return ResponseEntity.ok("Workspace ID " + workspaceId + "???⑦넧濡쒖? ?곗씠?곌? ArangoDB???깃났?곸쑝濡??숆린?붾릺?덉뒿?덈떎.");
        } catch (IllegalArgumentException e) {
            log.error("Workspace configuration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("ArangoDB ?숆린??以??ш컖???ㅻ쪟 諛쒖깮 (Workspace ID: {})", workspaceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("ArangoDB ?숆린???ㅽ뙣: ?대? ?쒕쾭 ?ㅻ쪟.");
        }
    }
}
