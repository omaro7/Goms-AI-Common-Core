/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.pipeline;

/**
 * RAG 검색, 프롬프트 증강 또는 LLM 호출 과정에서 발생한 예외입니다.
 */
public class RagPipelineException
    extends Exception {

    private static final long serialVersionUID = 1L;

    private final RagPipelineOperation operation;
    private final String query;
    private final String chatModel;

    public RagPipelineException(
        String message
    ) {
        this(
            message,
            RagPipelineOperation.UNKNOWN,
            "",
            "",
            null
        );
    }

    public RagPipelineException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            RagPipelineOperation.UNKNOWN,
            "",
            "",
            cause
        );
    }

    public RagPipelineException(
        String message,
        RagPipelineOperation operation
    ) {
        this(
            message,
            operation,
            "",
            "",
            null
        );
    }

    public RagPipelineException(
        String message,
        RagPipelineOperation operation,
        String query,
        String chatModel,
        Throwable cause
    ) {
        super(message, cause);

        this.operation =
            operation == null
                ? RagPipelineOperation.UNKNOWN
                : operation;

        this.query = normalize(query);
        this.chatModel =
            normalize(chatModel);
    }

    public RagPipelineOperation getOperation() {
        return operation;
    }

    public String getQuery() {
        return query;
    }

    public boolean hasQuery() {
        return !query.isBlank();
    }

    public String getChatModel() {
        return chatModel;
    }

    public boolean hasChatModel() {
        return !chatModel.isBlank();
    }

    private static String normalize(
        String value
    ) {
        return value == null
            ? ""
            : value.trim();
    }
}