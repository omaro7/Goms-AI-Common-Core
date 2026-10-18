/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm;

/**
 * LLM Client 또는 Provider를 현재 사용할 수 없을 때 발생합니다.
 */
public final class LlmUnavailableException extends LlmException {

    private static final long serialVersionUID = 1L;

    private final String clientName;

    public LlmUnavailableException(
            String clientName
    ) {
        super(
                "LLM client is unavailable: "
                        + String.valueOf(clientName)
        );

        this.clientName = clientName;
    }

    public String getClientName() {
        return clientName;
    }
}