/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm;

/**
 * LLM Framework에서 발생하는 공통 예외입니다.
 */
public class LlmException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LlmException(
            String message
    ) {
        super(message);
    }

    public LlmException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }
}