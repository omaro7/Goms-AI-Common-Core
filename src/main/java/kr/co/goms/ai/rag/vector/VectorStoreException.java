/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.vector;

/**
 * VectorStore 저장, 조회, 검색 또는 삭제 과정에서 발생한 예외입니다.
 */
public class VectorStoreException extends Exception {

    private static final long serialVersionUID = 1L;

    private final VectorStoreOperation operation;
    private final String recordId;
    private final String model;

    public VectorStoreException(String message) {
        this(
            message,
            VectorStoreOperation.UNKNOWN,
            "",
            "",
            null
        );
    }

    public VectorStoreException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            VectorStoreOperation.UNKNOWN,
            "",
            "",
            cause
        );
    }

    public VectorStoreException(
        String message,
        VectorStoreOperation operation
    ) {
        this(
            message,
            operation,
            "",
            "",
            null
        );
    }

    public VectorStoreException(
        String message,
        VectorStoreOperation operation,
        Throwable cause
    ) {
        this(
            message,
            operation,
            "",
            "",
            cause
        );
    }

    public VectorStoreException(
        String message,
        VectorStoreOperation operation,
        String recordId,
        String model
    ) {
        this(
            message,
            operation,
            recordId,
            model,
            null
        );
    }

    public VectorStoreException(
        String message,
        VectorStoreOperation operation,
        String recordId,
        String model,
        Throwable cause
    ) {
        super(message, cause);

        this.operation =
            operation == null
                ? VectorStoreOperation.UNKNOWN
                : operation;

        this.recordId = normalize(recordId);
        this.model = normalize(model);
    }

    public VectorStoreOperation getOperation() {
        return operation;
    }

    public String getRecordId() {
        return recordId;
    }

    public boolean hasRecordId() {
        return !recordId.isBlank();
    }

    public String getModel() {
        return model;
    }

    public boolean hasModel() {
        return !model.isBlank();
    }

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.trim();
    }
}