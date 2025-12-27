package com.knowlearnmap.search.service;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.knowlearnmap.ai.service.EmbeddingService;
import com.knowlearnmap.document.domain.DocumentChunk;
import com.knowlearnmap.document.repository.DocumentChunkRepository;
import com.knowlearnmap.search.dto.SearchDebugDto;
import com.knowlearnmap.search.dto.SearchDebugDto.SearchResult;
import com.knowlearnmap.workspace.domain.WorkspaceEntity;
import com.knowlearnmap.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchDebugService {

    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    private final ArangoDB arangoDB;
    private final WorkspaceRepository workspaceRepository;

    @Transactional(readOnly = true)
    public SearchDebugDto searchDebug(String query, Long workspaceId, List<Long> documentIds) {
        // 1. Generate Embedding for Query
        List<Double> queryVector = embeddingService.embed(query);

        // 2. RAG Search (Postgres In-Memory)
        List<SearchResult> ragResults = searchRag(queryVector, workspaceId, documentIds);

        // 3. AQL Search (ArangoDB Text)
        List<SearchResult> aqlResults = searchAql(query, workspaceId, documentIds);

        // 4. Knowlearn Search (ArangoDB Vector)
        List<SearchResult> knowlearnResults = searchKnowlearn(queryVector, workspaceId, documentIds);

        return SearchDebugDto.builder()
                .ragResults(ragResults)
                .aqlResults(aqlResults)
                .knowlearnResults(knowlearnResults)
                .build();
    }

    private List<SearchResult> searchRag(List<Double> queryVector, Long workspaceId, List<Long> documentIds) {
        List<DocumentChunk> chunks;
        if (documentIds != null && !documentIds.isEmpty()) {
            chunks = documentChunkRepository.findByDocumentIdIn(documentIds);
        } else {
            chunks = documentChunkRepository.findByDocumentWorkspaceId(workspaceId);
        }

        return chunks.stream()
                .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty())
                .map(chunk -> {
                    double similarity = cosineSimilarity(queryVector, chunk.getEmbedding());
                    return new AbstractMap.SimpleEntry<>(chunk, similarity);
                })
                .sorted(Map.Entry.<DocumentChunk, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> SearchResult.builder()
                        .content(entry.getKey().getContent())
                        .score(entry.getValue())
                        .metadata(Map.of("page",
                                entry.getKey().getPageNumber() != null ? entry.getKey().getPageNumber() : 0,
                                "filename", entry.getKey().getDocument().getFilename()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<SearchResult> searchAql(String query, Long workspaceId, List<Long> documentIds) {
        ArangoDatabase db = getArangoDb(workspaceId);
        if (db == null)
            return Collections.emptyList();

        try {
            boolean filterDocs = documentIds != null && !documentIds.isEmpty();
            String aql = "FOR doc IN ObjectNodes " +
                    "FILTER doc.workspace_id == @wsId " +
                    (filterDocs ? "AND @docIds ANY IN doc.document_ids " : "") +
                    "AND (CONTAINS(LOWER(doc.label), LOWER(@query)) OR CONTAINS(LOWER(doc.description), LOWER(@query))) "
                    +
                    "LIMIT 5 " +
                    "RETURN { content: CONCAT(doc.label, ': ', doc.description), id: doc._key, score: 1.0 }";

            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("wsId", workspaceId);
            bindVars.put("query", query);
            if (filterDocs) {
                bindVars.put("docIds", documentIds.stream().map(String::valueOf).collect(Collectors.toList()));
            }

            ArangoCursor<Map> cursor = db.query(aql, Map.class, bindVars);
            List<SearchResult> results = new ArrayList<>();
            while (cursor.hasNext()) {
                Map doc = cursor.next();
                results.add(SearchResult.builder()
                        .content((String) doc.get("content"))
                        .score(1.0)
                        .metadata(Map.of("id", doc.get("id")))
                        .build());
            }
            return results;
        } catch (Exception e) {
            log.error("AQL Search failed", e);
            return Collections.emptyList();
        }
    }

    private List<SearchResult> searchKnowlearn(List<Double> queryVector, Long workspaceId, List<Long> documentIds) {
        ArangoDatabase db = getArangoDb(workspaceId);
        if (db == null)
            return Collections.emptyList();

        try {
            boolean filterDocs = documentIds != null && !documentIds.isEmpty();
            // Search both ObjectNodes (Entities) and KnowlearnEdges (Facts)
            String aql = "LET nodes = ( " +
                    "  FOR doc IN ObjectNodes " +
                    "  FILTER doc.workspace_id == @wsId AND doc.embedding_vector != null " +
                    (filterDocs ? "AND @docIds ANY IN doc.document_ids " : "") +
                    "  LET score = COSINE_SIMILARITY(doc.embedding_vector, @vector) " +
                    "  RETURN { type: 'Node', content: CONCAT(doc.label_ko, ': ', doc.description), id: doc._key, score: score } "
                    +
                    ") " +
                    "LET edges = ( " +
                    "  FOR doc IN KnowlearnEdges " +
                    "  FILTER doc.workspace_id == @wsId AND doc.embedding_vector != null " +
                    (filterDocs ? "AND @docIds ANY IN doc.document_ids " : "") +
                    "  LET score = COSINE_SIMILARITY(doc.embedding_vector, @vector) " +
                    "  RETURN { type: 'Edge', content: doc.sentence_ko, id: doc._key, score: score } " +
                    ") " +
                    "FOR result IN UNION(nodes, edges) " +
                    "SORT result.score DESC " +
                    "LIMIT 5 " +
                    "RETURN result";

            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("wsId", workspaceId);
            bindVars.put("vector", queryVector);
            if (filterDocs) {
                bindVars.put("docIds", documentIds.stream().map(String::valueOf).collect(Collectors.toList()));
            }

            ArangoCursor<Map> cursor = db.query(aql, Map.class, bindVars);
            List<SearchResult> results = new ArrayList<>();
            while (cursor.hasNext()) {
                Map doc = cursor.next();
                String type = (String) doc.get("type");
                String prefix = type.equals("Edge") ? "[Fact] " : "[Entity] ";

                results.add(SearchResult.builder()
                        .content(prefix + doc.get("content"))
                        .score(((Number) doc.get("score")).doubleValue())
                        .metadata(Map.of("id", doc.get("id"), "type", type))
                        .build());
            }
            return results;
        } catch (Exception e) {
            log.error("Knowlearn Vector Search failed", e);
            return Collections.emptyList();
        }
    }

    private ArangoDatabase getArangoDb(Long workspaceId) {
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null || workspace.getDomain() == null)
            return null;
        String dbName = workspace.getDomain().getArangoDbName();
        if (dbName == null)
            return null;
        return arangoDB.db(dbName);
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size())
            return 0.0;
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
