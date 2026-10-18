/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ollama Chat API에 전달할 Tool 정의입니다.
 *
 * <p>Ollama의 함수 호출 형식을 표현합니다.</p>
 *
 * <pre>
 * {
 *   "type": "function",
 *   "function": {
 *     "name": "validate_xhtml",
 *     "description": "XHTML 문서를 검증합니다.",
 *     "parameters": {
 *       "type": "object",
 *       "properties": {
 *         "xhtml": {
 *           "type": "string"
 *         }
 *       },
 *       "required": ["xhtml"]
 *     }
 *   }
 * }
 * </pre>
 */
public final class OllamaToolDefinition {

    public static final String TYPE_FUNCTION = "function";

    private String type;
    private OllamaToolDefinition.FunctionDefinition function;

    /**
     * Gson 역직렬화를 위한 기본 생성자입니다.
     */
    public OllamaToolDefinition() {
        this.type = TYPE_FUNCTION;
    }

    /**
     * 함수형 Tool 정의를 생성합니다.
     *
     * @param function 함수 정의
     */
    public OllamaToolDefinition(
            FunctionDefinition function) {

        this(TYPE_FUNCTION, function);
    }

    /**
     * Tool 정의를 생성합니다.
     *
     * @param type     Tool 유형
     * @param function 함수 정의
     */
    public OllamaToolDefinition(
            String type,
            FunctionDefinition function) {

        setType(type);
        setFunction(function);
    }

    /**
     * Tool 유형을 반환합니다.
     */
    public String getType() {
        return type == null
                ? TYPE_FUNCTION
                : type;
    }

    /**
     * Tool 유형을 설정합니다.
     */
    public void setType(String type) {
        this.type = normalizeType(type);
    }

    /**
     * 함수 정의를 반환합니다.
     */
    public FunctionDefinition getFunction() {
        return function;
    }

    /**
     * 함수 정의를 설정합니다.
     */
    public void setFunction(
            FunctionDefinition function) {

        this.function = Objects.requireNonNull(
                function,
                "function must not be null"
        );
    }

    /**
     * 함수 호출 유형인지 확인합니다.
     */
    public boolean isFunction() {
        return TYPE_FUNCTION.equals(getType());
    }

    /**
     * Tool 이름을 반환합니다.
     */
    public String getName() {
        if (function == null) {
            return null;
        }

        return function.getName();
    }

    /**
     * Tool 설명을 반환합니다.
     */
    public String getDescription() {
        if (function == null) {
            return "";
        }

        return function.getDescription();
    }

    /**
     * Tool 입력 파라미터 스키마를 반환합니다.
     */
    public Map<String, Object> getParameters() {
        if (function == null) {
            return Map.of();
        }

        return function.getParameters();
    }

    /**
     * Tool 정의를 검증합니다.
     */
    public void validate() {
        if (!TYPE_FUNCTION.equals(getType())) {
            throw new IllegalStateException(
                    "Unsupported Ollama Tool type: "
                            + getType()
            );
        }

        if (function == null) {
            throw new IllegalStateException(
                    "Ollama Tool function is missing"
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
                    "Unsupported Ollama Tool type: "
                            + type
            );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "OllamaToolDefinition{"
                + "type='" + getType() + '\''
                + ", function=" + function
                + '}';
    }

    /**
     * Ollama 함수형 Tool의 세부 정의입니다.
     */
    public static final class FunctionDefinition {

        private String name;
        private String description;
        private Map<String, Object> parameters;

        /**
         * Gson 역직렬화를 위한 기본 생성자입니다.
         */
        public FunctionDefinition() {
            this.description = "";
            this.parameters = createEmptyObjectSchema();
        }

        /**
         * 함수 정의를 생성합니다.
         *
         * @param name        함수명
         * @param description 함수 설명
         * @param parameters  JSON Schema 형식 파라미터
         */
        public FunctionDefinition(
                String name,
                String description,
                Map<String, Object> parameters) {

            setName(name);
            setDescription(description);
            setParameters(parameters);
        }

        /**
         * 함수명을 반환합니다.
         */
        public String getName() {
            return name;
        }

        /**
         * 함수명을 설정합니다.
         */
        public void setName(String name) {
            this.name = requireName(name);
        }

        /**
         * 함수 설명을 반환합니다.
         */
        public String getDescription() {
            return description == null
                    ? ""
                    : description;
        }

        /**
         * 함수 설명을 설정합니다.
         */
        public void setDescription(String description) {
            this.description =
                    normalizeDescription(description);
        }

        /**
         * JSON Schema 파라미터를 반환합니다.
         *
         * @return 수정할 수 없는 파라미터 Map
         */
        public Map<String, Object> getParameters() {
            if (parameters == null || parameters.isEmpty()) {
                return Map.of();
            }

            return Collections.unmodifiableMap(parameters);
        }

        /**
         * JSON Schema 파라미터를 설정합니다.
         */
        public void setParameters(
                Map<String, Object> parameters) {

            this.parameters =
                    copyParameters(parameters);
        }

        /**
         * 파라미터 스키마가 존재하는지 확인합니다.
         */
        public boolean hasParameters() {
            return parameters != null
                    && !parameters.isEmpty();
        }

        /**
         * 함수 정의를 검증합니다.
         */
        public void validate() {
            requireName(name);

            if (parameters == null
                    || parameters.isEmpty()) {

                throw new IllegalStateException(
                        "Ollama Tool parameters are missing"
                );
            }

            Object schemaType =
                    parameters.get("type");

            if (!(schemaType instanceof String type)
                    || !"object".equals(type)) {

                throw new IllegalStateException(
                        "Ollama Tool parameter schema "
                                + "must have type=object"
                );
            }

            Object properties =
                    parameters.get("properties");

            if (properties != null
                    && !(properties instanceof Map<?, ?>)) {

                throw new IllegalStateException(
                        "Ollama Tool parameter properties "
                                + "must be an object"
                );
            }

            Object required =
                    parameters.get("required");

            if (required != null
                    && !(required instanceof Iterable<?>)) {

                throw new IllegalStateException(
                        "Ollama Tool parameter required "
                                + "must be an array"
                );
            }
        }

        private static String requireName(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "Ollama Tool function name "
                                + "must not be blank"
                );
            }

            String normalized = name.trim();

            if (!normalized.matches(
                    "[A-Za-z_][A-Za-z0-9_-]*")) {

                throw new IllegalArgumentException(
                        "Invalid Ollama Tool function name: "
                                + normalized
                );
            }

            return normalized;
        }

        private static String normalizeDescription(
                String description) {

            if (description == null
                    || description.isBlank()) {

                return "";
            }

            return description.trim();
        }

        private static Map<String, Object> copyParameters(
                Map<String, Object> parameters) {

            if (parameters == null
                    || parameters.isEmpty()) {

                return createEmptyObjectSchema();
            }

            return new LinkedHashMap<>(parameters);
        }

        private static Map<String, Object>
                createEmptyObjectSchema() {

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("type", "object");
            schema.put(
                    "properties",
                    new LinkedHashMap<String, Object>()
            );

            return schema;
        }

        @Override
        public String toString() {
            return "FunctionDefinition{"
                    + "name='" + name + '\''
                    + ", description='"
                    + description + '\''
                    + ", parameters="
                    + parameters
                    + '}';
        }
    }
}