/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * LLM이 요청한 Tool 함수 호출 정보를 나타냅니다.
 *
 * <p>함수명과 호출 인자를 포함하며, 호출 인자는 JSON 역직렬화
 * 호환성을 위해 {@code Map<String, Object>} 형태로 관리합니다.</p>
 */
public final class LlmToolCallFunction {

    private final String name;
    private Map<String, Object> arguments;

    /**
     * Tool 함수 호출 정보를 생성합니다.
     *
     * @param name      호출할 Tool 이름
     * @param arguments Tool 호출 인자
     */
    public LlmToolCallFunction(
            String name,
            Map<String, Object> arguments) {

        this.name = requireName(name);
        this.arguments = immutableArguments(arguments);
    }

    /**
     * 인자가 없는 Tool 함수 호출 정보를 생성합니다.
     *
     * @param name 호출할 Tool 이름
     */
    public LlmToolCallFunction(String name) {
        this(name, Map.of());
    }

    /**
     * 호출할 Tool 이름을 반환합니다.
     */
    public String getName() {
        return name;
    }

    /**
     * Tool 호출 인자를 반환합니다.
     *
     * @return 수정할 수 없는 인자 Map
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Tool 호출 인자가 존재하는지 확인합니다.
     */
    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    /**
     * 특정 인자값을 반환합니다.
     *
     * @param name 인자명
     * @return 인자값 또는 {@code null}
     */
    public Object getArgument(String name) {
        if (name == null) {
            return null;
        }

        return arguments.get(name);
    }

    /**
     * 특정 인자값을 지정한 타입으로 반환합니다.
     *
     * @param name 인자명
     * @param type 반환 타입
     * @param <T>  반환 타입
     * @return 인자값 또는 {@code null}
     */
    public <T> T getArgument(
            String name,
            Class<T> type) {

        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        Object value = getArgument(name);

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "LLM tool argument type mismatch. "
                            + "name=" + name
                            + ", expected=" + type.getName()
                            + ", actual="
                            + value.getClass().getName()
            );
        }

        return type.cast(value);
    }

    /**
     * 특정 인자값을 기본값과 함께 반환합니다.
     */
    public <T> T getArgumentOrDefault(
            String name,
            Class<T> type,
            T defaultValue) {

        T value = getArgument(name, type);

        return value != null
                ? value
                : defaultValue;
    }

    /**
     * 특정 인자가 존재하는지 확인합니다.
     */
    public boolean containsArgument(String name) {
        return name != null
                && arguments.containsKey(name);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "tool function name must not be blank"
            );
        }

        return name.trim();
    }

    private static Map<String, Object> immutableArguments(
            Map<String, Object> arguments) {

        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(arguments)
        );
    }
    
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(
                    "Ollama Tool function name is missing"
            );
        }

        if (!name.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
            throw new IllegalStateException(
                    "Invalid Ollama Tool function name: " + name
            );
        }

        if (arguments == null) {
            arguments = new LinkedHashMap<>();
        }
    }

    @Override
    public String toString() {
        return "LlmToolCallFunction{"
                + "name='" + name + '\''
                + ", arguments=" + arguments
                + '}';
    }
}