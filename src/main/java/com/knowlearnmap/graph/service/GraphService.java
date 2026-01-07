package com.knowlearnmap.graph.service;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.knowlearnmap.graph.dto.GraphDataDto;
import com.knowlearnmap.workspace.domain.WorkspaceEntity;
import com.knowlearnmap.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphService {

    private final ArangoDB arangoDB;
    private final WorkspaceRepository workspaceRepository;

    @Transactional(readOnly = true)
    public GraphDataDto getGraphData(Long workspaceId, List<Long> documentIds) {
        // 1. Resolve Database Name from Workspace
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        if (workspace.getDomain() == null || workspace.getDomain().getArangoDbName() == null) {
            throw new IllegalArgumentException("ArangoDB not configured for this workspace domain.");
        }

        String dbName = workspace.getDomain().getArangoDbName();
        ArangoDatabase db = arangoDB.db(dbName);

        log.info("Connecting to ArangoDB: {}", dbName);

        if (!db.exists()) {
            log.error("Database {} does not exist!", dbName);
            return new GraphDataDto(Collections.emptyList(), Collections.emptyList());
        }

        com.arangodb.ArangoCollection edgeCol = db.collection("KnowlearnEdges");
        if (!edgeCol.exists()) {
            log.error("Collection KnowlearnEdges does not exist in db {}!", dbName);
            return new GraphDataDto(Collections.emptyList(), Collections.emptyList());
        }

        try {
            log.info("KnowlearnEdges count: {}", edgeCol.count().getCount());
        } catch (Exception e) {
            log.error("Failed to count edges in KnowlearnEdges", e);
            // Don't throw, try to proceed, maybe just count failed
        }

        // 2. Fetch Edges with AQL Filtering
        String edgeAql;
        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("workspaceId", workspaceId);

        if (documentIds != null && !documentIds.isEmpty()) {
            // Robustness: Include both String and Long types in the filter list
            // This ensures we match whether ArangoDB stored them as ["1"] or [1]
            List<Object> mixedDocIds = new java.util.ArrayList<>();
            if (documentIds != null) {
                for (Long id : documentIds) {
                    mixedDocIds.add(id); // Add as Long
                    mixedDocIds.add(String.valueOf(id)); // Add as String
                }
            }
            bindVars.put("targetDocIds", mixedDocIds);

            // Filter by workspace AND document intersection
            // INTERSECTION(e.document_ids, @targetDocIds) returns elements present in both.
            // If length > 0, the edge is related to at least one selected document.
            edgeAql = "FOR e IN KnowlearnEdges " +
                    "FILTER e.workspace_id == @workspaceId " +
                    "FILTER LENGTH(INTERSECTION(e.document_ids, @targetDocIds)) > 0 " +
                    "RETURN { " +
                    " _id: e._id, " +
                    " _key: e._key, " +
                    " _from: e._from, " +
                    " _to: e._to, " +
                    " workspace_id: e.workspace_id, " +
                    " document_ids: e.document_ids, " +
                    " label_ko: e.label_ko, " +
                    " label_en: e.label_en, " +
                    " relation_ko: e.relation_ko, " +
                    " relation_en: e.relation_en " +
                    "}";
        } else {
            // Filter only by workspace
            edgeAql = "FOR e IN KnowlearnEdges " +
                    "FILTER e.workspace_id == @workspaceId " +
                    "RETURN { " +
                    " _id: e._id, " +
                    " _key: e._key, " +
                    " _from: e._from, " +
                    " _to: e._to, " +
                    " workspace_id: e.workspace_id, " +
                    " document_ids: e.document_ids, " +
                    " label_ko: e.label_ko, " +
                    " label_en: e.label_en, " +
                    " relation_ko: e.relation_ko, " +
                    " relation_en: e.relation_en " +
                    "}";
        }

        List<Map<String, Object>> edges;
        try {
            log.debug("Executing Edge AQL: {} with vars: {}", edgeAql, bindVars);
            ArangoCursor<Map> cursor = db.query(edgeAql, Map.class, bindVars, new com.arangodb.model.AqlQueryOptions());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) (List<?>) cursor.asListRemaining();
            edges = result;
        } catch (Exception e) {
            log.error("Failed to query edges with AQL", e);
            throw new RuntimeException("Graph AQL Query Error: " + e.getMessage(), e);
        }

        if (edges.isEmpty()) {
            return new GraphDataDto(Collections.emptyList(), Collections.emptyList());
        }

        // 3. Collect Unique Node IDs
        java.util.Set<String> nodeIds = new java.util.HashSet<>();
        for (Map<String, Object> edge : edges) {
            if (edge.containsKey("_from"))
                nodeIds.add((String) edge.get("_from"));
            if (edge.containsKey("_to"))
                nodeIds.add((String) edge.get("_to"));
        }

        if (nodeIds.isEmpty()) {
            return new GraphDataDto(Collections.emptyList(), edges);
        }

        // 4. Fetch Nodes by IDs with workspace_id filter for performance
        // Instead of DOCUMENT(id), we query the collections directly with workspace_id
        // filter
        String nodeAql = "FOR id IN @ids " +
                "LET parts = SPLIT(id, '/') " +
                "LET collectionName = parts[0] " +
                "LET docKey = parts[1] " +
                "LET doc = DOCUMENT(collectionName, docKey) " +
                "FILTER doc != null AND doc.workspace_id == @workspaceId " +
                "RETURN doc";

        Map<String, Object> nodeBindVars = new HashMap<>();
        nodeBindVars.put("ids", nodeIds);
        nodeBindVars.put("workspaceId", workspaceId);

        List<Map<String, Object>> nodes;
        try {
            log.debug("Executing Node AQL for {} ids with workspace filter", nodeIds.size());
            ArangoCursor<Map> cursor = db.query(nodeAql, Map.class, nodeBindVars,
                    new com.arangodb.model.AqlQueryOptions());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) (List<?>) cursor.asListRemaining();
            nodes = result;
        } catch (Exception e) {
            log.error("Failed to query nodes for workspace {}", workspaceId, e);
            throw new RuntimeException("Graph Node Query Error", e);
        }

        log.info("Graph query successful. Nodes: {}, Links: {}", nodes.size(), edges.size());
        return new GraphDataDto(nodes, edges);
    }
}
