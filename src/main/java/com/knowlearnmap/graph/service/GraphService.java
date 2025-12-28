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

        if (!db.exists()) {
            return new GraphDataDto(Collections.emptyList(), Collections.emptyList());
        }

        // 2. Build AQL Query
        // Supports both global view (documentIds empty) and filtered view
        boolean filterByDoc = documentIds != null && !documentIds.isEmpty();

        // [DEBUG] Inspect what kind of document_ids exist in the DB for this workspace
        try {
            String debugAql = "FOR e IN KnowlearnEdges FILTER e.workspace_id == @wsId LIMIT 5 RETURN { ids: e.document_ids, type: TYPENAME(e.document_ids[0]) }";
            Map<String, Object> debugBind = new HashMap<>();
            debugBind.put("wsId", workspaceId);

            ArangoCursor<Object> debugCursor = db.query(debugAql, Object.class, debugBind,
                    new com.arangodb.model.AqlQueryOptions());
            List<Object> debugList = debugCursor.asListRemaining();
            log.info("[DEBUG] Workspace {} Data Inspection: {}", workspaceId, debugList);
        } catch (Exception e) {
            log.warn("Debug query failed: {}", e.getMessage());
        }

        String aql = "LET edges = ( " +
                "  FOR e IN KnowlearnEdges " +
                "  FILTER e.workspace_id == @wsId " +
                (filterByDoc ? "  AND IS_LIST(e.document_ids) AND LENGTH(INTERSECTION(e.document_ids, @docIds)) > 0 "
                        : "")
                +
                "  RETURN e " +
                ") " +
                "LET nodeIds = UNIQUE(UNION(edges[*]._from, edges[*]._to)) " +
                "LET nodes = ( " +
                "  FOR id IN nodeIds " +
                "  LET doc = DOCUMENT(id) " +
                "  FILTER doc != null " +
                "  RETURN doc " +
                ") " +
                "RETURN { nodes: nodes, links: edges }";

        Map<String, Object> bindVars = new HashMap<>();
        bindVars.put("wsId", workspaceId);
        if (filterByDoc) {
            // ArangoDB stores document_ids as Strings (from JSON), so we need to match
            // types
            List<String> stringDocIds = documentIds.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.toList());
            bindVars.put("docIds", stringDocIds);
            log.info("Filtering graph by Document IDs: {}", stringDocIds);
        } else {
            log.info("Fetching full graph (no document filter)");
        }

        try {
            log.debug("Executing Graph AQL. BindVars: {}", bindVars);
            ArangoCursor<GraphDataDto> cursor = db.query(
                    aql,
                    GraphDataDto.class,
                    bindVars,
                    new com.arangodb.model.AqlQueryOptions());
            if (cursor.hasNext()) {
                GraphDataDto result = cursor.next();
                log.info("Graph query successful. Nodes: {}, Links: {}",
                        result.getNodes() != null ? result.getNodes().size() : 0,
                        result.getLinks() != null ? result.getLinks().size() : 0);
                return result;
            }
            log.info("Graph query returned empty result.");
        } catch (Exception e) {
            log.error("Failed to query graph data for workspace {}", workspaceId, e);
            throw new RuntimeException("Graph Query Error", e);
        }

        return new GraphDataDto(Collections.emptyList(), Collections.emptyList());
    }
}
