package com.knowlearnmap.ontologyToArango.service;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.knowlearnmap.llmToOntology.domain.*;
import com.knowlearnmap.workspace.domain.WorkspaceEntity;
import com.knowlearnmap.workspace.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OntologyToArangoService {

    private final WorkspaceRepository workspaceRepository;
    private final OntologyObjectDictRepository objectDictRepository;
    private final OntologyRelationDictRepository relationDictRepository;
    private final OntologyKnowlearnTypeRepository knowlearnTypeRepository;
    private final OntologyObjectSynonymsRepository objectSynonymsRepository;
    private final OntologyRelationSynonymsRepository relationSynonymsRepository;
    private final com.knowlearnmap.document.repository.DocumentChunkRepository documentChunkRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final ArangoDB arangoDB;

    @Transactional(readOnly = true)
    public void syncOntologyToArango(Long workspaceId, boolean dropExist) {

        // 1. PostgreSQL에서 Workspace 및 Domain 정보 조회
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace " + workspaceId + "를 찾을 수 없습니다."));

        if (workspace.getDomain() == null) {
            throw new IllegalArgumentException("Workspace " + workspaceId + "에 연결된 도메인이 없습니다.");
        }

        String targetDbName = workspace.getDomain().getArangoDbName();

        if (targetDbName == null || targetDbName.isEmpty()) {
            throw new IllegalArgumentException(
                    "Domain " + workspace.getDomain().getName() + "에 ArangoDB 이름이 설정되지 않았습니다.");
        }

        // 2. ArangoDB 인스턴스 관리
        if (arangoDB.db(targetDbName).exists()) {
            if (dropExist) {
                log.info("Dropping existing ArangoDB database: {}", targetDbName);
                arangoDB.db(targetDbName).drop();
                arangoDB.createDatabase(targetDbName);
                log.info("Created new ArangoDB database: {}", targetDbName);
            } else {
                log.info("Using existing ArangoDB database: {}", targetDbName);
            }
        } else {
            arangoDB.createDatabase(targetDbName);
            log.info("Created new ArangoDB database: {}", targetDbName);
        }

        ArangoDatabase db = arangoDB.db(targetDbName);

        // 컬렉션 생성
        ensureCollection(db, "ObjectNodes");
        ensureCollection(db, "RelationNodes");
        ensureEdgeCollection(db, "KnowlearnEdges");

        // 그래프 생성
        ensureGraph(db, "KnowlearnGraph", "KnowlearnEdges", "ObjectNodes");

        // 3. 데이터 조회 (Bulk Fetching for Performance)
        log.info("Fetching all data for workspace {}...", workspaceId);
        List<OntologyObjectDict> objectDicts = objectDictRepository.findByWorkspaceId(workspaceId);
        List<OntologyRelationDict> relationDicts = relationDictRepository.findByWorkspaceId(workspaceId);

        // Object Synonyms Map 구성
        Map<Long, List<OntologyObjectSynonyms>> objectSynonymsMap = new HashMap<>();
        for (OntologyObjectDict obj : objectDicts) {
            objectSynonymsMap.put(obj.getId(), objectSynonymsRepository.findByObjectId(obj.getId()));
        }

        // Relation Synonyms Map 구성
        Map<Long, List<OntologyRelationSynonyms>> relationSynonymsMap = new HashMap<>();
        for (OntologyRelationDict rel : relationDicts) {
            relationSynonymsMap.put(rel.getId(), relationSynonymsRepository.findByRelationId(rel.getId()));
        }

        // 4. 동기화 실행
        Map<Long, String> objectIdToKeyMap = syncObjectNodes(db, objectDicts, objectSynonymsMap, workspaceId);
        syncRelationNodes(db, relationDicts, relationSynonymsMap, workspaceId);
        syncKnowlearnEdges(db, workspaceId, objectDicts, relationDicts, objectSynonymsMap, relationSynonymsMap,
                objectIdToKeyMap);

        log.info("=================================================");
        log.info("Ontology Sync to ArangoDB Completed Successfully.");
        log.info("=================================================");
    }

    private void ensureCollection(ArangoDatabase db, String collectionName) {
        if (!db.collection(collectionName).exists()) {
            db.createCollection(collectionName);
        }
    }

    private void ensureEdgeCollection(ArangoDatabase db, String collectionName) {
        if (!db.collection(collectionName).exists()) {
            db.createCollection(collectionName,
                    new com.arangodb.model.CollectionCreateOptions().type(com.arangodb.entity.CollectionType.EDGES));
        }
    }

    private void ensureGraph(ArangoDatabase db, String graphName, String edgeCollection, String... vertexCollections) {
        try {
            if (!db.graph(graphName).exists()) {
                com.arangodb.entity.EdgeDefinition edgeDef = new com.arangodb.entity.EdgeDefinition()
                        .collection(edgeCollection)
                        .from(vertexCollections)
                        .to(vertexCollections);

                db.createGraph(graphName, Collections.singletonList(edgeDef));
                log.info("Created graph: {}", graphName);
            }
        } catch (ArangoDBException e) {
            log.error("Failed to create graph: {}", graphName, e);
        }
    }

    private Map<Long, String> syncObjectNodes(ArangoDatabase db, List<OntologyObjectDict> objectDicts,
            Map<Long, List<OntologyObjectSynonyms>> synonymsMap, Long workspaceId) {
        ArangoCollection collection = db.collection("ObjectNodes");
        log.info("Syncing {} ObjectNodes...", objectDicts.size());

        Map<Long, String> idToKeyMap = new HashMap<>();
        List<Map<String, Object>> batch = new ArrayList<>();

        for (OntologyObjectDict dict : objectDicts) {
            // Generate Readable Key: term_en (sanitized) + "_" + id
            String termEn = dict.getTermEn() != null ? dict.getTermEn() : "Unknown";
            String sanitizedTerm = termEn.replaceAll("[^a-zA-Z0-9_\\-:.@()+,=;$!*'%]", "_");
            String key = sanitizedTerm + "_" + dict.getId();
            idToKeyMap.put(dict.getId(), key);

            List<OntologyObjectSynonyms> synonyms = synonymsMap.getOrDefault(dict.getId(), Collections.emptyList());
            Map<String, List<String>> synMap = synonyms.stream()
                    .collect(Collectors.groupingBy(
                            OntologyObjectSynonyms::getLanguage,
                            Collectors.mapping(OntologyObjectSynonyms::getSynonym, Collectors.toList())));

            Map<String, Object> doc = new HashMap<>();
            doc.put("_key", key);
            doc.put("term_en", dict.getTermEn());
            doc.put("term_ko", dict.getTermKo());
            doc.put("category", dict.getCategory());
            doc.put("description", dict.getDescription());
            doc.put("label_ko", dict.getTermKo());
            doc.put("label_en", dict.getTermEn());
            doc.put("workspace_id", workspaceId);
            doc.put("synonyms_en", synMap.getOrDefault("en", Collections.emptyList()));
            doc.put("synonyms_ko", synMap.getOrDefault("ko", Collections.emptyList()));

            batch.add(doc);
            if (batch.size() >= 1000) {
                collection.importDocuments(batch, new com.arangodb.model.DocumentImportOptions()
                        .onDuplicate(com.arangodb.model.DocumentImportOptions.OnDuplicate.replace));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            collection.importDocuments(batch, new com.arangodb.model.DocumentImportOptions()
                    .onDuplicate(com.arangodb.model.DocumentImportOptions.OnDuplicate.replace));
        }
        return idToKeyMap;
    }

    private void syncRelationNodes(ArangoDatabase db, List<OntologyRelationDict> relationDicts,
            Map<Long, List<OntologyRelationSynonyms>> synonymsMap, Long workspaceId) {
        ArangoCollection collection = db.collection("RelationNodes");
        log.info("Syncing {} RelationNodes...", relationDicts.size());

        List<Map<String, Object>> batch = new ArrayList<>();
        for (OntologyRelationDict dict : relationDicts) {
            List<OntologyRelationSynonyms> synonyms = synonymsMap.getOrDefault(dict.getId(), Collections.emptyList());
            Map<String, List<String>> synMap = synonyms.stream()
                    .collect(Collectors.groupingBy(
                            OntologyRelationSynonyms::getLanguage,
                            Collectors.mapping(OntologyRelationSynonyms::getSynonym, Collectors.toList())));

            Map<String, Object> doc = new HashMap<>();
            doc.put("_key", String.valueOf(dict.getId()));
            doc.put("relation_en", dict.getRelationEn());
            doc.put("relation_ko", dict.getRelationKo());
            doc.put("category", dict.getCategory());
            doc.put("description", dict.getDescription());
            doc.put("label_ko", dict.getRelationKo());
            doc.put("label_en", dict.getRelationEn());
            doc.put("workspace_id", workspaceId);
            doc.put("synonyms_en", synMap.getOrDefault("en", Collections.emptyList()));
            doc.put("synonyms_ko", synMap.getOrDefault("ko", Collections.emptyList()));

            batch.add(doc);
            if (batch.size() >= 1000) {
                collection.importDocuments(batch, new com.arangodb.model.DocumentImportOptions()
                        .onDuplicate(com.arangodb.model.DocumentImportOptions.OnDuplicate.replace));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            collection.importDocuments(batch, new com.arangodb.model.DocumentImportOptions()
                    .onDuplicate(com.arangodb.model.DocumentImportOptions.OnDuplicate.replace));
        }
    }

    private void syncKnowlearnEdges(ArangoDatabase db, Long workspaceId,
            List<OntologyObjectDict> objectDicts,
            List<OntologyRelationDict> relationDicts,
            Map<Long, List<OntologyObjectSynonyms>> objectSynonymsMap,
            Map<Long, List<OntologyRelationSynonyms>> relationSynonymsMap,
            Map<Long, String> objectIdToKeyMap) {

        List<OntologyKnowlearnType> triples = knowlearnTypeRepository.findByWorkspaceId(workspaceId);
        ArangoCollection collection = db.collection("KnowlearnEdges");
        log.info("Syncing {} KnowlearnEdges...", triples.size());

        // Lookup Maps
        Map<Long, OntologyObjectDict> objectMap = objectDicts.stream()
                .collect(Collectors.toMap(OntologyObjectDict::getId, Function.identity()));
        Map<Long, OntologyRelationDict> relationMap = relationDictRepository.findByWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(OntologyRelationDict::getId, Function.identity()));

        // Chunk ID -> Document ID Map (Bulk Fetch)
        Map<Long, Long> chunkToDocMap = new HashMap<>();
        List<Object[]> chunkDocs = documentChunkRepository.findAllChunkIdAndDocumentId();
        for (Object[] row : chunkDocs) {
            chunkToDocMap.put((Long) row[0], (Long) row[1]);
        }

        List<Map<String, Object>> batch = new ArrayList<>();
        for (OntologyKnowlearnType triple : triples) {
            Map<String, Object> edge = new HashMap<>();

            String edgeKey = String.valueOf(triple.getId());
            // Use Readable Keys for From/To
            String subjectKey = objectIdToKeyMap.get(triple.getSubjectId());
            String objectKey = objectIdToKeyMap.get(triple.getObjectId());

            if (subjectKey == null || objectKey == null) {
                log.warn("Skipping edge {} due to missing subject/object key", triple.getId());
                continue;
            }

            edge.put("_key", edgeKey);
            edge.put("workspace_id", workspaceId);
            edge.put("_from", "ObjectNodes/" + subjectKey);
            edge.put("_to", "ObjectNodes/" + objectKey);

            // Basic Info
            edge.put("relationId", triple.getRelationId());
            edge.put("confidenceScore", triple.getConfidenceScore());
            edge.put("evidenceLevel", triple.getEvidenceLevel());

            // Enrich: Join Subject, Relation, Object
            OntologyObjectDict subject = objectMap.get(triple.getSubjectId());
            OntologyObjectDict object = objectMap.get(triple.getObjectId());
            OntologyRelationDict relation = relationMap.get(triple.getRelationId());

            if (subject != null && object != null && relation != null) {
                // Label Construction (Use Relation Name for Graph Label)
                edge.put("label_ko", relation.getRelationKo());
                edge.put("label_en", relation.getRelationEn());

                // Relation Info
                edge.put("relation_ko", relation.getRelationKo());
                edge.put("relation_en", relation.getRelationEn());

                // Subject & Object Info
                edge.put("subject_term_ko", subject.getTermKo());
                edge.put("subject_term_en", subject.getTermEn());
                edge.put("subject_id", subject.getId());

                edge.put("object_term_ko", object.getTermKo());
                edge.put("object_term_en", object.getTermEn());
                edge.put("object_id", object.getId());

                // Composite Sentence
                edge.put("sentence_ko",
                        String.format("%s %s %s", subject.getTermKo(), relation.getRelationKo(), object.getTermKo()));
                edge.put("sentence_en",
                        String.format("%s %s %s", subject.getTermEn(), relation.getRelationEn(), object.getTermEn()));

                // Synonyms Enrichment
                List<String> subjSynKo = getSynonyms(objectSynonymsMap, subject.getId(), "ko");
                List<String> subjSynEn = getSynonyms(objectSynonymsMap, subject.getId(), "en");

                List<String> objSynKo = getSynonyms(objectSynonymsMap, object.getId(), "ko");
                List<String> objSynEn = getSynonyms(objectSynonymsMap, object.getId(), "en");

                List<String> relSynKo = getSynonyms(relationSynonymsMap, relation.getId(), "ko");
                List<String> relSynEn = getSynonyms(relationSynonymsMap, relation.getId(), "en");

                edge.put("subject_synonyms_ko", subjSynKo);
                edge.put("subject_synonyms_en", subjSynEn);
                edge.put("object_synonyms_ko", objSynKo);
                edge.put("object_synonyms_en", objSynEn);
                edge.put("relation_synonyms_ko", relSynKo);
                edge.put("relation_synonyms_en", relSynEn);
                edge.put("relation_synonyms_ko", relSynKo);
                edge.put("relation_synonyms_en", relSynEn);

                // Source & Document IDs Mapping
                try {
                    String sourceJson = triple.getSource();
                    if (sourceJson != null && !sourceJson.isEmpty()) {
                        List<String> chunkIdsStr = objectMapper.readValue(sourceJson,
                                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                                });
                        List<Long> chunkIds = chunkIdsStr.stream().map(Long::valueOf).collect(Collectors.toList());
                        Set<Long> docIds = new HashSet<>();
                        for (Long cId : chunkIds) {
                            Long dId = chunkToDocMap.get(cId);
                            if (dId != null) {
                                docIds.add(dId);
                            }
                        }
                        edge.put("chunk_ids", chunkIds); // Original Source (Chunks)
                        edge.put("document_ids", new ArrayList<>(docIds)); // Derived Source (Documents)
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse source or map document IDs for edge {}: {}", triple.getId(),
                            e.getMessage());
                }
            }

            batch.add(edge);
            if (batch.size() >= 1000) {
                collection.importDocuments(batch, new com.arangodb.model.DocumentImportOptions()
                        .onDuplicate(com.arangodb.model.DocumentImportOptions.OnDuplicate.replace));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            collection.importDocuments(batch, new com.arangodb.model.DocumentImportOptions()
                    .onDuplicate(com.arangodb.model.DocumentImportOptions.OnDuplicate.replace));
        }
    }

    private List<String> getSynonyms(Map<Long, ? extends List<? extends Object>> map, Long id, String lang) {
        List<?> list = map.get(id);
        if (list == null)
            return Collections.emptyList();

        return list.stream()
                .filter(s -> {
                    if (s instanceof OntologyObjectSynonyms)
                        return ((OntologyObjectSynonyms) s).getLanguage().equals(lang);
                    if (s instanceof OntologyRelationSynonyms)
                        return ((OntologyRelationSynonyms) s).getLanguage().equals(lang);
                    return false;
                })
                .map(s -> {
                    if (s instanceof OntologyObjectSynonyms)
                        return ((OntologyObjectSynonyms) s).getSynonym();
                    if (s instanceof OntologyRelationSynonyms)
                        return ((OntologyRelationSynonyms) s).getSynonym();
                    return "";
                })
                .collect(Collectors.toList());
    }
}
