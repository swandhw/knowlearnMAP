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
            bindVars.put("docIds", documentIds);
        }

        try {
            ArangoCursor<GraphDataDto> cursor = db.query(
                    aql,
                    GraphDataDto.class,
                    bindVars,
                    new com.arangodb.model.AqlQueryOptions());
            if (cursor.hasNext()) {
                return cursor.next();
            }
        } catch (Exception e) {
            log.error("Failed to query graph data for workspace {}", workspaceId, e);
            throw new RuntimeException("Graph Query Error", e);
        }

        return new GraphDataDto(Collections.emptyList(), Collections.emptyList());
    }
}
