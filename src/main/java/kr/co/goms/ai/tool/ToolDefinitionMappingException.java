/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

/**
 * Agent Tool을 LLM Tool 정의로 변환하는 과정에서 발생하는 예외입니다.
 */
public class ToolDefinitionMappingException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ToolDefinitionMappingException(String message) {
        super(message);
    }

    public ToolDefinitionMappingException(
            String message,
            Throwable cause) {

        super(message, cause);
    }

    public static ToolDefinitionMappingException invalidTool(
            String toolName,
            String message) {

        String normalizedName =
                toolName == null || toolName.isBlank()
                        ? "unknown"
                        : toolName.trim();

        return new ToolDefinitionMappingException(
                "Invalid Agent Tool definition. "
                        + "tool=" + normalizedName
                        + ", reason=" + message
        );
    }

    public static ToolDefinitionMappingException mappingFailed(
            String toolName,
            Throwable cause) {

        String normalizedName =
                toolName == null || toolName.isBlank()
                        ? "unknown"
                        : toolName.trim();

        return new ToolDefinitionMappingException(
                "Failed to map Agent Tool definition: "
                        + normalizedName,
                cause
        );
    }
}