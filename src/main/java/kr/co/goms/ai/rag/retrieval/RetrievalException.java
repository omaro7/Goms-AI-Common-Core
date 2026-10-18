/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.retrieval;

/**
 * 질의 임베딩 또는 VectorStore 검색 과정에서 발생한 예외입니다.
 */
public class RetrievalException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String query;
    private final String model;
    private final RetrievalOperation operation;

    public RetrievalException(String message) {
        this(
            message,
            "",
            "",
            RetrievalOperation.UNKNOWN,
            null
        );
    }

    public RetrievalException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            "",
            "",
            RetrievalOperation.UNKNOWN,
            cause
        );
    }

    public RetrievalException(
        String message,
        String query,
        String model,
        RetrievalOperation operation,
        Throwable cause
    ) {
        super(message, cause);

        this.query = normalize(query);
        this.model = normalize(model);

        this.operation =
            operation == null
                ? RetrievalOperation.UNKNOWN
                : operation;
    }

    public String getQuery() {
        return query;
    }

    public boolean hasQuery() {
        return !query.isBlank();
    }

    public String getModel() {
        return model;
    }

    public boolean hasModel() {
        return !model.isBlank();
    }

    public RetrievalOperation getOperation() {
        return operation;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}