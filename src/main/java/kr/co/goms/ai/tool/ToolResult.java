/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import kr.co.goms.ai.util.ToolUtil;

/**
 * Tool 실행 결과를 나타내는 표준 응답 객체입니다.
 *
 * <p>Tool 실행 상태, 결과 데이터, 메시지, 검증 결과 및 오류 정보를
 * 포함합니다. Agent는 이 객체를 JSON으로 직렬화하여 LLM의
 * Tool 결과 메시지로 전달할 수 있습니다.</p>
 *
 * <p>사용 예시:</p>
 *
 * <pre>
 * ToolResult result = ToolResult.success("validate_xhtml")
 *         .requestId("request-001")
 *         .toolCallId("tool-call-001")
 *         .message("XHTML validation completed.")
 *         .data("valid", true)
 *         .build();
 * </pre>
 */
public final class ToolResult {

    private final String requestId;
    private final String toolCallId;
    private final String toolName;

    private final ToolStatus status;
    private final String message;

    private final Map<String, Object> data;
    private final List<ToolIssue> issues;

    private final ToolValidationResult validationResult;

    private final String errorCode;
    private final String errorMessage;

    /*
     * Throwable은 Gson 직렬화 대상에서 제외하는 편이 안전하므로
     * transient로 선언합니다.
     */
    private final transient Throwable cause;

    private final Instant startedAt;
    private final Instant completedAt;

    private ToolResult(Builder builder) {
        this.requestId =
                normalizeOptional(builder.requestId);

        this.toolCallId =
                normalizeOptional(builder.toolCallId);

        this.toolName =
                requireToolName(builder.toolName);

        this.status = Objects.requireNonNull(
                builder.status,
                "status must not be null"
        );

        this.message =
                normalizeOptional(builder.message);

        this.data =
                immutableMap(builder.data);

        this.issues =
                immutableIssues(builder.issues);

        this.validationResult =
                builder.validationResult;

        this.errorCode =
                normalizeOptional(builder.errorCode);

        this.errorMessage =
                normalizeOptional(builder.errorMessage);

        this.cause = builder.cause;

        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;

        validateExecutionTimes(
                startedAt,
                completedAt
        );

        validateState();
    }

    /**
     * 새로운 Builder를 생성합니다.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기존 ToolResult를 기반으로 Builder를 생성합니다.
     */
    public static Builder builder(ToolResult source) {
        Objects.requireNonNull(
                source,
                "source must not be null"
        );

        return new Builder(source);
    }

    /**
     * 성공 ToolResult Builder를 생성합니다.
     *
     * @param toolName Tool 이름
     */
    public static Builder success(String toolName) {
        return builder()
                .toolName(toolName)
                .status(ToolStatus.SUCCESS);
    }

    /**
     * 실패 ToolResult Builder를 생성합니다.
     *
     * @param toolName    Tool 이름
     * @param errorMessage 오류 메시지
     */
    public static Builder failure(
            String toolName,
            String errorMessage) {

        return builder()
                .toolName(toolName)
                .status(ToolStatus.FAILED)
                .errorMessage(errorMessage);
    }

    /**
     * 실패 ToolResult Builder를 생성합니다.
     */
    public static Builder failure(
            String toolName,
            String errorMessage,
            Throwable cause) {

        return failure(toolName, errorMessage)
                .cause(cause);
    }

    /**
     * 요청 식별자를 반환합니다.
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
     * 실행한 Tool 이름을 반환합니다.
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Tool 실행 상태를 반환합니다.
     */
    public ToolStatus getStatus() {
        return status;
    }

    /**
     * Tool 실행 메시지를 반환합니다.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Tool 실행 결과 데이터를 반환합니다.
     *
     * @return 수정할 수 없는 결과 Map
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * Tool 실행 또는 검증 이슈를 반환합니다.
     *
     * @return 수정할 수 없는 이슈 목록
     */
    public List<ToolIssue> getIssues() {
        return issues;
    }

    /**
     * Tool 요청 검증 결과를 반환합니다.
     */
    public ToolValidationResult getValidationResult() {
        return validationResult;
    }

    /**
     * 오류 코드를 반환합니다.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 오류 메시지를 반환합니다.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 원인 예외를 반환합니다.
     *
     * <p>이 값은 JSON 직렬화에서 제외됩니다.</p>
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Tool 실행 시작 시각을 반환합니다.
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Tool 실행 완료 시각을 반환합니다.
     */
    public Instant getCompletedAt() {
        return completedAt;
    }

    /**
     * Tool 실행이 성공했는지 확인합니다.
     */
    public boolean isSuccess() {
        return status == ToolStatus.SUCCESS;
    }

    /**
     * Tool 실행이 실패했는지 확인합니다.
     */
    public boolean isFailure() {
        return status == ToolStatus.FAILED
                || status == ToolStatus.VALIDATION_FAILED
                || status == ToolStatus.CANCELLED
                || status == ToolStatus.TIMEOUT;
    }

    /**
     * Tool 실행이 완료된 상태인지 확인합니다.
     */
    public boolean isCompleted() {
        return status.isTerminal();
    }

    /**
     * Tool 실행 결과 데이터가 존재하는지 확인합니다.
     */
    public boolean hasData() {
        return !data.isEmpty();
    }

    /**
     * Tool 이슈가 존재하는지 확인합니다.
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * 검증 결과가 존재하는지 확인합니다.
     */
    public boolean hasValidationResult() {
        return validationResult != null;
    }

    /**
     * 오류 정보가 존재하는지 확인합니다.
     */
    public boolean hasError() {
        return errorCode != null
                || errorMessage != null
                || cause != null;
    }

    /**
     * 메시지가 존재하는지 확인합니다.
     */
    public boolean hasMessage() {
        return message != null;
    }

    /**
     * Tool Call 식별자가 존재하는지 확인합니다.
     */
    public boolean hasToolCallId() {
        return toolCallId != null;
    }

    /**
     * 요청 식별자가 존재하는지 확인합니다.
     */
    public boolean hasRequestId() {
        return requestId != null;
    }

    /**
     * 지정한 결과 데이터를 반환합니다.
     */
    public Object getData(String name) {
        if (name == null) {
            return null;
        }

        return data.get(name);
    }

    /**
     * 지정한 결과 데이터를 특정 타입으로 반환합니다.
     */
    public <T> T getData(
            String name,
            Class<T> type) {

        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        Object value = getData(name);

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Tool result data type mismatch. "
                            + "tool=" + toolName
                            + ", name=" + name
                            + ", expected=" + type.getName()
                            + ", actual="
                            + value.getClass().getName()
            );
        }

        return type.cast(value);
    }

    /**
     * 지정한 결과 데이터를 기본값과 함께 반환합니다.
     */
    public <T> T getDataOrDefault(
            String name,
            Class<T> type,
            T defaultValue) {

        T value = getData(name, type);

        return value != null
                ? value
                : defaultValue;
    }

    /**
     * Tool 실행 시간을 밀리초 단위로 반환합니다.
     *
     * @return 실행 시간 또는 계산할 수 없는 경우 {@code -1}
     */
    public long getDurationMillis() {
        if (startedAt == null || completedAt == null) {
            return -1L;
        }

        return java.time.Duration
                .between(startedAt, completedAt)
                .toMillis();
    }

    /**
     * 상태와 오류 정보의 일관성을 검증합니다.
     */
    private void validateState() {
        if (status == ToolStatus.SUCCESS
                && hasError()) {

            throw new IllegalArgumentException(
                    "Successful ToolResult must not contain error information"
            );
        }

        if (status == ToolStatus.FAILED
                && !hasError()) {

            throw new IllegalArgumentException(
                    "Failed ToolResult must contain error information"
            );
        }

        if (status == ToolStatus.VALIDATION_FAILED
                && validationResult == null
                && issues.isEmpty()
                && errorMessage == null) {

            throw new IllegalArgumentException(
                    "Validation failed ToolResult must contain "
                            + "validationResult, issues, or errorMessage"
            );
        }
    }

    private static String requireToolName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "toolName must not be blank"
            );
        }

        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static void validateExecutionTimes(
            Instant startedAt,
            Instant completedAt) {

        if (startedAt != null
                && completedAt != null
                && completedAt.isBefore(startedAt)) {

            throw new IllegalArgumentException(
                    "completedAt must not be before startedAt"
            );
        }
    }

    private static List<ToolIssue> immutableIssues(
            List<ToolIssue> source) {

        if (source == null || source.isEmpty()) {
            return List.of();
        }

        List<ToolIssue> copied =
                new ArrayList<>(source.size());

        for (ToolIssue issue : source) {
            copied.add(
                    Objects.requireNonNull(
                            issue,
                            "issues must not contain null"
                    )
            );
        }

        return Collections.unmodifiableList(copied);
    }

    private static Map<String, Object> immutableMap(
            Map<String, Object> source) {

        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> copied =
                new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry
                : source.entrySet()) {

            String key = entry.getKey();

            validateDataName(key);

            copied.put(
                    key.trim(),
                    ToolUtil.deepCopy(entry.getValue())
            );
        }

        return Collections.unmodifiableMap(copied);
    }

    private static void validateDataName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Tool result data name must not be blank"
            );
        }
    }

    @Override
    public String toString() {
        return "ToolResult{"
                + "requestId='" + requestId + '\''
                + ", toolCallId='" + toolCallId + '\''
                + ", toolName='" + toolName + '\''
                + ", status=" + status
                + ", message='" + message + '\''
                + ", dataNames=" + data.keySet()
                + ", issueCount=" + issues.size()
                + ", errorCode='" + errorCode + '\''
                + ", errorMessage='" + errorMessage + '\''
                + ", startedAt=" + startedAt
                + ", completedAt=" + completedAt
                + '}';
    }

    /**
     * ToolResult Builder입니다.
     */
    public static final class Builder {

        private String requestId;
        private String toolCallId;
        private String toolName;

        private ToolStatus status =
                ToolStatus.SUCCESS;

        private String message;

        private final Map<String, Object> data =
                new LinkedHashMap<>();

        private final List<ToolIssue> issues =
                new ArrayList<>();

        private ToolValidationResult validationResult;

        private String errorCode;
        private String errorMessage;
        private Throwable cause;

        private Instant startedAt;
        private Instant completedAt;

        private Builder() {
        }

        private Builder(ToolResult source) {
            this.requestId = source.requestId;
            this.toolCallId = source.toolCallId;
            this.toolName = source.toolName;
            this.status = source.status;
            this.message = source.message;

            this.data.putAll(source.data);
            this.issues.addAll(source.issues);

            this.validationResult =
                    source.validationResult;

            this.errorCode = source.errorCode;
            this.errorMessage = source.errorMessage;
            this.cause = source.cause;

            this.startedAt = source.startedAt;
            this.completedAt = source.completedAt;
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

        public Builder status(ToolStatus status) {
            this.status = Objects.requireNonNull(
                    status,
                    "status must not be null"
            );

            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * 결과 데이터를 추가하거나 변경합니다.
         */
        public Builder data(
                String name,
                Object value) {

            validateDataName(name);

            this.data.put(
                    name.trim(),
                    value
            );

            return this;
        }

        /**
         * 여러 결과 데이터를 추가합니다.
         */
        public Builder data(
                Map<String, ?> values) {

            Objects.requireNonNull(
                    values,
                    "values must not be null"
            );

            for (Map.Entry<String, ?> entry
                    : values.entrySet()) {

                data(
                        entry.getKey(),
                        entry.getValue()
                );
            }

            return this;
        }

        public Builder removeData(String name) {
            validateDataName(name);
            this.data.remove(name);
            return this;
        }

        public Builder clearData() {
            this.data.clear();
            return this;
        }

        /**
         * Tool 이슈를 추가합니다.
         */
        public Builder issue(ToolIssue issue) {
            this.issues.add(
                    Objects.requireNonNull(
                            issue,
                            "issue must not be null"
                    )
            );

            return this;
        }

        /**
         * 여러 Tool 이슈를 추가합니다.
         */
        public Builder issues(List<ToolIssue> issues) {
            Objects.requireNonNull(
                    issues,
                    "issues must not be null"
            );

            for (ToolIssue issue : issues) {
                issue(issue);
            }

            return this;
        }

        public Builder clearIssues() {
            this.issues.clear();
            return this;
        }

        public Builder validationResult(
                ToolValidationResult validationResult) {

            this.validationResult = validationResult;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(Instant completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        /**
         * 현재 시각을 Tool 실행 시작 시각으로 설정합니다.
         */
        public Builder markStarted() {
            this.startedAt = Instant.now();
            return this;
        }

        /**
         * 현재 시각을 Tool 실행 완료 시각으로 설정합니다.
         */
        public Builder markCompleted() {
            this.completedAt = Instant.now();
            return this;
        }

        /**
         * ToolResult를 생성합니다.
         */
        public ToolResult build() {
            return new ToolResult(this);
        }
    }
}