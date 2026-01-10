package com.knowlearnmap.ontologyToArango.service;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoCollection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * ArangoDB Ontology Cleanup Service
 * 
 * Document 및 Workspace 삭제 시 ArangoDB 데이터 정리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyArangoCleanupService {

    private final ArangoDB arangoDB;

    /**
     * Document 삭제 시 ArangoDB 참조 제거
     * 
     * @param dbName      ArangoDB 데이터베이스 이름
     * @param workspaceId 워크스페이스 ID
     * @param documentId  삭제할 문서 ID
     */
    public void removeDocumentReferences(String dbName, Long workspaceId, Long documentId) {
        if (dbName == null || dbName.isEmpty()) {
            log.warn("ArangoDB name is null, skipping document reference cleanup");
            return;
        }

        try {
            ArangoDatabase db = arangoDB.db(dbName);
            if (!db.exists()) {
                log.warn("Database {} does not exist, skipping cleanup", dbName);
                return;
            }

            log.info("Removing document references from ArangoDB: dbName={}, workspaceId={}, documentId={}",
                    dbName, workspaceId, documentId);

            // Remove from ontology_knowlearn_reference
            removeDocumentFromCollection(db, "ontology_knowlearn_reference", workspaceId, documentId);

            // Remove from ontology_object_reference
            removeDocumentFromCollection(db, "ontology_object_reference", workspaceId, documentId);

            // Remove from ontology_relation_reference
            removeDocumentFromCollection(db, "ontology_relation_reference", workspaceId, documentId);

            log.info("Document references removed successfully from ArangoDB");

        } catch (Exception e) {
            log.error("Failed to remove document references from ArangoDB: dbName={}, documentId={}",
                    dbName, documentId, e);
            // Don't throw - allow PostgreSQL deletion to proceed
        }
    }

    /**
     * 특정 컬렉션에서 documentId 참조 제거
     */
    private void removeDocumentFromCollection(ArangoDatabase db, String collectionName,
            Long workspaceId, Long documentId) {
        try {
            ArangoCollection collection = db.collection(collectionName);
            if (!collection.exists()) {
                log.debug("Collection {} does not exist, skipping", collectionName);
                return;
            }

            // AQL to remove documentId from document_ids array
            String aql = "FOR doc IN " + collectionName + " " +
                    "FILTER doc.workspace_id == @workspaceId " +
                    "FILTER @documentId IN doc.document_ids " +
                    "UPDATE doc WITH { " +
                    "  document_ids: REMOVE_VALUE(doc.document_ids, @documentId) " +
                    "} IN " + collectionName;

            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("workspaceId", workspaceId);
            bindVars.put("documentId", documentId);

            db.query(aql, Void.class, bindVars, null);
            log.debug("Removed documentId {} from collection {}", documentId, collectionName);

        } catch (Exception e) {
            log.error("Failed to remove document from collection {}: {}", collectionName, e.getMessage());
        }
    }

    /**
     * 고아 레코드 삭제 (참조가 없는 dict/synonyms)
     * 
     * @param dbName      ArangoDB 데이터베이스 이름
     * @param workspaceId 워크스페이스 ID
     */
    public void deleteOrphanedRecords(String dbName, Long workspaceId) {
        if (dbName == null || dbName.isEmpty()) {
            log.warn("ArangoDB name is null, skipping orphaned records cleanup");
            return;
        }

        try {
            ArangoDatabase db = arangoDB.db(dbName);
            if (!db.exists()) {
                log.warn("Database {} does not exist, skipping orphaned cleanup", dbName);
                return;
            }

            log.info("Deleting orphaned records from ArangoDB: dbName={}, workspaceId={}", dbName, workspaceId);

            // Delete orphaned knowlearn dict/synonyms
            deleteOrphanedKnowlearnRecords(db, workspaceId);

            // Delete orphaned object dict
            deleteOrphanedObjectRecords(db, workspaceId);

            // Delete orphaned relation dict
            deleteOrphanedRelationRecords(db, workspaceId);

            log.info("Orphaned records deleted successfully from ArangoDB");

        } catch (Exception e) {
            log.error("Failed to delete orphaned records from ArangoDB: dbName={}", dbName, e);
            // Don't throw - this is cleanup operation
        }
    }

    /**
     * 고아 knowlearn 레코드 삭제
     */
    private void deleteOrphanedKnowlearnRecords(ArangoDatabase db, Long workspaceId) {
        try {
            // Delete dict records with no references
            String dictAql = "FOR dict IN ontology_knowlearn_dict " +
                    "FILTER dict.workspace_id == @workspaceId " +
                    "LET hasRef = ( " +
                    "  FOR ref IN ontology_knowlearn_reference " +
                    "    FILTER ref.workspace_id == @workspaceId " +
                    "    FILTER ref.dict_id == dict._key " +
                    "    LIMIT 1 " +
                    "    RETURN 1 " +
                    ") " +
                    "FILTER LENGTH(hasRef) == 0 " +
                    "REMOVE dict IN ontology_knowlearn_dict";

            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("workspaceId", workspaceId);

            db.query(dictAql, Void.class, bindVars, null);
            log.debug("Deleted orphaned knowlearn dict records");

            // Delete synonym records with no dict
            String synonymAql = "FOR syn IN ontology_knowlearn_synonyms " +
                    "FILTER syn.workspace_id == @workspaceId " +
                    "LET hasDict = DOCUMENT('ontology_knowlearn_dict', syn.dict_id) " +
                    "FILTER hasDict == null " +
                    "REMOVE syn IN ontology_knowlearn_synonyms";

            db.query(synonymAql, Void.class, bindVars, null);
            log.debug("Deleted orphaned knowlearn synonym records");

        } catch (Exception e) {
            log.error("Failed to delete orphaned knowlearn records: {}", e.getMessage());
        }
    }

    /**
     * 고아 object 레코드 삭제
     */
    private void deleteOrphanedObjectRecords(ArangoDatabase db, Long workspaceId) {
        try {
            String aql = "FOR dict IN ontology_object_dict " +
                    "FILTER dict.workspace_id == @workspaceId " +
                    "LET hasRef = ( " +
                    "  FOR ref IN ontology_object_reference " +
                    "    FILTER ref.workspace_id == @workspaceId " +
                    "    FILTER ref.dict_id == dict._key " +
                    "    LIMIT 1 " +
                    "    RETURN 1 " +
                    ") " +
                    "FILTER LENGTH(hasRef) == 0 " +
                    "REMOVE dict IN ontology_object_dict";

            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("workspaceId", workspaceId);

            db.query(aql, Void.class, bindVars, null);
            log.debug("Deleted orphaned object dict records");

        } catch (Exception e) {
            log.error("Failed to delete orphaned object records: {}", e.getMessage());
        }
    }

    /**
     * 고아 relation 레코드 삭제
     */
    private void deleteOrphanedRelationRecords(ArangoDatabase db, Long workspaceId) {
        try {
            String aql = "FOR dict IN ontology_relation_dict " +
                    "FILTER dict.workspace_id == @workspaceId " +
                    "LET hasRef = ( " +
                    "  FOR ref IN ontology_relation_reference " +
                    "    FILTER ref.workspace_id == @workspaceId " +
                    "    FILTER ref.dict_id == dict._key " +
                    "    LIMIT 1 " +
                    "    RETURN 1 " +
                    ") " +
                    "FILTER LENGTH(hasRef) == 0 " +
                    "REMOVE dict IN ontology_relation_dict";

            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("workspaceId", workspaceId);

            db.query(aql, Void.class, bindVars, null);
            log.debug("Deleted orphaned relation dict records");

        } catch (Exception e) {
            log.error("Failed to delete orphaned relation records: {}", e.getMessage());
        }
    }

    /**
     * Workspace 삭제 시 해당 워크스페이스의 ArangoDB 데이터만 삭제
     * (ObjectNodes, RelationNodes, KnowlearnEdges만 삭제)
     * 
     * @param dbName      ArangoDB 데이터베이스 이름
     * @param workspaceId 삭제할 워크스페이스 ID
     */
    public void deleteWorkspaceData(String dbName, Long workspaceId) {
        if (dbName == null || dbName.isEmpty()) {
            log.warn("ArangoDB name is null, skipping workspace data cleanup");
            return;
        }

        try {
            ArangoDatabase db = arangoDB.db(dbName);
            if (!db.exists()) {
                log.warn("Database {} does not exist, skipping workspace cleanup", dbName);
                return;
            }

            log.info("Deleting workspace {} data from ArangoDB: dbName={}", workspaceId, dbName);

            Map<String, Object> bindVars = new HashMap<>();
            bindVars.put("workspaceId", workspaceId);

            // Delete ObjectNodes for this workspace
            deleteFromCollection(db, "ObjectNodes", workspaceId, bindVars);

            // Delete RelationNodes for this workspace
            deleteFromCollection(db, "RelationNodes", workspaceId, bindVars);

            // Delete KnowlearnEdges for this workspace
            deleteFromCollection(db, "KnowlearnEdges", workspaceId, bindVars);

            log.info("Workspace {} data deleted successfully from ArangoDB", workspaceId);

        } catch (Exception e) {
            log.error("Failed to delete workspace {} data from ArangoDB: dbName={}", workspaceId, dbName, e);
            // Don't throw - allow workspace deletion to proceed
        }
    }

    /**
     * 특정 컬렉션에서 workspace_id에 해당하는 데이터 삭제
     */
    private void deleteFromCollection(ArangoDatabase db, String collectionName, Long workspaceId,
            Map<String, Object> bindVars) {
        try {
            ArangoCollection collection = db.collection(collectionName);
            if (!collection.exists()) {
                log.debug("Collection {} does not exist, skipping", collectionName);
                return;
            }

            String aql = "FOR doc IN " + collectionName + " " +
                    "FILTER doc.workspace_id == @workspaceId " +
                    "REMOVE doc IN " + collectionName;

            db.query(aql, Void.class, bindVars, null);
            log.info("Deleted workspace {} data from collection {}", workspaceId, collectionName);

        } catch (Exception e) {
            log.error("Failed to delete from collection {}: {}", collectionName, e.getMessage());
            // Continue with other collections
        }
    }
}
