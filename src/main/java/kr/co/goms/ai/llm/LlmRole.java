/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm;

/**
 * LLM 대화 메시지의 역할을 나타냅니다.
 *
 * <p>
 * GomsBook AI Agent는 Provider와 무관한 공통 역할 모델을 사용합니다.
 * 각 LLM Provider 구현체는 이 값을 OpenAI, Gemini, Claude,
 * Ollama 등의 Provider별 메시지 형식으로 변환합니다.
 * </p>
 */
public enum LlmRole {

    /**
     * 최상위 시스템 지침입니다.
     *
     * <p>
     * 보안 정책, Agent 역할, 출력 형식, Tool 사용 규칙처럼
     * 사용자 입력보다 높은 우선순위를 가져야 하는 지침에 사용합니다.
     * </p>
     */
    SYSTEM,

    /**
     * 애플리케이션 또는 개발자 수준의 지침입니다.
     *
     * <p>
     * GomsBookEditor 내부 규칙, XHTML 생성 규칙,
     * 프로젝트 처리 정책 등에 사용할 수 있습니다.
     * </p>
     *
     * <p>
     * 일부 Provider가 DEVELOPER 역할을 직접 지원하지 않으면,
     * Provider Adapter에서 SYSTEM 메시지로 병합할 수 있습니다.
     * </p>
     */
    DEVELOPER,

    /**
     * 사용자가 입력한 요청입니다.
     */
    USER,

    /**
     * LLM이 생성한 응답입니다.
     */
    ASSISTANT,

    /**
     * Tool 실행 결과를 LLM에 다시 전달할 때 사용합니다.
     */
    TOOL;

    /**
     * 현재 역할이 사용자 입력인지 확인합니다.
     *
     * @return USER이면 true
     */
    public boolean isUser() {
        return this == USER;
    }

    /**
     * 현재 역할이 LLM 응답인지 확인합니다.
     *
     * @return ASSISTANT이면 true
     */
    public boolean isAssistant() {
        return this == ASSISTANT;
    }

    /**
     * 현재 역할이 시스템 또는 애플리케이션 지침인지 확인합니다.
     *
     * @return SYSTEM 또는 DEVELOPER이면 true
     */
    public boolean isInstruction() {
        return this == SYSTEM || this == DEVELOPER;
    }

    /**
     * 현재 역할이 Tool 실행 결과인지 확인합니다.
     *
     * @return TOOL이면 true
     */
    public boolean isTool() {
        return this == TOOL;
    }

    /**
     * Provider가 DEVELOPER 역할을 지원하지 않을 때 사용할
     * 기본 대체 역할을 반환합니다.
     *
     * @return DEVELOPER이면 SYSTEM, 나머지는 현재 역할
     */
    public LlmRole fallbackRole() {
        return this == DEVELOPER
                ? SYSTEM
                : this;
    }
}