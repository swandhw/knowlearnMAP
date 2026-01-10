package com.knowlearnmap.chat.service;

import com.knowlearnmap.chat.dto.ChatResponseDto;
import com.knowlearnmap.chat.dto.SourceDto;
import com.knowlearnmap.search.dto.SearchDebugDto;
import com.knowlearnmap.search.service.SearchDebugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final SearchDebugService searchDebugService;

    /**
     * Chat with RAG and Ontology search
     * Returns search results without LLM integration (for now)
     */
    @Transactional(readOnly = true)
    public ChatResponseDto chat(String query, Long workspaceId, List<Long> documentIds) {
        log.info("Chat request: workspaceId={}, query={}, documentIds={}", workspaceId, query, documentIds);

        // Execute search
        SearchDebugDto searchResults = searchDebugService.searchDebug(query, workspaceId, documentIds);

        // Convert RAG results
        List<SourceDto> ragResults = searchResults.getRagResults().stream()
                .map(result -> SourceDto.builder()
                        .type("RAG")
                        .content(result.getContent())
                        .score(result.getScore())
                        .metadata(result.getMetadata())
                        .build())
                .collect(Collectors.toList());

        // Combine ontology results (AQL text + Knowlearn vector)
        List<SourceDto> ontologyResults = new ArrayList<>();

        // Add AQL text search results
        searchResults.getAqlResults().stream()
                .map(result -> SourceDto.builder()
                        .type("ONTOLOGY_TEXT")
                        .content(result.getContent())
                        .score(result.getScore())
                        .metadata(result.getMetadata())
                        .build())
                .forEach(ontologyResults::add);

        // Add Knowlearn vector search results
        searchResults.getKnowlearnResults().stream()
                .map(result -> SourceDto.builder()
                        .type("ONTOLOGY_VECTOR")
                        .content(result.getContent())
                        .score(result.getScore())
                        .metadata(result.getMetadata())
                        .build())
                .forEach(ontologyResults::add);

        log.info("Search completed: ragResults={}, ontologyResults={}", ragResults.size(), ontologyResults.size());

        return ChatResponseDto.builder()
                .ragResults(ragResults)
                .ontologyResults(ontologyResults)
                .build();
    }
}
