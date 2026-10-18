/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

/**
 * RAG 인덱싱 중 발생한 경고 또는 오류입니다.
 */
public final class RagIndexIssue {

    private final RagIndexIssueSeverity severity;
    private final String message;
    private final String chunkId;

    private RagIndexIssue(
        RagIndexIssueSeverity severity,
        String message,
        String chunkId
    ) {
        this.severity =
            severity == null
                ? RagIndexIssueSeverity.ERROR
                : severity;

        this.message =
            requireText(
                message,
                "message"
            );

        this.chunkId =
            normalize(chunkId);
    }

    public static RagIndexIssue warning(
        String message
    ) {
        return new RagIndexIssue(
            RagIndexIssueSeverity.WARNING,
            message,
            ""
        );
    }

    public static RagIndexIssue warning(
        String message,
        String chunkId
    ) {
        return new RagIndexIssue(
            RagIndexIssueSeverity.WARNING,
            message,
            chunkId
        );
    }

    public static RagIndexIssue error(
        String message
    ) {
        return new RagIndexIssue(
            RagIndexIssueSeverity.ERROR,
            message,
            ""
        );
    }

    public static RagIndexIssue error(
        String message,
        String chunkId
    ) {
        return new RagIndexIssue(
            RagIndexIssueSeverity.ERROR,
            message,
            chunkId
        );
    }

    public RagIndexIssueSeverity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getChunkId() {
        return chunkId;
    }

    public boolean hasChunkId() {
        return !chunkId.isBlank();
    }

    private static String requireText(
        String value,
        String fieldName
    ) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }

        return normalized;
    }

    private static String normalize(
        String value
    ) {
        return value == null
            ? ""
            : value.trim();
    }
}