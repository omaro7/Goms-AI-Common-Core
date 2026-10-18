/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm;

import java.util.Map;
import java.util.Objects;

/**
 * LLM이 요청한 Tool 호출 정보입니다.
 *
 * <p>현재는 function 방식의 Tool Calling을 지원합니다.</p>
 *
 * <pre>
 * {
 *   "id": "tool-call-001",
 *   "type": "function",
 *   "function": {
 *     "name": "validate_xhtml",
 *     "arguments": {
 *       "xhtml": "..."
 *     }
 *   }
 * }
 * </pre>
 */
public final class LlmToolCall {

    public static final String TYPE_FUNCTION = "function";

    private final String id;
    private final String type;
    private final LlmToolCallFunction function;

    /**
     * 함수형 Tool Call을 생성합니다.
     *
     * @param id       Tool Call 식별자
     * @param function 호출 함수 정보
     */
    public LlmToolCall(
            String id,
            LlmToolCallFunction function) {

        this(
                id,
                TYPE_FUNCTION,
                function
        );
    }

    /**
     * Tool Call을 생성합니다.
     *
     * @param id       Tool Call 식별자
     * @param type     Tool Call 유형
     * @param function 함수 정보
     */
    public LlmToolCall(
            String id,
            String type,
            LlmToolCallFunction function) {

        this.id =
                normalizeOptional(id);

        this.type =
                normalizeType(type);

        this.function =
                Objects.requireNonNull(
                        function,
                        "function must not be null"
                );

        validate();
    }

    /**
     * Tool Call 식별자를 반환합니다.
     */
    public String getId() {
        return id;
    }

    /**
     * Tool Call 유형을 반환합니다.
     */
    public String getType() {
        return type;
    }

    /**
     * 함수 정보를 반환합니다.
     */
    public LlmToolCallFunction getFunction() {
        return function;
    }

    /**
     * Tool Call ID가 존재하는지 확인합니다.
     */
    public boolean hasId() {
        return id != null;
    }

    /**
     * function 타입의 Tool Call인지 확인합니다.
     */
    public boolean isFunctionCall() {
        return TYPE_FUNCTION.equals(type);
    }

    /**
     * 호출할 Tool 이름을 반환합니다.
     */
    public String getToolName() {
        return function.getName();
    }

    /**
     * Tool 호출 인자를 반환합니다.
     *
     * @return 수정할 수 없는 인자 Map
     */
    public Map<String, Object> getArguments() {
        return function.getArguments();
    }

    /**
     * Tool 호출 인자가 존재하는지 확인합니다.
     */
    public boolean hasArguments() {
        return function.hasArguments();
    }

    /**
     * 특정 Tool 인자를 반환합니다.
     */
    public Object getArgument(String name) {
        return function.getArgument(name);
    }

    /**
     * 특정 Tool 인자를 지정한 타입으로 반환합니다.
     */
    public <T> T getArgument(
            String name,
            Class<T> type) {

        return function.getArgument(
                name,
                type
        );
    }

    /**
     * Tool Call의 유효성을 검증합니다.
     */
    public void validate() {
        if (!TYPE_FUNCTION.equals(type)) {
            throw new IllegalArgumentException(
                    "Unsupported LLM Tool Call type: "
                            + type
            );
        }

        function.validate();
    }

    private static String normalizeType(
            String value) {

        if (value == null || value.isBlank()) {
            return TYPE_FUNCTION;
        }

        String normalized =
                value.trim().toLowerCase();

        if (!TYPE_FUNCTION.equals(normalized)) {
            throw new IllegalArgumentException(
                    "Unsupported LLM Tool Call type: "
                            + value
            );
        }

        return normalized;
    }

    private static String normalizeOptional(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Override
    public String toString() {
        return "LlmToolCall{"
                + "id='" + id + '\''
                + ", type='" + type + '\''
                + ", function=" + function
                + '}';
    }
}