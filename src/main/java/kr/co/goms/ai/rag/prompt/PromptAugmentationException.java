/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.prompt;

/**
 * 사용자 프롬프트와 RAG 컨텍스트를 결합하는 과정에서 발생한
 * 예외입니다.
 */
public class PromptAugmentationException
    extends Exception {

    private static final long serialVersionUID = 1L;

    private final PromptAugmentationOperation operation;

    private final String query;

    public PromptAugmentationException(
        String message
    ) {
        this(
            message,
            PromptAugmentationOperation.UNKNOWN,
            "",
            null
        );
    }

    public PromptAugmentationException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            PromptAugmentationOperation.UNKNOWN,
            "",
            cause
        );
    }

    public PromptAugmentationException(
        String message,
        PromptAugmentationOperation operation
    ) {
        this(
            message,
            operation,
            "",
            null
        );
    }

    public PromptAugmentationException(
        String message,
        PromptAugmentationOperation operation,
        Throwable cause
    ) {
        this(
            message,
            operation,
            "",
            cause
        );
    }

    public PromptAugmentationException(
        String message,
        PromptAugmentationOperation operation,
        String query,
        Throwable cause
    ) {
        super(message, cause);

        this.operation =
            operation == null
                ? PromptAugmentationOperation.UNKNOWN
                : operation;

        this.query =
            query == null
                ? ""
                : query.trim();
    }

    public PromptAugmentationOperation getOperation() {
        return operation;
    }

    public String getQuery() {
        return query;
    }

    public boolean hasQuery() {
        return !query.isBlank();
    }
}