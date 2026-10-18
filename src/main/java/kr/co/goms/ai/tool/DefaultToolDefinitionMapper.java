/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.llm.LlmToolDefinition;

/**
 * {@link AgentTool}을 {@link LlmToolDefinition}으로 변환하는
 * 기본 구현체입니다.
 *
 * <p>Agent Tool 계층의 이름, 설명 및 입력 스키마를
 * LLM Tool Calling 표준 구조로 변환합니다.</p>
 *
 * <pre>
 * AgentTool
 *     ├── name
 *     ├── description
 *     └── inputSchema
 *
 *         ↓
 *
 * LlmToolDefinition
 *     ├── type: function
 *     └── function
 *          ├── name
 *          ├── description
 *          └── parameters
 * </pre>
 */
public final class DefaultToolDefinitionMapper
        implements ToolDefinitionMapper {

    /**
     * Agent Tool을 LLM Tool 정의로 변환합니다.
     *
     * @param tool 변환할 Agent Tool
     * @return LLM Tool 정의
     * @throws ToolDefinitionMappingException Tool 정의가 올바르지 않거나
     *                                        변환에 실패한 경우
     */
    @Override
    public LlmToolDefinition map(AgentTool tool) {
        Objects.requireNonNull(
                tool,
                "tool must not be null"
        );

        String toolName = resolveToolName(tool);

        try {
            String description =
                    normalizeDescription(
                            tool.getDescription()
                    );

            Map<String, Object> parameters =
                    normalizeInputSchema(
                            tool.getInputSchema()
                    );

            return LlmToolDefinition.function(
                    toolName,
                    description,
                    parameters
            );

        } catch (ToolDefinitionMappingException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw ToolDefinitionMappingException.mappingFailed(
                    toolName,
                    exception
            );
        }
    }

    /**
     * Agent Tool 이름을 검증하고 반환합니다.
     */
    private String resolveToolName(AgentTool tool) {
        String name;

        try {
            name = tool.getName();

        } catch (RuntimeException exception) {
            throw ToolDefinitionMappingException.mappingFailed(
                    "unknown",
                    exception
            );
        }

        if (name == null || name.isBlank()) {
            throw ToolDefinitionMappingException.invalidTool(
                    "unknown",
                    "Tool name must not be blank."
            );
        }

        String normalized = name.trim();

        validateToolName(normalized);

        return normalized;
    }

    /**
     * Tool 이름이 LLM 함수명으로 사용할 수 있는지 검증합니다.
     *
     * <p>영문자 또는 밑줄로 시작하고, 이후에는 영문자·숫자·밑줄·
     * 하이픈만 허용합니다.</p>
     */
    private void validateToolName(String name) {
        if (!name.matches("[A-Za-z_][A-Za-z0-9_-]*")) {
            throw ToolDefinitionMappingException.invalidTool(
                    name,
                    "Tool name must match "
                            + "[A-Za-z_][A-Za-z0-9_-]*"
            );
        }

        if (name.length() > 128) {
            throw ToolDefinitionMappingException.invalidTool(
                    name,
                    "Tool name must not exceed 128 characters."
            );
        }
    }

    /**
     * Tool 설명을 정규화합니다.
     */
    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }

        return description.trim();
    }

    /**
     * Agent Tool 입력 스키마를 LLM Tool parameters 형식으로
     * 정규화합니다.
     */
    private Map<String, Object> normalizeInputSchema(
            Map<String, Object> inputSchema) {

        if (inputSchema == null || inputSchema.isEmpty()) {
            return createEmptyObjectSchema();
        }

        Map<String, Object> schema =
                deepCopyMap(inputSchema);

        Object schemaType = schema.get("type");

        if (schemaType == null) {
            schema.put("type", "object");

        } else if (!(schemaType instanceof String)
                || !"object".equals(schemaType)) {

            throw new ToolDefinitionMappingException(
                    "Tool input schema type must be object."
            );
        }

        Object properties = schema.get("properties");

        if (properties == null) {
            schema.put(
                    "properties",
                    new LinkedHashMap<String, Object>()
            );

        } else if (!(properties instanceof Map<?, ?>)) {
            throw new ToolDefinitionMappingException(
                    "Tool input schema properties "
                            + "must be an object."
            );
        }

        validateRequiredFields(schema);

        return schema;
    }

    /**
     * required 필드가 배열 또는 List 형태인지 검증합니다.
     */
    private void validateRequiredFields(
            Map<String, Object> schema) {

        Object required = schema.get("required");

        if (required == null) {
            return;
        }

        if (!(required instanceof List<?> requiredFields)) {
            throw new ToolDefinitionMappingException(
                    "Tool input schema required "
                            + "must be an array."
            );
        }

        for (Object field : requiredFields) {
            if (!(field instanceof String stringField)
                    || stringField.isBlank()) {

                throw new ToolDefinitionMappingException(
                        "Tool input schema required fields "
                                + "must contain non-blank strings."
                );
            }
        }

        Object propertiesObject =
                schema.get("properties");

        if (!(propertiesObject instanceof Map<?, ?> properties)) {
            return;
        }

        for (Object field : requiredFields) {
            if (!properties.containsKey(field)) {
                throw new ToolDefinitionMappingException(
                        "Required Tool parameter is not "
                                + "declared in properties: "
                                + field
                );
            }
        }
    }

    /**
     * 파라미터가 없는 Tool에 사용할 기본 JSON Schema를 생성합니다.
     */
    private Map<String, Object> createEmptyObjectSchema() {
        Map<String, Object> schema =
                new LinkedHashMap<>();

        schema.put("type", "object");
        schema.put(
                "properties",
                new LinkedHashMap<String, Object>()
        );

        return schema;
    }

    /**
     * 입력 스키마를 재귀적으로 복사합니다.
     *
     * <p>원본 Tool 스키마가 이후 변경되더라도 생성된
     * LLM 정의에 영향을 주지 않도록 합니다.</p>
     */
    private Map<String, Object> deepCopyMap(
            Map<?, ?> source) {

        Map<String, Object> copied =
                new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object key = entry.getKey();

            if (key == null) {
                throw new ToolDefinitionMappingException(
                        "Tool input schema must not "
                                + "contain null keys."
                );
            }

            copied.put(
                    String.valueOf(key),
                    deepCopyValue(entry.getValue())
            );
        }

        return copied;
    }

    /**
     * 스키마 값을 재귀적으로 복사합니다.
     */
    private Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return deepCopyMap(map);
        }

        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::deepCopyValue)
                    .toList();
        }

        return value;
    }
}