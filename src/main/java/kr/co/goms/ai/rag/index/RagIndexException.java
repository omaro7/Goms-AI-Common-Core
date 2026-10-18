/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

/**
 * 문서 분할, 임베딩 생성 또는 VectorStore 저장 중 발생한 예외입니다.
 */
public class RagIndexException extends Exception {

    private static final long serialVersionUID = 1L;

    private final RagIndexOperation operation;
    private final String sourcePath;
    private final String chunkId;
    private final String model;

    public RagIndexException(
        String message
    ) {
        this(
            message,
            RagIndexOperation.UNKNOWN,
            "",
            "",
            "",
            null
        );
    }

    public RagIndexException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            RagIndexOperation.UNKNOWN,
            "",
            "",
            "",
            cause
        );
    }

    public RagIndexException(
        String message,
        RagIndexOperation operation,
        String sourcePath,
        String chunkId,
        String model,
        Throwable cause
    ) {
        super(message, cause);

        this.operation =
            operation == null
                ? RagIndexOperation.UNKNOWN
                : operation;

        this.sourcePath =
            normalize(sourcePath);

        this.chunkId =
            normalize(chunkId);

        this.model =
            normalize(model);
    }

    public RagIndexOperation getOperation() {
        return operation;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public boolean hasSourcePath() {
        return !sourcePath.isBlank();
    }

    public String getChunkId() {
        return chunkId;
    }

    public boolean hasChunkId() {
        return !chunkId.isBlank();
    }

    public String getModel() {
        return model;
    }

    public boolean hasModel() {
        return !model.isBlank();
    }

    private static String normalize(
        String value
    ) {
        return value == null
            ? ""
            : value.trim();
    }
}