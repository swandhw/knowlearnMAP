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

    private static final int BATCH_SIZE = 50;

    private final WorkspaceRepository workspaceRepository;
    private final OntologyObjectDictRepository objectDictRepository;
    private final OntologyRelationDictRepository relationDictRepository;
    private final OntologyKnowlearnTypeRepository knowlearnTypeRepository;
    private final OntologyObjectSynonymsRepository objectSynonymsRepository;
    private final OntologyRelationSynonymsRepository relationSynonymsRepository;
    private final com.knowlearnmap.document.repository.DocumentChunkRepository documentChunkRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final ArangoDB arangoDB;
    private final com.knowlearnmap.ai.service.EmbeddingService embeddingService;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    public void syncOntologyToArango(Long workspaceId, boolean dropExist) {
        // 1. Fetch all data from PostgreSQL (Short-lived Transaction)
        // Use TransactionTemplate to ensure this runs in a transaction despite being an
        // internal call
        OntologyData data = transactionTemplate.execute(status -> fetchAllOntologyData(workspaceId));

        // 2. ArangoDB Setup & Sync (No DB Transaction needed here)
        if (data != null) {
            processArangoSync(data, workspaceId, dropExist);

            // 3. Update Sync Status
            WorkspaceEntity workspace = workspaceRepository.findById(workspaceId).orElseThrow();
            workspace.setNeedsArangoSync(false);
            workspaceRepository.save(workspace);
        }
    }

    protected OntologyData fetchAllOntologyData(Long workspaceId) {
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace " + workspaceId + "를 찾을 수 없습니다."));

        if (workspace.getDomain() == null) {
            throw new IllegalArgumentException("Workspace " + workspaceId + "에 연결된 도메인이 없습니다.");
        }

        String targetDbName = workspace.getDomain().getArangoDbName();
        // Initialize lazy loaded property inside transaction
        if (targetDbName == null || targetDbName.isEmpty()) {
            throw new IllegalArgumentException(
                    "Domain " + workspace.getDomain().getName() + "에 ArangoDB 이름이 설정되지 않았습니다.");
        }

        log.info("Fetching all data for workspace {}...", workspaceId);
        List<OntologyObjectDict> objectDicts = objectDictRepository.findByWorkspaceId(workspaceId);
        List<OntologyRelationDict> relationDicts = relationDictRepository.findByWorkspaceId(workspaceId);
        List<OntologyKnowlearnType> triples = knowlearnTypeRepository.findByWorkspaceId(workspaceId);

        // Object Synonyms Map
        Map<Long, List<OntologyObjectSynonyms>> objectSynonymsMap = new HashMap<>();
        for (OntologyObjectDict obj : objectDicts) {
            objectSynonymsMap.put(obj.getId(), objectSynonymsRepository.findByObjectId(obj.getId()));
        }

        // Relation Synonyms Map
        Map<Long, List<OntologyRelationSynonyms>> relationSynonymsMap = new HashMap<>();
        for (OntologyRelationDict rel : relationDicts) {
            relationSynonymsMap.put(rel.getId(), relationSynonymsRepository.findByRelationId(rel.getId()));
        }

        // Chunk to Doc Map
        Map<Long, Long> chunkToDocMap = new HashMap<>();
        List<Object[]> chunkDocs = documentChunkRepository.findAllChunkIdAndDocumentId();
        for (Object[] row : chunkDocs) {
            chunkToDocMap.put((Long) row[0], (Long) row[1]);
        }

        return new OntologyData(targetDbName, objectDicts, relationDicts, triples, objectSynonymsMap,
                relationSynonymsMap, chunkToDocMap);
    }

    private void processArangoSync(OntologyData data, Long workspaceId, boolean dropExist) {
        String targetDbName = data.targetDbName;

        // ArangoDB Connection Management
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

        ensureCollection(db, "ObjectNodes");
        ensureCollection(db, "RelationNodes");
        ensureEdgeCollection(db, "KnowlearnEdges");
        ensureGraph(db, "KnowlearnGraph", "KnowlearnEdges", "ObjectNodes");

        // Execute Sync
        Map<Long, String> objectIdToKeyMap = syncObjectNodes(db, data.objectDicts, data.objectSynonymsMap, workspaceId);
        syncRelationNodes(db, data.relationDicts, data.relationSynonymsMap, workspaceId);
        syncKnowlearnEdges(db, workspaceId, data.objectDicts, data.relationDicts, data.triples, data.objectSynonymsMap,
                data.relationSynonymsMap, objectIdToKeyMap, data.chunkToDocMap);

        log.info("Ontology Sync to ArangoDB Completed Successfully.");
    }

    // Internal DTO class to hold fetched data
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class OntologyData {
        String targetDbName;
        List<OntologyObjectDict> objectDicts;
        List<OntologyRelationDict> relationDicts;
        List<OntologyKnowlearnType> triples;
        Map<Long, List<OntologyObjectSynonyms>> objectSynonymsMap;
        Map<Long, List<OntologyRelationSynonyms>> relationSynonymsMap;
        Map<Long, Long> chunkToDocMap;
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
        log.info("Syncing {} ObjectNodes (Parallel)...", objectDicts.size());

        // 1. Build ID to Key Map (Fast, Sequential)
        Map<Long, String> idToKeyMap = new HashMap<>();
        for (OntologyObjectDict dict : objectDicts) {
            String termEn = dict.getTermEn() != null ? dict.getTermEn() : "Unknown";
            String sanitizedTerm = termEn.replaceAll("[^a-zA-Z0-9_\\-:.@()+,=;$!*'%]", "_");
            String key = sanitizedTerm + "_" + dict.getId();
            idToKeyMap.put(dict.getId(), key);
        }

        // 2. Process Batches in Parallel (Embedding + Insert)
        List<List<OntologyObjectDict>> partitions = partition(objectDicts, BATCH_SIZE);

        partitions.parallelStream().forEach(batchList -> {
            List<Map<String, Object>> arangoBatch = new ArrayList<>();
            for (OntologyObjectDict dict : batchList) {
                String key = idToKeyMap.get(dict.getId());

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
                doc.put("synonyms_en", synMap.getOrDefault("en", Collections.emptyList()));
                doc.put("synonyms_ko", synMap.getOrDefault("ko", Collections.emptyList()));
                doc.put("document_ids", parseJsonToList(dict.getSource()));
                doc.put("chunk_ids", parseJsonToList(dict.getChunkSource()));

                // Embedding (Slow operation - benefits from parallel)
                try {
                    String textToEmbed = String.format("%s (%s): %s",
                            dict.getTermKo(),
                            dict.getTermEn(),
                            dict.getDescription() != null ? dict.getDescription() : "");
                    List<Double> vector = embeddingService.embed(textToEmbed);
                    doc.put("embedding_vector", vector);
                } catch (Exception e) {
                    log.error("Failed to generate embedding for object {}: {}", dict.getId(), e.getMessage());
                }
                arangoBatch.add(doc);
            }

            if (!arangoBatch.isEmpty()) {
                collection.importDocuments(arangoBatch, new com.arangodb.model.DocumentImportOptions()
                        .onDuplicate(com.arangodb.model.DocumentImportOptions.OnDuplicate.replace));
            }
        });

        return idToKeyMap;
    }

    private void syncRelationNodes(ArangoDatabase db, List<OntologyRelationDict> relationDicts,
            Map<Long, List<OntologyRelationSynonyms>> synonymsMap, Long workspaceId) {
        ArangoCollection collection = db.collection("RelationNodes");
        log.info("Syncing {} RelationNodes (Parallel)...", relationDicts.size());

        List<List<OntologyRelationDict>> partitions = partition(relationDicts, BATCH_SIZE);

        partitions.parallelStream().forEach(batchList -> {
            List<Map<String, Object>> arangoBatch = new ArrayList<>();
            for (OntologyRelationDict dict : batchList) {
                List<OntologyRelationSynonyms> synonyms = synonymsMap.getOrDefault(dict.getId(),
                        Collections.emptyList());
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
                doc.put("synonyms_en", synMap.getOrDefault("en", Collections.emptyList()));
                doc.put("synonyms_ko", synMap.getOrDefault("ko", Collections.emptyList()));
                doc.put("document_ids", parseJsonToList(dict.getSource()));
                doc.put("chunk_ids", parseJsonToList(dict.getChunkSource()));

                arangoBatch.add(doc);
            }
            if (!arangoBatch.isEmpty()) {
                collection.importDocuments(arangoBatch, new com.arangodb.model.DocumentImportOptions()
                        .onDuplicate(com.arangodb.model.DocumentImportOptions.OnDuplicate.replace));
            }
        });
    }

    private void syncKnowlearnEdges(ArangoDatabase db, Long workspaceId,
            List<OntologyObjectDict> objectDicts,
            List<OntologyRelationDict> relationDicts,
            List<OntologyKnowlearnType> triples,
            Map<Long, List<OntologyObjectSynonyms>> objectSynonymsMap,
            Map<Long, List<OntologyRelationSynonyms>> relationSynonymsMap,
            Map<Long, String> objectIdToKeyMap,
            Map<Long, Long> chunkToDocMap) {

        ArangoCollection collection = db.collection("KnowlearnEdges");
        log.info("Syncing {} KnowlearnEdges (Parallel)...", triples.size());

        // Lookup Maps
        Map<Long, OntologyObjectDict> objectMap = objectDicts.stream()
                .collect(Collectors.toMap(OntologyObjectDict::getId, Function.identity()));
        Map<Long, OntologyRelationDict> relationMap = relationDicts.stream()
                .collect(Collectors.toMap(OntologyRelationDict::getId, Function.identity()));

        List<List<OntologyKnowlearnType>> partitions = partition(triples, BATCH_SIZE);

        partitions.parallelStream().forEach(batchList -> {
            List<Map<String, Object>> arangoBatch = new ArrayList<>();
            for (OntologyKnowlearnType triple : batchList) {
                Map<String, Object> edge = new HashMap<>();

                String edgeKey = String.valueOf(triple.getId());
                // Use Readable Keys for From/To
                String subjectKey = objectIdToKeyMap.get(triple.getSubjectId());
                String objectKey = objectIdToKeyMap.get(triple.getObjectId());

                if (subjectKey == null || objectKey == null) {
                    continue; // Skip silently or log debug
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
                    String sentenceKo = String.format("%s %s %s", subject.getTermKo(), relation.getRelationKo(),
                            object.getTermKo());
                    String sentenceEn = String.format("%s %s %s", subject.getTermEn(), relation.getRelationEn(),
                            object.getTermEn());

                    edge.put("sentence_ko", sentenceKo);
                    edge.put("sentence_en", sentenceEn);

                    // Embedding Generation for Edge (Fact)
                    try {
                        // Use Korean sentence for embedding as it is the primary language
                        // This uses parallel stream, so it runs concurrently!
                        if (sentenceKo != null && !sentenceKo.trim().isEmpty()) {
                            List<Double> vector = embeddingService.embed(sentenceKo);
                            edge.put("embedding_vector", vector);
                        }
                    } catch (Exception e) {
                        log.error("Failed to generate embedding for edge {}: {}", triple.getId(), e.getMessage());
                    }

                    // Synonyms Enrichment
                    edge.put("subject_synonyms_ko", getSynonyms(objectSynonymsMap, subject.getId(), "ko"));
                    edge.put("subject_synonyms_en", getSynonyms(objectSynonymsMap, subject.getId(), "en"));
                    edge.put("object_synonyms_ko", getSynonyms(objectSynonymsMap, object.getId(), "ko"));
                    edge.put("object_synonyms_en", getSynonyms(objectSynonymsMap, object.getId(), "en"));
                    edge.put("relation_synonyms_ko", getSynonyms(relationSynonymsMap, relation.getId(), "ko"));
                    edge.put("relation_synonyms_en", getSynonyms(relationSynonymsMap, relation.getId(), "en"));

                    // Source & Document IDs Mapping
                    // Source & Document IDs Mapping
                    edge.put("document_ids", parseJsonToList(triple.getSource()));
                    edge.put("chunk_ids", parseJsonToList(triple.getChunkSource()));
                }
                arangoBatch.add(edge);
            }

            if (!arangoBatch.isEmpty()) {
                collection.importDocuments(arangoBatch, new com.arangodb.model.DocumentImportOptions()
                        .onDuplicate(com.arangodb.model.DocumentImportOptions.OnDuplicate.replace));
            }
        });
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
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

    private List<String> parseJsonToList(String sourceJson) {
        if (sourceJson == null || sourceJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(sourceJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });
        } catch (Exception e) {
            // Try to parse as single value if list parsing fails
            try {
                String single = objectMapper.readValue(sourceJson, String.class);
                return Collections.singletonList(single);
            } catch (Exception ex) {
                // If it's a raw number not in quotes
                try {
                    Long id = objectMapper.readValue(sourceJson, Long.class);
                    return Collections.singletonList(String.valueOf(id));
                } catch (Exception ex2) {
                    // Fallback: Treat as raw string identifier (e.g. "initial_data")
                    return Collections.singletonList(sourceJson);
                }
            }
        }
    }
}
