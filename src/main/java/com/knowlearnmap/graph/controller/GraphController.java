package com.knowlearnmap.graph.controller;

import com.knowlearnmap.common.dto.ApiResponse;
import com.knowlearnmap.graph.dto.GraphDataDto;
import com.knowlearnmap.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graph/workspace")
@RequiredArgsConstructor
@Slf4j
public class GraphController {

    private final GraphService graphService;

    @GetMapping("/{workspaceId}")
    public ResponseEntity<ApiResponse<GraphDataDto>> getGraph(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) List<Long> documentIds) {

        log.info("Graph Request - Workspace: {}, Documents: {}", workspaceId, documentIds);

        GraphDataDto graphData = graphService.getGraphData(workspaceId, documentIds);
        return ResponseEntity.ok(ApiResponse.success(graphData));
    }
}
