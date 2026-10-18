/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.runtime;

/**
 * RagRuntime 초기화, 프로젝트 관리, 인덱싱 또는 실행 과정에서
 * 발생하는 예외입니다.
 */
public class RagRuntimeException extends Exception {

    private static final long serialVersionUID = 1L;

    private final RagRuntimeOperation operation;
    private final RagRuntimeState runtimeState;

    public RagRuntimeException(
        String message
    ) {
        this(
            message,
            RagRuntimeOperation.UNKNOWN,
            null,
            null
        );
    }

    public RagRuntimeException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            RagRuntimeOperation.UNKNOWN,
            null,
            cause
        );
    }

    public RagRuntimeException(
        String message,
        RagRuntimeOperation operation,
        RagRuntimeState runtimeState
    ) {
        this(
            message,
            operation,
            runtimeState,
            null
        );
    }

    public RagRuntimeException(
        String message,
        RagRuntimeOperation operation,
        RagRuntimeState runtimeState,
        Throwable cause
    ) {
        super(message, cause);

        this.operation =
            operation == null
                ? RagRuntimeOperation.UNKNOWN
                : operation;

        this.runtimeState =
            runtimeState;
    }

    public RagRuntimeOperation getOperation() {
        return operation;
    }

    public RagRuntimeState getRuntimeState() {
        return runtimeState;
    }

    public boolean hasRuntimeState() {
        return runtimeState != null;
    }
}