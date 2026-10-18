/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.embedding;

/**
 * 임베딩 요청, 통신, 응답 변환 또는 벡터 검증 과정에서 발생한 예외입니다.
 */
public class EmbeddingException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * 요청에 사용된 임베딩 모델명.
     */
    private final String model;

    /**
     * 요청 추적 식별자.
     */
    private final String requestId;

    /**
     * HTTP 상태 코드.
     *
     * HTTP 오류가 아닌 경우 0입니다.
     */
    private final int statusCode;

    /**
     * 재시도 가능한 오류인지 여부.
     */
    private final boolean retryable;

    public EmbeddingException(String message) {
        this(
            message,
            "",
            "",
            0,
            false,
            null
        );
    }

    public EmbeddingException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            "",
            "",
            0,
            false,
            cause
        );
    }

    public EmbeddingException(
        String message,
        String model
    ) {
        this(
            message,
            model,
            "",
            0,
            false,
            null
        );
    }

    public EmbeddingException(
        String message,
        String model,
        String requestId
    ) {
        this(
            message,
            model,
            requestId,
            0,
            false,
            null
        );
    }

    public EmbeddingException(
        String message,
        String model,
        String requestId,
        Throwable cause
    ) {
        this(
            message,
            model,
            requestId,
            0,
            false,
            cause
        );
    }

    public EmbeddingException(
        String message,
        String model,
        String requestId,
        int statusCode,
        boolean retryable
    ) {
        this(
            message,
            model,
            requestId,
            statusCode,
            retryable,
            null
        );
    }

    public EmbeddingException(
        String message,
        String model,
        String requestId,
        int statusCode,
        boolean retryable,
        Throwable cause
    ) {
        super(message, cause);

        if (statusCode < 0) {
            throw new IllegalArgumentException(
                "statusCode must be greater than or equal to zero"
            );
        }

        this.model = normalize(model);
        this.requestId = normalize(requestId);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public String getModel() {
        return model;
    }

    public boolean hasModel() {
        return !model.isBlank();
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean hasRequestId() {
        return !requestId.isBlank();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean hasStatusCode() {
        return statusCode > 0;
    }

    public boolean isRetryable() {
        return retryable;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}