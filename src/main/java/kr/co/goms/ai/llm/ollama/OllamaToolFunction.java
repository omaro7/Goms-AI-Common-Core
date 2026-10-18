/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ollama Tool Calling에서 호출할 함수 정보를 나타냅니다.
 *
 * <p>Ollama 응답 예:</p>
 *
 * <pre>
 * {
 *   "name": "validate_xhtml",
 *   "arguments": {
 *     "xhtml": "&lt;html&gt;...&lt;/html&gt;"
 *   }
 * }
 * </pre>
 */
public final class OllamaToolFunction {

    private String name;

    private Map<String, Object> arguments;

    /**
     * Gson 역직렬화를 위한 기본 생성자입니다.
     */
    public OllamaToolFunction() {
        this.arguments = new LinkedHashMap<>();
    }

    /**
     * Tool 함수 정보를 생성합니다.
     *
     * @param name      Tool 함수명
     * @param arguments Tool 호출 인자
     */
    public OllamaToolFunction(
            String name,
            Map<String, Object> arguments) {

        this();

        setName(name);
        setArguments(arguments);
    }

    /**
     * 인자가 없는 Tool 함수 정보를 생성합니다.
     *
     * @param name Tool 함수명
     */
    public OllamaToolFunction(String name) {
        this(name, Map.of());
    }

    /**
     * Tool 함수명을 반환합니다.
     */
    public String getName() {
        return name;
    }

    /**
     * Tool 함수명을 설정합니다.
     */
    public void setName(String name) {
        this.name = normalizeName(name);
    }

    /**
     * Tool 호출 인자를 반환합니다.
     *
     * @return 수정할 수 없는 인자 Map
     */
    public Map<String, Object> getArguments() {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(arguments);
    }

    /**
     * Tool 호출 인자를 설정합니다.
     */
    public void setArguments(
            Map<String, Object> arguments) {

        if (arguments == null || arguments.isEmpty()) {
            this.arguments = new LinkedHashMap<>();
            return;
        }

        this.arguments =
                new LinkedHashMap<>(arguments);
    }

    /**
     * Tool 호출 인자를 하나 추가합니다.
     */
    public void setArgument(
            String name,
            Object value) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "argument name must not be blank"
            );
        }

        ensureArguments();

        arguments.put(
                name.trim(),
                value
        );
    }

    /**
     * Tool 호출 인자가 존재하는지 확인합니다.
     */
    public boolean hasArguments() {
        return arguments != null
                && !arguments.isEmpty();
    }

    /**
     * 특정 Tool 인자가 존재하는지 확인합니다.
     */
    public boolean containsArgument(String name) {
        return name != null
                && arguments != null
                && arguments.containsKey(name);
    }

    /**
     * 특정 Tool 인자를 반환합니다.
     */
    public Object getArgument(String name) {
        if (name == null || arguments == null) {
            return null;
        }

        return arguments.get(name);
    }

    /**
     * 특정 Tool 인자를 지정한 타입으로 반환합니다.
     */
    public <T> T getArgument(
            String name,
            Class<T> type) {

        if (type == null) {
            throw new IllegalArgumentException(
                    "type must not be null"
            );
        }

        Object value = getArgument(name);

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Ollama Tool argument type mismatch. "
                            + "name=" + name
                            + ", expected="
                            + type.getName()
                            + ", actual="
                            + value.getClass().getName()
            );
        }

        return type.cast(value);
    }

    /**
     * Tool 함수 정의를 검증합니다.
     */
    public void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException(
                    "Ollama Tool function name is missing"
            );
        }

        if (!name.matches(
                "[A-Za-z_][A-Za-z0-9_-]*")) {

            throw new IllegalStateException(
                    "Invalid Ollama Tool function name: "
                            + name
            );
        }

        /*
         * arguments가 없는 Tool 호출도 허용합니다.
         */
        if (arguments == null) {
            arguments = new LinkedHashMap<>();
        }

        for (String argumentName
                : arguments.keySet()) {

            if (argumentName == null
                    || argumentName.isBlank()) {

                throw new IllegalStateException(
                        "Ollama Tool argument name "
                                + "must not be blank"
                );
            }
        }
    }

    private void ensureArguments() {
        if (arguments == null) {
            arguments = new LinkedHashMap<>();
        }
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Ollama Tool function name "
                            + "must not be blank"
            );
        }

        String normalized =
                name.trim();

        if (!normalized.matches(
                "[A-Za-z_][A-Za-z0-9_-]*")) {

            throw new IllegalArgumentException(
                    "Invalid Ollama Tool function name: "
                            + normalized
            );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "OllamaToolFunction{"
                + "name='" + name + '\''
                + ", argumentNames="
                + (arguments == null
                        ? "[]"
                        : arguments.keySet())
                + '}';
    }
}