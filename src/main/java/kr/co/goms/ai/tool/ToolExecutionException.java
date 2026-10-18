/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

/**
 * Tool 조회, 검증 또는 실행 과정에서 발생하는 예외입니다.
 */
public class ToolExecutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String toolName;

    public ToolExecutionException(String message) {
        this(null, null, message, null);
    }

    public ToolExecutionException(
            String message,
            Throwable cause) {

        this(null, null, message, cause);
    }

    public ToolExecutionException(
            String errorCode,
            String toolName,
            String message) {

        this(errorCode, toolName, message, null);
    }

    public ToolExecutionException(
            String errorCode,
            String toolName,
            String message,
            Throwable cause) {

        super(message, cause);

        this.errorCode =
                normalizeOptional(errorCode);

        this.toolName =
                normalizeOptional(toolName);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getToolName() {
        return toolName;
    }

    public boolean hasErrorCode() {
        return errorCode != null;
    }

    public boolean hasToolName() {
        return toolName != null;
    }

    public static ToolExecutionException invalidRequest(
            String message) {

        return new ToolExecutionException(
                ErrorCodes.INVALID_REQUEST,
                null,
                requireMessage(message)
        );
    }

    public static ToolExecutionException toolNotFound(
            String toolName) {

        String normalizedToolName =
                normalizeToolName(toolName);

        return new ToolExecutionException(
                ErrorCodes.TOOL_NOT_FOUND,
                normalizedToolName,
                "Tool not found: " + normalizedToolName
        );
    }

    public static ToolExecutionException toolUnavailable(
            String toolName) {

        String normalizedToolName =
                normalizeToolName(toolName);

        return new ToolExecutionException(
                ErrorCodes.TOOL_UNAVAILABLE,
                normalizedToolName,
                "Tool is not available: "
                        + normalizedToolName
        );
    }

    public static ToolExecutionException registryFailed(
            String toolName,
            Throwable cause) {

        String normalizedToolName =
                normalizeToolName(toolName);

        return new ToolExecutionException(
                ErrorCodes.REGISTRY_FAILED,
                normalizedToolName,
                "Failed to resolve Tool from registry: "
                        + normalizedToolName,
                cause
        );
    }

    public static ToolExecutionException validationFailed(
            String toolName,
            Throwable cause) {

        String normalizedToolName =
                normalizeToolName(toolName);

        return new ToolExecutionException(
                ErrorCodes.VALIDATION_FAILED,
                normalizedToolName,
                "Tool request validation failed: "
                        + normalizedToolName,
                cause
        );
    }

    public static ToolExecutionException executionFailed(String toolName, Throwable cause) {

        String normalizedToolName = normalizeToolName(toolName);
        String causeMessage = cause != null && cause.getMessage() != null && !cause.getMessage().isBlank() ? cause.getMessage().trim() : "Unknown error.";

        return new ToolExecutionException(
                ErrorCodes.EXECUTION_FAILED,
                normalizedToolName,
                "Tool execution failed: " + normalizedToolName + " | " + causeMessage,
                cause
        );
    }

    public static ToolExecutionException invalidResult(
            String toolName,
            String message) {

        String normalizedToolName =
                normalizeToolName(toolName);

        return new ToolExecutionException(
                ErrorCodes.INVALID_RESULT,
                normalizedToolName,
                "Invalid Tool result. tool="
                        + normalizedToolName
                        + ", reason="
                        + requireMessage(message)
        );
    }

    public static ToolExecutionException resultSerializationFailed(
            String toolName,
            Throwable cause) {

        String normalizedToolName =
                normalizeToolName(toolName);

        return new ToolExecutionException(
                ErrorCodes.RESULT_SERIALIZATION_FAILED,
                normalizedToolName,
                "Failed to serialize Tool result: "
                        + normalizedToolName,
                cause
        );
    }

    public static ToolExecutionException duplicateExecution(
            String toolName,
            String toolCallId) {

        String normalizedToolName =
                normalizeToolName(toolName);

        String normalizedToolCallId =
                normalizeOptional(toolCallId);

        return new ToolExecutionException(
                ErrorCodes.DUPLICATE_EXECUTION,
                normalizedToolName,
                "Duplicate Tool execution request. "
                        + "tool=" + normalizedToolName
                        + ", toolCallId="
                        + (normalizedToolCallId == null
                                ? "unknown"
                                : normalizedToolCallId)
        );
    }

    public static ToolExecutionException timeout(
            String toolName,
            Throwable cause) {

        String normalizedToolName =
                normalizeToolName(toolName);

        return new ToolExecutionException(
                ErrorCodes.TIMEOUT,
                normalizedToolName,
                "Tool execution timed out: "
                        + normalizedToolName,
                cause
        );
    }

    public static ToolExecutionException cancelled(
            String toolName) {

        String normalizedToolName =
                normalizeToolName(toolName);

        return new ToolExecutionException(
                ErrorCodes.CANCELLED,
                normalizedToolName,
                "Tool execution was cancelled: "
                        + normalizedToolName
        );
    }

    private static String normalizeToolName(
            String toolName) {

        if (toolName == null || toolName.isBlank()) {
            return "unknown";
        }

        return toolName.trim();
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown Tool execution error.";
        }

        return message.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Tool 실행 오류 코드입니다.
     */
    public static final class ErrorCodes {

        public static final String INVALID_REQUEST =
                "TOOL_INVALID_REQUEST";

        public static final String TOOL_NOT_FOUND =
                "TOOL_NOT_FOUND";

        public static final String TOOL_UNAVAILABLE =
                "TOOL_UNAVAILABLE";

        public static final String REGISTRY_FAILED =
                "TOOL_REGISTRY_FAILED";

        public static final String VALIDATION_FAILED =
                "TOOL_VALIDATION_FAILED";

        public static final String EXECUTION_FAILED =
                "TOOL_EXECUTION_FAILED";

        public static final String INVALID_RESULT =
                "TOOL_INVALID_RESULT";

        public static final String RESULT_SERIALIZATION_FAILED =
                "TOOL_RESULT_SERIALIZATION_FAILED";

        public static final String DUPLICATE_EXECUTION =
                "TOOL_DUPLICATE_EXECUTION";

        public static final String TIMEOUT =
                "TOOL_EXECUTION_TIMEOUT";

        public static final String CANCELLED =
                "TOOL_EXECUTION_CANCELLED";

        private ErrorCodes() {
            throw new AssertionError("Utility class");
        }
    }
}