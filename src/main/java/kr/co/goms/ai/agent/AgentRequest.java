/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.llm.LlmAttachment;
import kr.co.goms.ai.llm.LlmMessage;

/**
 * AI Agent 실행 요청입니다.
 *
 * <p>사용자 요청, 이전 대화 메시지, 첨부파일, 메타데이터 및
 * Agent 실행 옵션을 하나의 객체로 전달합니다.</p>
 *
 * <p>인스턴스는 {@link Builder}를 통해 생성합니다.</p>
 *
 * <pre>
 * AgentRequest request = AgentRequest.builder()
 *         .instruction("현재 XHTML 문서의 접근성을 개선해 주세요.")
 *         .sessionId("session-001")
 *         .maxIterations(8)
 *         .toolCallingEnabled(true)
 *         .build();
 * </pre>
 */
public final class AgentRequest {

    /**
     * Agent 반복 실행 횟수 기본값입니다.
     */
    public static final int DEFAULT_MAX_ITERATIONS = 8;

    /**
     * Agent 반복 실행 횟수 최댓값입니다.
     *
     * <p>무한 Tool Calling 반복을 방지하기 위한 상한입니다.</p>
     */
    public static final int MAX_ALLOWED_ITERATIONS = 30;

    private final String requestId;
    private final String sessionId;
    private final String instruction;
    private final String systemPrompt;
    private final String model;
    private final List<LlmMessage> messages;
    private final List<LlmAttachment> attachments;
    private final Map<String, Object> attributes;
    private final int maxIterations;
    private final boolean toolCallingEnabled;
    private final boolean validationEnabled;

    private AgentRequest(Builder builder) {
        this.requestId = normalizeOptional(builder.requestId);
        this.sessionId = normalizeOptional(builder.sessionId);
        this.instruction = requireInstruction(builder.instruction);
        this.systemPrompt = normalizeOptional(builder.systemPrompt);
        this.model = normalizeOptional(builder.model);

        this.messages = immutableList(builder.messages);
        this.attachments = immutableList(builder.attachments);
        this.attributes = immutableMap(builder.attributes);

        this.maxIterations = validateMaxIterations(
                builder.maxIterations
        );

        this.toolCallingEnabled = builder.toolCallingEnabled;
        this.validationEnabled = builder.validationEnabled;
    }

    /**
     * 새로운 Builder를 생성합니다.
     *
     * @return AgentRequest Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기존 요청을 기반으로 Builder를 생성합니다.
     *
     * @param source 원본 Agent 요청
     * @return 원본 값이 복사된 Builder
     */
    public static Builder builder(AgentRequest source) {
        Objects.requireNonNull(
                source,
                "source must not be null"
        );

        return new Builder(source);
    }

    /**
     * 요청 식별자를 반환합니다.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 세션 식별자를 반환합니다.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 사용자의 핵심 실행 지시사항을 반환합니다.
     */
    public String getInstruction() {
        return instruction;
    }

    /**
     * 요청별 시스템 Prompt를 반환합니다.
     *
     * <p>값이 없으면 Agent 기본 시스템 Prompt를 사용합니다.</p>
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 요청에서 지정한 모델명을 반환합니다.
     *
     * <p>값이 없으면 LLM 클라이언트의 기본 모델을 사용합니다.</p>
     */
    public String getModel() {
        return model;
    }

    /**
     * 이전 대화 메시지를 반환합니다.
     *
     * @return 수정할 수 없는 메시지 목록
     */
    public List<LlmMessage> getMessages() {
        return messages;
    }

    /**
     * 요청에 포함된 첨부파일을 반환합니다.
     *
     * @return 수정할 수 없는 첨부파일 목록
     */
    public List<LlmAttachment> getAttachments() {
        return attachments;
    }

    /**
     * Agent 또는 Tool에 전달할 확장 속성을 반환합니다.
     *
     * @return 수정할 수 없는 속성 Map
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 최대 Agent 반복 횟수를 반환합니다.
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Tool Calling 사용 여부를 반환합니다.
     */
    public boolean isToolCallingEnabled() {
        return toolCallingEnabled;
    }

    /**
     * 생성 결과 검증 사용 여부를 반환합니다.
     */
    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    /**
     * 요청 식별자가 설정되어 있는지 확인합니다.
     */
    public boolean hasRequestId() {
        return requestId != null;
    }

    /**
     * 세션 식별자가 설정되어 있는지 확인합니다.
     */
    public boolean hasSessionId() {
        return sessionId != null;
    }

    /**
     * 요청별 시스템 Prompt가 설정되어 있는지 확인합니다.
     */
    public boolean hasSystemPrompt() {
        return systemPrompt != null;
    }

    /**
     * 요청별 모델이 설정되어 있는지 확인합니다.
     */
    public boolean hasModel() {
        return model != null;
    }

    /**
     * 이전 대화 메시지가 포함되어 있는지 확인합니다.
     */
    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    /**
     * 첨부파일이 포함되어 있는지 확인합니다.
     */
    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    /**
     * 확장 속성값을 반환합니다.
     *
     * @param name 속성명
     * @return 속성값 또는 {@code null}
     */
    public Object getAttribute(String name) {
        if (name == null) {
            return null;
        }

        return attributes.get(name);
    }

    /**
     * 확장 속성값을 지정한 타입으로 반환합니다.
     *
     * @param name 속성명
     * @param type 반환 타입
     * @param <T> 반환 타입
     * @return 변환된 속성값 또는 {@code null}
     * @throws IllegalArgumentException 속성값의 타입이 일치하지 않는 경우
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
                    "Agent request attribute type mismatch. "
                            + "name=" + name
                            + ", expected=" + type.getName()
                            + ", actual="
                            + value.getClass().getName()
            );
        }

        return type.cast(value);
    }

    private static String requireInstruction(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "instruction must not be blank"
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

    private static int validateMaxIterations(int value) {
        if (value < 1) {
            throw new IllegalArgumentException(
                    "maxIterations must be greater than 0"
            );
        }

        if (value > MAX_ALLOWED_ITERATIONS) {
            throw new IllegalArgumentException(
                    "maxIterations must not exceed "
                            + MAX_ALLOWED_ITERATIONS
            );
        }

        return value;
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
     * AgentRequest Builder입니다.
     */
    public static final class Builder {

        private String requestId;
        private String sessionId;
        private String instruction;
        private String systemPrompt;
        private String model;

        private final List<LlmMessage> messages =
                new ArrayList<>();

        private final List<LlmAttachment> attachments =
                new ArrayList<>();

        private final Map<String, Object> attributes =
                new LinkedHashMap<>();

        private int maxIterations =
                DEFAULT_MAX_ITERATIONS;

        private boolean toolCallingEnabled = true;
        private boolean validationEnabled = true;

        private Builder() {
        }

        private Builder(AgentRequest source) {
            this.requestId = source.requestId;
            this.sessionId = source.sessionId;
            this.instruction = source.instruction;
            this.systemPrompt = source.systemPrompt;
            this.model = source.model;
            this.messages.addAll(source.messages);
            this.attachments.addAll(source.attachments);
            this.attributes.putAll(source.attributes);
            this.maxIterations = source.maxIterations;
            this.toolCallingEnabled =
                    source.toolCallingEnabled;
            this.validationEnabled =
                    source.validationEnabled;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder instruction(String instruction) {
            this.instruction = instruction;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
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

        public Builder attachment(
                LlmAttachment attachment) {

            this.attachments.add(
                    Objects.requireNonNull(
                            attachment,
                            "attachment must not be null"
                    )
            );

            return this;
        }

        public Builder attachments(
                List<LlmAttachment> attachments) {

            Objects.requireNonNull(
                    attachments,
                    "attachments must not be null"
            );

            for (LlmAttachment attachment : attachments) {
                attachment(attachment);
            }

            return this;
        }

        public Builder clearAttachments() {
            this.attachments.clear();
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

        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder toolCallingEnabled(
                boolean toolCallingEnabled) {

            this.toolCallingEnabled =
                    toolCallingEnabled;

            return this;
        }

        public Builder validationEnabled(
                boolean validationEnabled) {

            this.validationEnabled =
                    validationEnabled;

            return this;
        }

        /**
         * AgentRequest를 생성합니다.
         */
        public AgentRequest build() {
            return new AgentRequest(this);
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
