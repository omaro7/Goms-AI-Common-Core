/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;

/**
 * Ollama Chat API가 반환한 Tool Call 정보입니다.
 *
 * <p>Assistant 메시지의 {@code tool_calls} 배열에 포함되며,
 * 호출할 함수 이름과 인자를 보유합니다.</p>
 *
 * <pre>
 * {
 *   "type": "function",
 *   "function": {
 *     "name": "generate_xhtml",
 *     "arguments": {
 *       "title": "제1장",
 *       "content": "본문 내용"
 *     }
 *   }
 * }
 * </pre>
 */
public final class OllamaToolCall {

    public static final String TYPE_FUNCTION = "function";

    /**
     * 일부 Ollama 응답에서 제공될 수 있는 Tool Call 순번입니다.
     *
     * <p>값이 제공되지 않으면 {@code null}입니다.</p>
     */
    private Integer index;

    /**
     * Tool Call 유형입니다.
     *
     * <p>현재는 {@code function} 유형만 지원합니다.</p>
     */
    private String type;

    /**
     * 호출할 함수 정보입니다.
     */
    private OllamaToolFunction function;

    /**
     * Gson 역직렬화를 위한 기본 생성자입니다.
     */
    public OllamaToolCall() {
        this.type = TYPE_FUNCTION;
    }

    /**
     * 함수형 Tool Call을 생성합니다.
     *
     * @param function 호출할 함수 정보
     */
    public OllamaToolCall(
            OllamaToolFunction function) {

        this(null, TYPE_FUNCTION, function);
    }

    /**
     * 함수형 Tool Call을 생성합니다.
     *
     * @param index    Tool Call 순번
     * @param function 호출할 함수 정보
     */
    public OllamaToolCall(
            Integer index,
            OllamaToolFunction function) {

        this(index, TYPE_FUNCTION, function);
    }

    /**
     * Tool Call을 생성합니다.
     *
     * @param index    Tool Call 순번
     * @param type     Tool Call 유형
     * @param function 호출할 함수 정보
     */
    public OllamaToolCall(
            Integer index,
            String type,
            OllamaToolFunction function) {

        setIndex(index);
        setType(type);
        setFunction(function);
    }

    /**
     * Tool Call 순번을 반환합니다.
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * Tool Call 순번을 설정합니다.
     */
    public void setIndex(Integer index) {
        if (index != null && index < 0) {
            throw new IllegalArgumentException(
                    "index must not be negative"
            );
        }

        this.index = index;
    }

    /**
     * Tool Call 유형을 반환합니다.
     */
    public String getType() {
        return type == null
                ? TYPE_FUNCTION
                : type;
    }

    /**
     * Tool Call 유형을 설정합니다.
     */
    public void setType(String type) {
        this.type = normalizeType(type);
    }

    /**
     * 호출할 함수 정보를 반환합니다.
     */
    public OllamaToolFunction getFunction() {
        return function;
    }

    /**
     * 호출할 함수 정보를 설정합니다.
     */
    public void setFunction(
            OllamaToolFunction function) {

        this.function = Objects.requireNonNull(
                function,
                "function must not be null"
        );
    }

    /**
     * 함수 호출 유형인지 확인합니다.
     */
    public boolean isFunctionCall() {
        return TYPE_FUNCTION.equals(getType());
    }

    /**
     * Tool Call 순번이 존재하는지 확인합니다.
     */
    public boolean hasIndex() {
        return index != null;
    }

    /**
     * 호출할 Tool 이름을 반환합니다.
     */
    public String getToolName() {
        if (function == null) {
            return null;
        }

        return function.getName();
    }

    /**
     * 호출할 Tool 이름이 존재하는지 확인합니다.
     */
    public boolean hasToolName() {
        String toolName = getToolName();

        return toolName != null
                && !toolName.isBlank();
    }

    /**
     * Tool 호출 인자를 반환합니다.
     */
    public Object getArguments() {
        if (function == null) {
            return null;
        }

        return function.getArguments();
    }

    /**
     * Tool 호출 인자가 존재하는지 확인합니다.
     */
    public boolean hasArguments() {
        if (function == null) {
            return false;
        }

        return function.hasArguments();
    }
    
    public void validate() {
        if (!TYPE_FUNCTION.equals(getType())) {
            throw new IllegalStateException(
                    "Unsupported Ollama tool call type: "
                            + getType()
            );
        }

        if (function == null) {
            throw new IllegalStateException(
                    "Ollama tool call function is missing"
            );
        }

        function.validate();
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return TYPE_FUNCTION;
        }

        String normalized =
                type.trim().toLowerCase();

        if (!TYPE_FUNCTION.equals(normalized)) {
            throw new IllegalArgumentException(
                    "Unsupported Ollama tool call type: "
                            + type
            );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "OllamaToolCall{"
                + "index=" + index
                + ", type='" + getType() + '\''
                + ", function=" + function
                + '}';
    }
}
