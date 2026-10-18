/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.hash;

/**
 * 해시 알고리즘 초기화 또는 해시 생성 과정에서 발생한 예외입니다.
 */
public class HashException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String algorithm;
    private final HashOperation operation;

    public HashException(
        String message
    ) {
        this(
            message,
            "",
            HashOperation.UNKNOWN,
            null
        );
    }

    public HashException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            "",
            HashOperation.UNKNOWN,
            cause
        );
    }

    public HashException(
        String message,
        String algorithm,
        HashOperation operation
    ) {
        this(
            message,
            algorithm,
            operation,
            null
        );
    }

    public HashException(
        String message,
        String algorithm,
        HashOperation operation,
        Throwable cause
    ) {
        super(message, cause);

        this.algorithm =
            algorithm == null
                ? ""
                : algorithm.trim();

        this.operation =
            operation == null
                ? HashOperation.UNKNOWN
                : operation;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public boolean hasAlgorithm() {
        return !algorithm.isBlank();
    }

    public HashOperation getOperation() {
        return operation;
    }
}