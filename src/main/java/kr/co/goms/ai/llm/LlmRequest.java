/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * LLM 호출 요청입니다.
 *
 * <p>대화 메시지, 사용할 모델, Tool 정의 및 생성 옵션을 포함합니다.</p>
 *
 * <p>일반 대화 요청과 Tool Calling 요청을 모두 지원합니다.</p>
 */
public final class LlmRequest {

    private final String model;
    private final List<LlmMessage> messages;
    private final List<LlmToolDefinition> tools;

    private final Double temperature;
    private final Integer maxTokens;
    private final boolean stream;

    /**
     * 일반적인 비스트리밍 LLM 요청을 생성합니다.
     *
     * @param model    사용할 모델명
     * @param messages 대화 메시지 목록
     */
    public LlmRequest(
            String model,
            List<LlmMessage> messages) {

        this(
                model,
                messages,
                List.of(),
                null,
                null,
                false
        );
    }

    /**
     * Tool 정의를 포함하는 비스트리밍 LLM 요청을 생성합니다.
     *
     * @param model    사용할 모델명
     * @param messages 대화 메시지 목록
     * @param tools    LLM이 사용할 수 있는 Tool 정의 목록
     */
    public LlmRequest(
            String model,
            List<LlmMessage> messages,
            List<LlmToolDefinition> tools) {

        this(
                model,
                messages,
                tools,
                null,
                null,
                false
        );
    }

    /**
     * 전체 옵션을 포함하는 LLM 요청을 생성합니다.
     *
     * @param model       사용할 모델명
     * @param messages    대화 메시지 목록
     * @param tools       Tool 정의 목록
     * @param temperature 생성 다양성 설정
     * @param maxTokens   최대 생성 토큰 수
     * @param stream      스트리밍 여부
     */
    public LlmRequest(
            String model,
            List<LlmMessage> messages,
            List<LlmToolDefinition> tools,
            Double temperature,
            Integer maxTokens,
            boolean stream) {

        this.model = normalizeOptional(model);
        this.messages = immutableMessages(messages);
        this.tools = immutableTools(tools);

        this.temperature =
                validateTemperature(temperature);

        this.maxTokens =
                validateMaxTokens(maxTokens);

        this.stream = stream;

        validate();
    }

    /**
     * Builder를 생성합니다.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기존 요청을 기반으로 Builder를 생성합니다.
     */
    public static Builder builder(LlmRequest source) {
        Objects.requireNonNull(
                source,
                "source must not be null"
        );

        return new Builder(source);
    }

    /**
     * 사용할 모델명을 반환합니다.
     *
     * <p>값이 없으면 LLM 클라이언트의 기본 모델을 사용할 수 있습니다.</p>
     */
    public String getModel() {
        return model;
    }

    /**
     * 대화 메시지 목록을 반환합니다.
     *
     * @return 수정할 수 없는 메시지 목록
     */
    public List<LlmMessage> getMessages() {
        return messages;
    }

    /**
     * LLM이 사용할 수 있는 Tool 정의 목록을 반환합니다.
     *
     * @return 수정할 수 없는 Tool 정의 목록
     */
    public List<LlmToolDefinition> getTools() {
        return tools;
    }

    /**
     * 생성 다양성 값을 반환합니다.
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     * 최대 생성 토큰 수를 반환합니다.
     */
    public Integer getMaxTokens() {
        return maxTokens;
    }

    /**
     * 스트리밍 사용 여부를 반환합니다.
     */
    public boolean isStream() {
        return stream;
    }

    /**
     * 모델이 요청에 명시되어 있는지 확인합니다.
     */
    public boolean hasModel() {
        return model != null;
    }

    /**
     * 메시지가 존재하는지 확인합니다.
     */
    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    /**
     * Tool 정의가 존재하는지 확인합니다.
     */
    public boolean hasTools() {
        return !tools.isEmpty();
    }

    /**
     * temperature가 설정되어 있는지 확인합니다.
     */
    public boolean hasTemperature() {
        return temperature != null;
    }

    /**
     * 최대 토큰 수가 설정되어 있는지 확인합니다.
     */
    public boolean hasMaxTokens() {
        return maxTokens != null;
    }

    /**
     * 요청의 마지막 메시지를 반환합니다.
     *
     * @return 마지막 메시지 또는 {@code null}
     */
    public LlmMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(messages.size() - 1);
    }

    /**
     * Tool Calling 요청인지 확인합니다.
     */
    public boolean isToolCallingRequest() {
        return hasTools();
    }

    private void validate() {
        if (messages.isEmpty()) {
            throw new IllegalArgumentException(
                    "messages must not be empty"
            );
        }

        boolean hasUserMessage = false;

        for (LlmMessage message : messages) {
            if (message.isUser()) {
                hasUserMessage = true;
                break;
            }
        }

        if (!hasUserMessage) {
            throw new IllegalArgumentException(
                    "LLM request must contain at least one user message"
            );
        }
    }

    private static List<LlmMessage> immutableMessages(
            List<LlmMessage> messages) {

        Objects.requireNonNull(
                messages,
                "messages must not be null"
        );

        if (messages.isEmpty()) {
            return List.of();
        }

        List<LlmMessage> copied =
                new ArrayList<>(messages.size());

        for (LlmMessage message : messages) {
            copied.add(
                    Objects.requireNonNull(
                            message,
                            "messages must not contain null"
                    )
            );
        }

        return Collections.unmodifiableList(copied);
    }

    private static List<LlmToolDefinition> immutableTools(
            List<LlmToolDefinition> tools) {

        if (tools == null || tools.isEmpty()) {
            return List.of();
        }

        List<LlmToolDefinition> copied =
                new ArrayList<>(tools.size());

        for (LlmToolDefinition tool : tools) {
            copied.add(
                    Objects.requireNonNull(
                            tool,
                            "tools must not contain null"
                    )
            );
        }

        return Collections.unmodifiableList(copied);
    }

    private static Double validateTemperature(
            Double temperature) {

        if (temperature == null) {
            return null;
        }

        if (temperature.isNaN()
                || temperature.isInfinite()) {

            throw new IllegalArgumentException(
                    "temperature must be a finite number"
            );
        }

        if (temperature < 0.0
                || temperature > 2.0) {

            throw new IllegalArgumentException(
                    "temperature must be between 0.0 and 2.0"
            );
        }

        return temperature;
    }

    private static Integer validateMaxTokens(
            Integer maxTokens) {

        if (maxTokens == null) {
            return null;
        }

        if (maxTokens < 1) {
            throw new IllegalArgumentException(
                    "maxTokens must be greater than 0"
            );
        }

        return maxTokens;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * LlmRequest Builder입니다.
     */
    public static final class Builder {

        private String model;

        private final List<LlmMessage> messages =
                new ArrayList<>();

        private final List<LlmToolDefinition> tools =
                new ArrayList<>();

        private Double temperature;
        private Integer maxTokens;
        private boolean stream;

        private Builder() {
        }

        private Builder(LlmRequest source) {
            this.model = source.model;
            this.messages.addAll(source.messages);
            this.tools.addAll(source.tools);
            this.temperature = source.temperature;
            this.maxTokens = source.maxTokens;
            this.stream = source.stream;
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

        public Builder messages(
                List<LlmMessage> messages) {

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

        public Builder systemMessage(String content) {
            return message(
                    LlmMessage.system(content)
            );
        }

        public Builder userMessage(String content) {
            return message(
                    LlmMessage.user(content)
            );
        }

        public Builder assistantMessage(String content) {
            return message(
                    LlmMessage.assistant(content)
            );
        }

        public Builder toolResultMessage(
                String toolCallId,
                String toolName,
                String content) {

            return message(
                    LlmMessage.toolResult(
                            toolCallId,
                            toolName,
                            content
                    )
            );
        }

        public Builder tool(
                LlmToolDefinition tool) {

            this.tools.add(
                    Objects.requireNonNull(
                            tool,
                            "tool must not be null"
                    )
            );

            return this;
        }

        public Builder tools(
                List<LlmToolDefinition> tools) {

            Objects.requireNonNull(
                    tools,
                    "tools must not be null"
            );

            for (LlmToolDefinition tool : tools) {
                tool(tool);
            }

            return this;
        }

        public Builder clearTools() {
            this.tools.clear();
            return this;
        }

        public Builder temperature(
                Double temperature) {

            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(
                Integer maxTokens) {

            this.maxTokens = maxTokens;
            return this;
        }

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public LlmRequest build() {
            return new LlmRequest(
                    model,
                    messages,
                    tools,
                    temperature,
                    maxTokens,
                    stream
            );
        }
    }

    @Override
    public String toString() {
        return "LlmRequest{"
                + "model='" + model + '\''
                + ", messageCount=" + messages.size()
                + ", toolCount=" + tools.size()
                + ", temperature=" + temperature
                + ", maxTokens=" + maxTokens
                + ", stream=" + stream
                + '}';
    }
}