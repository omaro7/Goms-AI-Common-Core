/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.llm.LlmMessage;
import kr.co.goms.ai.tool.ToolResult;

/**
 * AI Agent 실행 결과입니다.
 *
 * <p>Agent의 최종 응답과 실행 상태, 반복 횟수, Tool 실행 결과,
 * 대화 메시지 및 실행 메타데이터를 포함합니다.</p>
 *
 * <p>인스턴스는 {@link Builder}를 통해 생성합니다.</p>
 */
public final class AgentResponse {

    private final String requestId;
    private final String sessionId;
    private final AgentStatus status;
    private final String content;
    private final String model;

    private final List<LlmMessage> messages;
    private final List<ToolResult> toolResults;
    private final Map<String, Object> attributes;

    private final int iterations;
    private final Instant startedAt;
    private final Instant completedAt;

    private final String errorCode;
    private final String errorMessage;
    private final Throwable cause;

    private AgentResponse(Builder builder) {
        this.requestId = normalizeOptional(builder.requestId);
        this.sessionId = normalizeOptional(builder.sessionId);

        this.status = Objects.requireNonNull(
                builder.status,
                "status must not be null"
        );

        this.content = normalizeContent(
                builder.content,
                builder.status
        );

        this.model = normalizeOptional(builder.model);

        this.messages = immutableList(builder.messages);
        this.toolResults = immutableList(builder.toolResults);
        this.attributes = immutableMap(builder.attributes);

        this.iterations = validateIterations(builder.iterations);
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;

        validateExecutionTime(startedAt, completedAt);

        this.errorCode = normalizeOptional(builder.errorCode);
        this.errorMessage = normalizeOptional(builder.errorMessage);
        this.cause = builder.cause;

        validateErrorState();
    }

    /**
     * 새로운 Builder를 생성합니다.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기존 응답을 기반으로 Builder를 생성합니다.
     */
    public static Builder builder(AgentResponse source) {
        Objects.requireNonNull(
                source,
                "source must not be null"
        );

        return new Builder(source);
    }

    /**
     * 성공 응답 Builder를 생성합니다.
     *
     * @param content 최종 Agent 응답
     */
    public static Builder success(String content) {
        return builder()
                .status(AgentStatus.COMPLETED)
                .content(content);
    }

    /**
     * 실패 응답 Builder를 생성합니다.
     *
     * @param errorMessage 오류 메시지
     */
    public static Builder failure(String errorMessage) {
        return builder()
                .status(AgentStatus.FAILED)
                .errorMessage(errorMessage);
    }

    /**
     * 실패 응답 Builder를 생성합니다.
     *
     * @param errorMessage 오류 메시지
     * @param cause        원인 예외
     */
    public static Builder failure(
            String errorMessage,
            Throwable cause) {

        return failure(errorMessage)
                .cause(cause);
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public AgentStatus getStatus() {
        return status;
    }

    /**
     * Agent의 최종 출력 문자열을 반환합니다.
     */
    public String getContent() {
        return content;
    }

    public String getModel() {
        return model;
    }

    /**
     * Agent 실행 중 구성된 전체 메시지 목록을 반환합니다.
     */
    public List<LlmMessage> getMessages() {
        return messages;
    }

    /**
     * Agent 실행 중 수행된 Tool 결과 목록을 반환합니다.
     */
    public List<ToolResult> getToolResults() {
        return toolResults;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * LLM 및 Tool Calling 반복 횟수를 반환합니다.
     */
    public int getIterations() {
        return iterations;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getCause() {
        return cause;
    }

    /**
     * Agent 실행이 정상적으로 완료되었는지 확인합니다.
     */
    public boolean isSuccess() {
        return status == AgentStatus.COMPLETED;
    }

    /**
     * Agent 실행이 실패했는지 확인합니다.
     */
    public boolean isFailure() {
        return status == AgentStatus.FAILED;
    }

    /**
     * 최대 반복 횟수에 도달했는지 확인합니다.
     */
    public boolean isIterationLimitReached() {
        return status == AgentStatus.ITERATION_LIMIT_REACHED;
    }

    public boolean hasContent() {
        return content != null && !content.isBlank();
    }

    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    public boolean hasToolResults() {
        return !toolResults.isEmpty();
    }

    public boolean hasError() {
        return errorMessage != null || cause != null;
    }

    /**
     * Agent 실행 시간을 반환합니다.
     *
     * <p>시작 또는 종료 시간이 없으면 {@code null}을 반환합니다.</p>
     */
    public Duration getDuration() {
        if (startedAt == null || completedAt == null) {
            return null;
        }

        return Duration.between(startedAt, completedAt);
    }

    public Object getAttribute(String name) {
        if (name == null) {
            return null;
        }

        return attributes.get(name);
    }

    /**
     * 속성값을 지정한 타입으로 반환합니다.
     */
    public <T> T getAttribute(
            String name,
            Class<T> type) {

        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        Object value = getAttribute(name);

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Agent response attribute type mismatch. "
                            + "name=" + name
                            + ", expected=" + type.getName()
                            + ", actual="
                            + value.getClass().getName()
            );
        }

        return type.cast(value);
    }

    private void validateErrorState() {
        if (status == AgentStatus.FAILED
                && errorMessage == null
                && cause == null) {

            throw new IllegalArgumentException(
                    "Failed AgentResponse must contain "
                            + "an errorMessage or cause"
            );
        }

        if (status == AgentStatus.COMPLETED
                && content == null) {

            throw new IllegalArgumentException(
                    "Completed AgentResponse must contain content"
            );
        }
    }

    private static String normalizeContent(
            String content,
            AgentStatus status) {

        if (content == null || content.isBlank()) {
            return status == AgentStatus.COMPLETED
                    ? null
                    : "";
        }

        return content.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static int validateIterations(int iterations) {
        if (iterations < 0) {
            throw new IllegalArgumentException(
                    "iterations must not be negative"
            );
        }

        return iterations;
    }

    private static void validateExecutionTime(
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

    private static <T> List<T> immutableList(
            List<T> source) {

        if (source == null || source.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(source)
        );
    }

    private static Map<String, Object> immutableMap(
            Map<String, Object> source) {

        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(source)
        );
    }

    /**
     * AgentResponse Builder입니다.
     */
    public static final class Builder {

        private String requestId;
        private String sessionId;
        private AgentStatus status = AgentStatus.COMPLETED;
        private String content;
        private String model;

        private final List<LlmMessage> messages =
                new ArrayList<>();

        private final List<ToolResult> toolResults =
                new ArrayList<>();

        private final Map<String, Object> attributes =
                new LinkedHashMap<>();

        private int iterations;
        private Instant startedAt;
        private Instant completedAt;

        private String errorCode;
        private String errorMessage;
        private Throwable cause;

        private Builder() {
        }

        private Builder(AgentResponse source) {
            this.requestId = source.requestId;
            this.sessionId = source.sessionId;
            this.status = source.status;
            this.content = source.content;
            this.model = source.model;

            this.messages.addAll(source.messages);
            this.toolResults.addAll(source.toolResults);
            this.attributes.putAll(source.attributes);

            this.iterations = source.iterations;
            this.startedAt = source.startedAt;
            this.completedAt = source.completedAt;

            this.errorCode = source.errorCode;
            this.errorMessage = source.errorMessage;
            this.cause = source.cause;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder status(AgentStatus status) {
            this.status = Objects.requireNonNull(
                    status,
                    "status must not be null"
            );

            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder message(LlmMessage message) {
            this.messages.add(
                    Objects.requireNonNull(
                            message,
                            "message must not be null"
                    )
            );

            return this;
        }

        public Builder messages(List<LlmMessage> messages) {
            Objects.requireNonNull(
                    messages,
                    "messages must not be null"
            );

            for (LlmMessage message : messages) {
                message(message);
            }

            return this;
        }

        public Builder clearMessages() {
            this.messages.clear();
            return this;
        }

        public Builder toolResult(ToolResult toolResult) {
            this.toolResults.add(
                    Objects.requireNonNull(
                            toolResult,
                            "toolResult must not be null"
                    )
            );

            return this;
        }

        public Builder toolResults(
                List<ToolResult> toolResults) {

            Objects.requireNonNull(
                    toolResults,
                    "toolResults must not be null"
            );

            for (ToolResult toolResult : toolResults) {
                toolResult(toolResult);
            }

            return this;
        }

        public Builder clearToolResults() {
            this.toolResults.clear();
            return this;
        }

        public Builder attribute(
                String name,
                Object value) {

            validateAttributeName(name);
            this.attributes.put(name, value);

            return this;
        }

        public Builder attributes(
                Map<String, ?> attributes) {

            Objects.requireNonNull(
                    attributes,
                    "attributes must not be null"
            );

            attributes.forEach(this::attribute);

            return this;
        }

        public Builder removeAttribute(String name) {
            validateAttributeName(name);
            this.attributes.remove(name);
            return this;
        }

        public Builder clearAttributes() {
            this.attributes.clear();
            return this;
        }

        public Builder iterations(int iterations) {
            this.iterations = iterations;
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
         * 현재 시각을 실행 시작 시각으로 설정합니다.
         */
        public Builder markStarted() {
            this.startedAt = Instant.now();
            return this;
        }

        /**
         * 현재 시각을 실행 완료 시각으로 설정합니다.
         */
        public Builder markCompleted() {
            this.completedAt = Instant.now();
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

        public AgentResponse build() {
            return new AgentResponse(this);
        }

        private static void validateAttributeName(
                String name) {

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "attribute name must not be blank"
                );
            }
        }
    }
}
