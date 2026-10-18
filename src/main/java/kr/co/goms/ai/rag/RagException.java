/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag;

/**
 * RAG 검색, 컨텍스트 생성 또는 프롬프트 증강 과정에서 발생한 예외입니다.
 */
public class RagException extends Exception {

    private static final long serialVersionUID = 1L;

    private final RagOperation operation;
    private final String query;

    public RagException(
        String message
    ) {
        this(
            message,
            RagOperation.UNKNOWN,
            "",
            null
        );
    }

    public RagException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            RagOperation.UNKNOWN,
            "",
            cause
        );
    }

    public RagException(
        String message,
        RagOperation operation
    ) {
        this(
            message,
            operation,
            "",
            null
        );
    }

    public RagException(
        String message,
        RagOperation operation,
        String query,
        Throwable cause
    ) {
        super(message, cause);

        this.operation =
            operation == null
                ? RagOperation.UNKNOWN
                : operation;

        this.query =
            query == null
                ? ""
                : query.trim();
    }

    public RagOperation getOperation() {
        return operation;
    }

    public String getQuery() {
        return query;
    }

    public boolean hasQuery() {
        return !query.isBlank();
    }
}