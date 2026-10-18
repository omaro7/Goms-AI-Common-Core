/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Agent가 Tool 실행을 요청할 때 사용하는 표준 요청 객체입니다.
 *
 * <p>LLM Tool Call 정보를 Tool 실행 계층으로 전달하며 다음 정보를 포함합니다.</p>
 *
 * <ul>
 *     <li>Agent 요청 식별자</li>
 *     <li>LLM Tool Call 식별자</li>
 *     <li>호출할 Tool 이름</li>
 *     <li>Tool 실행 인자</li>
 * </ul>
 *
 * <p>사용 예시:</p>
 *
 * <pre>
 * ToolRequest request = ToolRequest.builder()
 *         .requestId("request-001")
 *         .toolCallId("tool-call-001")
 *         .toolName("validate_xhtml")
 *         .argument("xhtml", xhtml)
 *         .build();
 * </pre>
 */
public final class ToolRequest {

    private final String requestId;
    private final String toolCallId;
    private final String toolName;
    private final Map<String, Object> arguments;

    private ToolRequest(Builder builder) {

        this.requestId = normalizeOptional(builder.requestId);
        this.toolCallId = normalizeOptional(builder.toolCallId);
        this.toolName = requireToolName(builder.toolName);
        this.arguments = immutableArguments(builder.arguments);
    }

    /**
     * 인자 없는 Tool 요청을 생성합니다.
     *
     * @param toolName Tool 이름
     */
    public ToolRequest(String toolName) {
        this(builder().toolName(toolName));
    }

    /**
     * Tool 이름과 인자로 요청을 생성합니다.
     *
     * @param toolName  Tool 이름
     * @param arguments Tool 실행 인자
     */
    public ToolRequest(String toolName, Map<String, Object> arguments) {
        this(builder().toolName(toolName).arguments(arguments));
    }

    /**
     * Builder를 생성합니다.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기존 요청을 기반으로 Builder를 생성합니다.
     *
     * @param source 원본 Tool 요청
     */
    public static Builder builder(ToolRequest source) {

        Objects.requireNonNull(source, "source must not be null");

        return new Builder(source);
    }

    /**
     * Agent 요청 식별자를 반환합니다.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * LLM Tool Call 식별자를 반환합니다.
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * 호출할 Tool 이름을 반환합니다.
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Tool 실행 인자를 반환합니다.
     *
     * @return 수정할 수 없는 인자 Map
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Agent 요청 식별자가 있는지 확인합니다.
     */
    public boolean hasRequestId() {
        return requestId != null;
    }

    /**
     * Tool Call 식별자가 있는지 확인합니다.
     */
    public boolean hasToolCallId() {
        return toolCallId != null;
    }

    /**
     * Tool 실행 인자가 있는지 확인합니다.
     */
    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    /**
     * 특정 Tool 인자가 존재하는지 확인합니다.
     *
     * @param name 인자명
     */
    public boolean containsArgument(String name) {
        return name != null && arguments.containsKey(name);
    }

    /**
     * 특정 Tool 인자를 반환합니다.
     *
     * @param name 인자명
     * @return 인자값 또는 {@code null}
     */
    public Object getArgument(String name) {

        if (name == null) return null;

        return arguments.get(name);
    }

    /**
     * 특정 Tool 인자를 지정한 타입으로 반환합니다.
     *
     * <p>숫자 인자의 경우 Number 타입 간 안전한 변환을 지원합니다.</p>
     *
     * @param name 인자명
     * @param type 반환 타입
     * @param <T>  반환 타입
     * @return 인자값 또는 {@code null}
     * @throws IllegalArgumentException 변환할 수 없는 타입이거나 숫자 범위를 벗어난 경우
     */
    public <T> T getArgument(String name, Class<T> type) {

        Objects.requireNonNull(type, "type must not be null");

        Object value = getArgument(name);

        if (value == null) return null;

        return convertArgument(name, value, type, false);
    }

    /**
     * Tool 인자를 기본값과 함께 반환합니다.
     */
    public <T> T getArgumentOrDefault(String name, Class<T> type, T defaultValue) {

        T value = getArgument(name, type);

        return value != null ? value : defaultValue;
    }

    /**
     * 필수 문자열 인자를 반환합니다.
     *
     * @param name 인자명
     * @return 공백이 제거된 문자열
     * @throws IllegalArgumentException 인자가 없거나 문자열이 아닌 경우
     */
    public String requireStringArgument(String name) {

        Object value = requireArgument(name);

        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException(
                    "Required Tool argument must be a string. "
                            + "tool=" + toolName
                            + ", argument=" + name);
        }

        if (stringValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Required Tool argument must not be blank. "
                            + "tool=" + toolName
                            + ", argument=" + name);
        }

        return stringValue.trim();
    }

    /**
     * 필수 Tool 인자를 반환합니다.
     *
     * @param name 인자명
     * @return 인자값
     * @throws IllegalArgumentException 인자가 없는 경우
     */
    public Object requireArgument(String name) {

        validateArgumentName(name);

        if (!arguments.containsKey(name)) {
            throw new IllegalArgumentException(
                    "Required Tool argument is missing. "
                            + "tool=" + toolName
                            + ", argument=" + name);
        }

        Object value = arguments.get(name);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Required Tool argument must not be null. "
                            + "tool=" + toolName
                            + ", argument=" + name);
        }

        return value;
    }

    /**
     * 필수 Tool 인자를 지정한 타입으로 반환합니다.
     *
     * <p>숫자 인자의 경우 Number 타입 간 안전한 변환을 지원합니다.</p>
     */
    public <T> T requireArgument(String name, Class<T> type) {

        Objects.requireNonNull(type, "type must not be null");

        Object value = requireArgument(name);

        return convertArgument(name, value, type, true);
    }

    private <T> T convertArgument(String name, Object value, Class<T> type, boolean required) {

        if (type.isInstance(value)) return type.cast(value);

        if (value instanceof Number number) {

            Object converted = convertNumber(name, number, type);

            if (converted != null) return type.cast(converted);
        }

        String prefix = required ? "Required Tool argument type mismatch. " : "Tool argument type mismatch. ";

        throw new IllegalArgumentException(
                prefix
                        + "tool=" + toolName
                        + ", argument=" + name
                        + ", expected=" + type.getName()
                        + ", actual=" + value.getClass().getName());
    }

    private Object convertNumber(String name, Number number, Class<?> type) {

        if (type == Integer.class) return toInteger(name, number);
        if (type == Long.class) return toLong(name, number);
        if (type == Double.class) return toDouble(name, number);
        if (type == Float.class) return toFloat(name, number);

        return null;
    }

    private Integer toInteger(String name, Number number) {

        double value = number.doubleValue();

        requireFiniteNumber(name, value);

        if (value != Math.rint(value)) {
            throw new IllegalArgumentException(
                    "Tool argument must be an integer. "
                            + "tool=" + toolName
                            + ", argument=" + name
                            + ", actual=" + number);
        }

        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Tool argument is outside Integer range. "
                            + "tool=" + toolName
                            + ", argument=" + name
                            + ", actual=" + number);
        }

        return Integer.valueOf((int) value);
    }

    private Long toLong(String name, Number number) {

        double value = number.doubleValue();

        requireFiniteNumber(name, value);

        if (value != Math.rint(value)) {
            throw new IllegalArgumentException(
                    "Tool argument must be a long integer. "
                            + "tool=" + toolName
                            + ", argument=" + name
                            + ", actual=" + number);
        }

        if (value < Long.MIN_VALUE || value > Long.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Tool argument is outside Long range. "
                            + "tool=" + toolName
                            + ", argument=" + name
                            + ", actual=" + number);
        }

        return Long.valueOf((long) value);
    }

    private Double toDouble(String name, Number number) {

        double value = number.doubleValue();

        requireFiniteNumber(name, value);

        return Double.valueOf(value);
    }

    private Float toFloat(String name, Number number) {

        double value = number.doubleValue();

        requireFiniteNumber(name, value);

        if (value < -Float.MAX_VALUE || value > Float.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Tool argument is outside Float range. "
                            + "tool=" + toolName
                            + ", argument=" + name
                            + ", actual=" + number);
        }

        return Float.valueOf((float) value);
    }

    private void requireFiniteNumber(String name, double value) {

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Tool argument must be a finite number. "
                            + "tool=" + toolName
                            + ", argument=" + name
                            + ", actual=" + value);
        }
    }

    private static String requireToolName(String value) {

        if (value == null || value.isBlank()) throw new IllegalArgumentException("toolName must not be blank");

        String normalized = value.trim();

        if (!normalized.matches("[A-Za-z_][A-Za-z0-9_-]*")) throw new IllegalArgumentException("Invalid Tool name: " + normalized);

        return normalized;
    }

    private static String normalizeOptional(String value) {

        if (value == null || value.isBlank()) return null;

        return value.trim();
    }

    private static Map<String, Object> immutableArguments(Map<String, Object> source) {

        if (source == null || source.isEmpty()) return Map.of();

        Map<String, Object> copied = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : source.entrySet()) {

            String name = entry.getKey();

            validateArgumentName(name);

            copied.put(name.trim(), deepCopyValue(entry.getValue()));
        }

        return Collections.unmodifiableMap(copied);
    }

    /**
     * 중첩 Map과 Iterable을 복사하여 외부 변경 영향을 줄입니다.
     */
    private static Object deepCopyValue(Object value) {

        if (value instanceof Map<?, ?> map) {

            Map<String, Object> copied = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {

                if (entry.getKey() == null) throw new IllegalArgumentException("Nested Tool argument Map must not contain null keys");

                copied.put(String.valueOf(entry.getKey()), deepCopyValue(entry.getValue()));
            }

            return Collections.unmodifiableMap(copied);
        }

        if (value instanceof Iterable<?> iterable) {

            java.util.List<Object> copied = new java.util.ArrayList<>();

            for (Object item : iterable) copied.add(deepCopyValue(item));

            return Collections.unmodifiableList(copied);
        }

        return value;
    }

    private static void validateArgumentName(String name) {

        if (name == null || name.isBlank()) throw new IllegalArgumentException("argument name must not be blank");
    }

    @Override
    public String toString() {
        return "ToolRequest{"
                + "requestId='" + requestId + '\''
                + ", toolCallId='" + toolCallId + '\''
                + ", toolName='" + toolName + '\''
                + ", argumentNames=" + arguments.keySet()
                + '}';
    }

    /**
     * ToolRequest Builder입니다.
     */
    public static final class Builder {

        private String requestId;
        private String toolCallId;
        private String toolName;
        private final Map<String, Object> arguments = new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(ToolRequest source) {

            this.requestId = source.requestId;
            this.toolCallId = source.toolCallId;
            this.toolName = source.toolName;
            this.arguments.putAll(source.arguments);
        }

        public Builder requestId(String requestId) {

            this.requestId = requestId;

            return this;
        }

        public Builder toolCallId(String toolCallId) {

            this.toolCallId = toolCallId;

            return this;
        }

        public Builder toolName(String toolName) {

            this.toolName = toolName;

            return this;
        }

        /**
         * Tool 인자를 하나 추가하거나 변경합니다.
         */
        public Builder argument(String name, Object value) {

            validateArgumentName(name);

            this.arguments.put(name.trim(), value);

            return this;
        }

        /**
         * 여러 Tool 인자를 추가합니다.
         */
        public Builder arguments(Map<String, ?> arguments) {

            Objects.requireNonNull(arguments, "arguments must not be null");

            for (Map.Entry<String, ?> entry : arguments.entrySet()) argument(entry.getKey(), entry.getValue());

            return this;
        }

        /**
         * 지정한 Tool 인자를 제거합니다.
         */
        public Builder removeArgument(String name) {

            validateArgumentName(name);
            this.arguments.remove(name);

            return this;
        }

        /**
         * 모든 Tool 인자를 제거합니다.
         */
        public Builder clearArguments() {

            this.arguments.clear();

            return this;
        }

        /**
         * ToolRequest를 생성합니다.
         */
        public ToolRequest build() {
            return new ToolRequest(this);
        }
    }
}