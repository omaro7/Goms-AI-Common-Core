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
 * LLM에 제공할 Tool 정의입니다.
 *
 * <p>현재는 함수 호출 방식만 지원하며 다음 구조를 표현합니다.</p>
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
 * 
 * Map<String, Object> properties =
        new LinkedHashMap<>();

	properties.put(
	        "xhtml",
	        Map.of(
	                "type", "string",
	                "description", "검증할 XHTML 문서"
	        )
	);
	
	Map<String, Object> parameters =
	        new LinkedHashMap<>();
	
	parameters.put("type", "object");
	parameters.put("properties", properties);
	parameters.put("required", List.of("xhtml"));
	
	LlmToolDefinition definition =
        LlmToolDefinition.function(
                "validate_xhtml",
                "XHTML 문서의 문법과 EPUB3 호환성을 검증합니다.",
                parameters
        );
 */
public final class LlmToolDefinition {

    public static final String TYPE_FUNCTION = "function";

    private final String type;
    private final FunctionDefinition function;

    /**
     * 함수형 Tool 정의를 생성합니다.
     *
     * @param function 함수 정의
     */
    public LlmToolDefinition(FunctionDefinition function) {
        this(TYPE_FUNCTION, function);
    }

    /**
     * Tool 정의를 생성합니다.
     *
     * @param type     Tool 유형
     * @param function 함수 정의
     */
    public LlmToolDefinition(
            String type,
            FunctionDefinition function) {

        this.type = normalizeType(type);
        this.function = Objects.requireNonNull(
                function,
                "function must not be null"
        );

        validate();
    }

    /**
     * 간단한 함수형 Tool 정의를 생성합니다.
     *
     * @param name        Tool 이름
     * @param description Tool 설명
     * @param parameters  JSON Schema 형식 입력 파라미터
     * @return Tool 정의
     */
    public static LlmToolDefinition function(
            String name,
            String description,
            Map<String, Object> parameters) {

        return new LlmToolDefinition(
                new FunctionDefinition(
                        name,
                        description,
                        parameters
                )
        );
    }

    /**
     * Tool 유형을 반환합니다.
     */
    public String getType() {
        return type;
    }

    /**
     * 함수 정의를 반환합니다.
     */
    public FunctionDefinition getFunction() {
        return function;
    }

    /**
     * 함수 호출 유형인지 확인합니다.
     */
    public boolean isFunction() {
        return TYPE_FUNCTION.equals(type);
    }

    /**
     * Tool 이름을 반환합니다.
     */
    public String getName() {
        return function.getName();
    }

    /**
     * Tool 설명을 반환합니다.
     */
    public String getDescription() {
        return function.getDescription();
    }

    /**
     * Tool 입력 파라미터 스키마를 반환합니다.
     */
    public Map<String, Object> getParameters() {
        return function.getParameters();
    }

    /**
     * Tool 정의를 검증합니다.
     */
    public void validate() {
        if (!TYPE_FUNCTION.equals(type)) {
            throw new IllegalArgumentException(
                    "Unsupported LLM tool definition type: " + type
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
                    "Unsupported LLM tool definition type: "
                            + type
            );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "LlmToolDefinition{"
                + "type='" + type + '\''
                + ", function=" + function
                + '}';
    }

    /**
     * 함수형 Tool의 세부 정의입니다.
     */
    public static final class FunctionDefinition {

        private final String name;
        private final String description;
        private final Map<String, Object> parameters;

        /**
         * 함수 정의를 생성합니다.
         *
         * @param name        함수명
         * @param description 함수 설명
         * @param parameters  JSON Schema 형식 입력 파라미터
         */
        public FunctionDefinition(
                String name,
                String description,
                Map<String, Object> parameters) {

            this.name = requireText(
                    name,
                    "name"
            );

            this.description =
                    normalizeDescription(description);

            this.parameters =
                    immutableParameters(parameters);

            validate();
        }

        /**
         * 함수명을 반환합니다.
         */
        public String getName() {
            return name;
        }

        /**
         * 함수 설명을 반환합니다.
         */
        public String getDescription() {
            return description;
        }

        /**
         * JSON Schema 형식의 입력 파라미터를 반환합니다.
         */
        public Map<String, Object> getParameters() {
            return parameters;
        }

        /**
         * 파라미터 스키마가 있는지 확인합니다.
         */
        public boolean hasParameters() {
            return !parameters.isEmpty();
        }

        /**
         * 함수 정의를 검증합니다.
         */
        public void validate() {
            if (name.isBlank()) {
                throw new IllegalArgumentException(
                        "tool function name must not be blank"
                );
            }

            if (parameters.isEmpty()) {
                throw new IllegalArgumentException(
                        "tool function parameters must not be empty"
                );
            }

            Object schemaType =
                    parameters.get("type");

            if (!(schemaType instanceof String)
                    || !"object".equals(schemaType)) {

                throw new IllegalArgumentException(
                        "tool function parameter schema "
                                + "must have type=object"
                );
            }
        }

        private static String normalizeDescription(
                String description) {

            if (description == null || description.isBlank()) {
                return "";
            }

            return description.trim();
        }

        private static Map<String, Object>
                immutableParameters(
                        Map<String, Object> parameters) {

            if (parameters == null || parameters.isEmpty()) {
                return defaultEmptyObjectSchema();
            }

            return Collections.unmodifiableMap(
                    new LinkedHashMap<>(parameters)
            );
        }

        private static Map<String, Object>
                defaultEmptyObjectSchema() {

            Map<String, Object> schema =
                    new LinkedHashMap<>();

            schema.put("type", "object");
            schema.put("properties", Map.of());

            return Collections.unmodifiableMap(schema);
        }

        private static String requireText(
                String value,
                String fieldName) {

            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        fieldName + " must not be blank"
                );
            }

            return value.trim();
        }

        @Override
        public String toString() {
            return "FunctionDefinition{"
                    + "name='" + name + '\''
                    + ", description='" + description + '\''
                    + ", parameters=" + parameters
                    + '}';
        }
    }
}