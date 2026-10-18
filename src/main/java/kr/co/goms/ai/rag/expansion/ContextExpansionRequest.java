/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.expansion;

import java.util.Collections;
import java.util.List;

import kr.co.goms.ai.rag.retrieval.RetrievedDocument;

/**
 * Context Expansion 요청 모델입니다.
 */
public final class ContextExpansionRequest {

    private final String projectId;
    private final List<RetrievedDocument> retrievedDocuments;
    private final int beforeChunks;
    private final int afterChunks;

    public ContextExpansionRequest(String projectId, List<RetrievedDocument> retrievedDocuments, int beforeChunks, int afterChunks) {

        if (projectId == null || projectId.trim().isEmpty()) {
            throw new IllegalArgumentException("projectId must not be blank");
        }

        if (beforeChunks < 0) {
            throw new IllegalArgumentException("beforeChunks must be >= 0");
        }

        if (afterChunks < 0) {
            throw new IllegalArgumentException("afterChunks must be >= 0");
        }

        this.projectId = projectId.trim();
        this.retrievedDocuments = retrievedDocuments == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(List.copyOf(retrievedDocuments));
        this.beforeChunks = beforeChunks;
        this.afterChunks = afterChunks;
    }

    public String getProjectId() {
        return projectId;
    }

    public List<RetrievedDocument> getRetrievedDocuments() {
        return retrievedDocuments;
    }

    public int getBeforeChunks() {
        return beforeChunks;
    }

    public int getAfterChunks() {
        return afterChunks;
    }
}