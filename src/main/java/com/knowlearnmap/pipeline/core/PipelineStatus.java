package com.knowlearnmap.pipeline.core;

/**
 * Pipeline execution status.
 */
public enum PipelineStatus {
    PENDING("대기 중"),
    PROCESSING("처리 중"),
    COMPLETED("완료"),
    FAILED("실패"),
    CANCELLED("취소됨");

    private final String displayName;

    PipelineStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
