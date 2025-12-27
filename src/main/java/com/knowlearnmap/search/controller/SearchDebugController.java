package com.knowlearnmap.search.controller;

import com.knowlearnmap.search.dto.SearchDebugDto;
import com.knowlearnmap.search.service.SearchDebugService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchDebugController {

    private final SearchDebugService searchDebugService;

    @PostMapping("/debug")
    public ResponseEntity<SearchDebugDto> debugSearch(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        Long workspaceId = Long.valueOf(request.get("workspaceId").toString());

        List<Long> documentIds = null;
        if (request.containsKey("documentIds") && request.get("documentIds") instanceof List) {
            List<?> list = (List<?>) request.get("documentIds");
            if (!list.isEmpty()) {
                // Safely convert to List<Long>
                try {
                    documentIds = list.stream()
                            .map(item -> Long.valueOf(item.toString()))
                            .collect(Collectors.toList());
                } catch (NumberFormatException e) {
                    // ignore if invalid
                }
            }
        }

        return ResponseEntity.ok(searchDebugService.searchDebug(query, workspaceId, documentIds));
    }
}
