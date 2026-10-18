/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent;

/**
 * AI Agent 실행 과정에서 발생하는 예외입니다.
 *
 * <p>대표적인 발생 사례:</p>
 * <ul>
 *     <li>Agent 요청이 올바르지 않은 경우</li>
 *     <li>Agent 실행 상태 전이가 잘못된 경우</li>
 *     <li>최대 반복 횟수에 도달한 경우</li>
 *     <li>LLM 또는 Tool 실행에 실패한 경우</li>
 *     <li>최종 응답을 생성하지 못한 경우</li>
 * </ul>
 */
public class AgentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    /**
     * 오류 메시지를 포함하는 예외를 생성합니다.
     *
     * @param message 오류 메시지
     */
    public AgentException(String message) {
        this(null, message, null);
    }

    /**
     * 오류 메시지와 원인 예외를 포함하는 예외를 생성합니다.
     *
     * @param message 오류 메시지
     * @param cause   원인 예외
     */
    public AgentException(
            String message,
            Throwable cause) {

        this(null, message, cause);
    }

    /**
     * 오류 코드와 오류 메시지를 포함하는 예외를 생성합니다.
     *
     * @param errorCode 오류 코드
     * @param message   오류 메시지
     */
    public AgentException(
            String errorCode,
            String message) {

        this(errorCode, message, null);
    }

    /**
     * 오류 코드, 오류 메시지 및 원인 예외를 포함하는 예외를 생성합니다.
     *
     * @param errorCode 오류 코드
     * @param message   오류 메시지
     * @param cause     원인 예외
     */
    public AgentException(
            String errorCode,
            String message,
            Throwable cause) {

        super(message, cause);
        this.errorCode = normalizeErrorCode(errorCode);
    }

    /**
     * Agent 오류 코드를 반환합니다.
     *
     * @return 오류 코드 또는 {@code null}
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 오류 코드가 존재하는지 확인합니다.
     */
    public boolean hasErrorCode() {
        return errorCode != null;
    }

    /**
     * 요청 검증 실패 예외를 생성합니다.
     */
    public static AgentException invalidRequest(String message) {
        return new AgentException(
                AgentErrorCodes.INVALID_REQUEST,
                message
        );
    }

    /**
     * 실행 상태 오류 예외를 생성합니다.
     */
    public static AgentException invalidState(String message) {
        return new AgentException(
                AgentErrorCodes.INVALID_STATE,
                message
        );
    }

    /**
     * 최대 반복 횟수 도달 예외를 생성합니다.
     */
    public static AgentException iterationLimitReached(
            int maxIterations) {

        return new AgentException(
                AgentErrorCodes.ITERATION_LIMIT_REACHED,
                "Agent maximum iteration limit reached: "
                        + maxIterations
        );
    }

    /**
     * LLM 호출 실패 예외를 생성합니다.
     */
    public static AgentException llmFailed(Throwable cause) {
        return new AgentException(
                AgentErrorCodes.LLM_FAILED,
                "Agent LLM execution failed.",
                cause
        );
    }

    /**
     * Tool 실행 실패 예외를 생성합니다.
     */
    public static AgentException toolFailed(
            String toolName,
            Throwable cause) {

        String normalizedToolName =
                toolName == null || toolName.isBlank()
                        ? "unknown"
                        : toolName.trim();

        return new AgentException(
                AgentErrorCodes.TOOL_FAILED,
                "Agent tool execution failed: "
                        + normalizedToolName,
                cause
        );
    }

    /**
     * 최종 응답 생성 실패 예외를 생성합니다.
     */
    public static AgentException responseFailed(
            String message) {

        return new AgentException(
                AgentErrorCodes.RESPONSE_FAILED,
                message
        );
    }

    /**
     * Agent 실행 취소 예외를 생성합니다.
     */
    public static AgentException cancelled() {
        return new AgentException(
                AgentErrorCodes.CANCELLED,
                "Agent execution was cancelled."
        );
    }

    /**
     * 일반 실행 실패 예외를 생성합니다.
     */
    public static AgentException executionFailed(
            Throwable cause) {

        return new AgentException(
                AgentErrorCodes.EXECUTION_FAILED,
                "Agent execution failed.",
                cause
        );
    }

    private static String normalizeErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return null;
        }

        return errorCode.trim();
    }

    /**
     * Agent 오류 코드 상수입니다.
     */
    public static final class AgentErrorCodes {

        public static final String INVALID_REQUEST =
                "AGENT_INVALID_REQUEST";

        public static final String INVALID_STATE =
                "AGENT_INVALID_STATE";

        public static final String ITERATION_LIMIT_REACHED =
                "AGENT_ITERATION_LIMIT_REACHED";

        public static final String LLM_FAILED =
                "AGENT_LLM_FAILED";

        public static final String TOOL_FAILED =
                "AGENT_TOOL_FAILED";

        public static final String RESPONSE_FAILED =
                "AGENT_RESPONSE_FAILED";

        public static final String CANCELLED =
                "AGENT_CANCELLED";

        public static final String EXECUTION_FAILED =
                "AGENT_EXECUTION_FAILED";

        private AgentErrorCodes() {
            throw new AssertionError("Utility class");
        }
    }
}