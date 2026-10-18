/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

/**
 * Ollama LLM 연동 과정에서 발생하는 예외입니다.
 *
 * <p>다음과 같은 오류를 표현합니다.</p>
 *
 * <ul>
 *     <li>Ollama 서버 연결 실패</li>
 *     <li>HTTP API 호출 실패</li>
 *     <li>요청 JSON 직렬화 실패</li>
 *     <li>응답 JSON 역직렬화 실패</li>
 *     <li>잘못된 Ollama 응답</li>
 *     <li>Tool Call 변환 실패</li>
 *     <li>Tool arguments 변환 실패</li>
 * </ul>
 */
public class OllamaLlmException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final Integer statusCode;

    /**
     * 메시지만 포함하는 예외를 생성합니다.
     *
     * @param message 오류 메시지
     */
    public OllamaLlmException(String message) {
        this(
                null,
                null,
                message,
                null
        );
    }

    /**
     * 메시지와 원인 예외를 포함하는 예외를 생성합니다.
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public OllamaLlmException(
            String message,
            Throwable cause) {

        this(
                null,
                null,
                message,
                cause
        );
    }

    /**
     * 오류 코드와 메시지를 포함하는 예외를 생성합니다.
     *
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     */
    public OllamaLlmException(
            String errorCode,
            String message) {

        this(
                errorCode,
                null,
                message,
                null
        );
    }

    /**
     * 오류 코드, 메시지 및 원인 예외를 포함하는 예외를 생성합니다.
     *
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public OllamaLlmException(
            String errorCode,
            String message,
            Throwable cause) {

        this(
                errorCode,
                null,
                message,
                cause
        );
    }

    /**
     * HTTP 상태 코드를 포함하는 예외를 생성합니다.
     *
     * @param errorCode 오류 코드
     * @param statusCode HTTP 상태 코드
     * @param message 오류 메시지
     */
    public OllamaLlmException(
            String errorCode,
            Integer statusCode,
            String message) {

        this(
                errorCode,
                statusCode,
                message,
                null
        );
    }

    /**
     * Ollama 예외의 전체 정보를 포함하여 생성합니다.
     *
     * @param errorCode 오류 코드
     * @param statusCode HTTP 상태 코드
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public OllamaLlmException(
            String errorCode,
            Integer statusCode,
            String message,
            Throwable cause) {

        super(
                normalizeMessage(message),
                cause
        );

        this.errorCode =
                normalizeOptional(errorCode);

        this.statusCode =
                statusCode;

        validateStatusCode(statusCode);
    }

    /**
     * 오류 코드를 반환합니다.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * HTTP 상태 코드를 반환합니다.
     *
     * <p>HTTP 요청과 관련 없는 오류라면 {@code null}입니다.</p>
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * 오류 코드가 존재하는지 확인합니다.
     */
    public boolean hasErrorCode() {
        return errorCode != null;
    }

    /**
     * HTTP 상태 코드가 존재하는지 확인합니다.
     */
    public boolean hasStatusCode() {
        return statusCode != null;
    }

    /**
     * HTTP 오류인지 확인합니다.
     */
    public boolean isHttpError() {
        return statusCode != null
                && statusCode >= 400;
    }

    /**
     * 클라이언트 오류인지 확인합니다.
     */
    public boolean isClientError() {
        return statusCode != null
                && statusCode >= 400
                && statusCode < 500;
    }

    /**
     * Ollama 서버 오류인지 확인합니다.
     */
    public boolean isServerError() {
        return statusCode != null
                && statusCode >= 500
                && statusCode < 600;
    }

    /**
     * Ollama 서버에 연결할 수 없는 오류를 생성합니다.
     */
    public static OllamaLlmException connectionFailed(
            String endpoint,
            Throwable cause) {

        String normalizedEndpoint =
                normalizeOptional(endpoint);

        return new OllamaLlmException(
                ErrorCodes.CONNECTION_FAILED,
                null,
                normalizedEndpoint == null
                        ? "Failed to connect to Ollama server."
                        : "Failed to connect to Ollama server: "
                                + normalizedEndpoint,
                cause
        );
    }

    /**
     * Ollama HTTP API 오류를 생성합니다.
     */
    public static OllamaLlmException httpError(
            int statusCode,
            String responseBody) {

        String body =
                normalizeOptional(responseBody);

        String message =
                "Ollama API request failed. statusCode="
                        + statusCode;

        if (body != null) {
            message += ", responseBody=" + body;
        }

        return new OllamaLlmException(
                ErrorCodes.HTTP_ERROR,
                statusCode,
                message
        );
    }

    /**
     * 요청 직렬화 오류를 생성합니다.
     */
    public static OllamaLlmException serializationFailed(
            Throwable cause) {

        return new OllamaLlmException(
                ErrorCodes.SERIALIZATION_FAILED,
                "Failed to serialize Ollama request.",
                cause
        );
    }

    /**
     * 응답 역직렬화 오류를 생성합니다.
     */
    public static OllamaLlmException deserializationFailed(
            Throwable cause) {

        return new OllamaLlmException(
                ErrorCodes.DESERIALIZATION_FAILED,
                "Failed to deserialize Ollama response.",
                cause
        );
    }

    /**
     * Ollama 응답 형식 오류를 생성합니다.
     */
    public static OllamaLlmException invalidResponse(
            String message) {

        return new OllamaLlmException(
                ErrorCodes.INVALID_RESPONSE,
                message
        );
    }

    /**
     * Tool Call 변환 오류를 생성합니다.
     */
    public static OllamaLlmException invalidToolCall(
            String message) {

        return new OllamaLlmException(
                ErrorCodes.INVALID_TOOL_CALL,
                message
        );
    }

    /**
     * Tool arguments 변환 오류를 생성합니다.
     */
    public static OllamaLlmException invalidToolArguments(
            String message,
            Throwable cause) {

        return new OllamaLlmException(
                ErrorCodes.INVALID_TOOL_ARGUMENTS,
                message,
                cause
        );
    }

    /**
     * 요청 중단 오류를 생성합니다.
     */
    public static OllamaLlmException interrupted(
            Throwable cause) {

        return new OllamaLlmException(
                ErrorCodes.INTERRUPTED,
                "Ollama request was interrupted.",
                cause
        );
    }

    private static void validateStatusCode(
            Integer statusCode) {

        if (statusCode == null) {
            return;
        }

        if (statusCode < 100
                || statusCode > 599) {

            throw new IllegalArgumentException(
                    "Invalid HTTP status code: "
                            + statusCode
            );
        }
    }

    private static String normalizeMessage(
            String message) {

        if (message == null || message.isBlank()) {
            return "Unknown Ollama LLM error.";
        }

        return message.trim();
    }

    private static String normalizeOptional(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Ollama 연동 오류 코드입니다.
     */
    public static final class ErrorCodes {

        public static final String CONNECTION_FAILED =
                "OLLAMA_CONNECTION_FAILED";

        public static final String HTTP_ERROR =
                "OLLAMA_HTTP_ERROR";

        public static final String SERIALIZATION_FAILED =
                "OLLAMA_SERIALIZATION_FAILED";

        public static final String DESERIALIZATION_FAILED =
                "OLLAMA_DESERIALIZATION_FAILED";

        public static final String INVALID_RESPONSE =
                "OLLAMA_INVALID_RESPONSE";

        public static final String INVALID_TOOL_CALL =
                "OLLAMA_INVALID_TOOL_CALL";

        public static final String INVALID_TOOL_ARGUMENTS =
                "OLLAMA_INVALID_TOOL_ARGUMENTS";

        public static final String INTERRUPTED =
                "OLLAMA_REQUEST_INTERRUPTED";

        private ErrorCodes() {
            throw new AssertionError(
                    "Utility class"
            );
        }
    }
}