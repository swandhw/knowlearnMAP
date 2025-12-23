package com.knowlearnmap.pipeline.core;

/**
 * Pipeline stages enum defining all stages in the hybrid RAG pipeline.
 * 
 * <p>
 * Pipeline has two independent execution paths after chunking:
 * </p>
 * <ul>
 * <li><strong>Path 1 (Vector):</strong> WORKSPACE → UPLOAD → PARSE → CHUNK →
 * VECTORIZE (PostgreSQL) → END</li>
 * <li><strong>Path 2 (Ontology):</strong> WORKSPACE → UPLOAD → PARSE → CHUNK →
 * ONTOLOGY → ARANGO_SYNC → EMBED → END</li>
 * </ul>
 * 
 * <p>
 * VECTORIZE and ONTOLOGY run in parallel but are independent completion paths.
 * </p>
 */
public enum PipelineStage {
    WORKSPACE(0, "Workspace Setup", false, false), // Workspace 생성/검증
    UPLOAD(1, "Document Upload", false, false), // Document 업로드 (Workspace에 종속)
    PARSE(2, "PDF Parsing", false, false),
    CHUNK(3, "LLM Chunking", false, false),
    VECTORIZE(4, "Vector Embedding to PostgreSQL", true, true), // Parallel, Terminal
    ONTOLOGY(4, "Ontology Extraction", true, false), // Parallel, continues to ARANGO_SYNC
    ARANGO_SYNC(5, "ArangoDB Sync", false, false),
    EMBED(6, "ArangoDB Vectorization", false, true); // Terminal

    private final int order;
    private final String displayName;
    private final boolean parallelExecutable;
    private final boolean terminalStage;

    PipelineStage(int order, String displayName, boolean parallelExecutable, boolean terminalStage) {
        this.order = order;
        this.displayName = displayName;
        this.parallelExecutable = parallelExecutable;
        this.terminalStage = terminalStage;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isParallelExecutable() {
        return parallelExecutable;
    }

    public boolean isTerminalStage() {
        return terminalStage;
    }
}
