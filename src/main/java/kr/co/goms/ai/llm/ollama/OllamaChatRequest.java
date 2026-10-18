/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Ollama Chat API {@code /api/chat} 요청 객체입니다.
 *
 * <p>일반 대화, Tool Calling, 생성 옵션 및 스트리밍 설정을
 * 지원합니다.</p>
 */
public final class OllamaChatRequest {

    private String model;

    private List<OllamaChatMessage> messages;

    private List<OllamaToolDefinition> tools;

    private Map<String, Object> options;

    private boolean stream;

    private Object format;

    private Boolean think;

    @SerializedName("keep_alive")
    private Object keepAlive;

    /**
     * Gson 직렬화 및 일반 생성을 위한 기본 생성자입니다.
     */
    public OllamaChatRequest() {
        this.messages = new ArrayList<>();
        this.tools = new ArrayList<>();
        this.options = new LinkedHashMap<>();
        this.stream = false;
    }

    /**
     * 기본 Chat 요청을 생성합니다.
     *
     * @param model    모델명
     * @param messages 대화 메시지
     */
    public OllamaChatRequest(
            String model,
            List<OllamaChatMessage> messages) {

        this();
        setModel(model);
        setMessages(messages);
    }

    /**
     * Tool Calling을 포함하는 Chat 요청을 생성합니다.
     *
     * @param model    모델명
     * @param messages 대화 메시지
     * @param tools    Tool 정의 목록
     */
    public OllamaChatRequest(
            String model,
            List<OllamaChatMessage> messages,
            List<OllamaToolDefinition> tools) {

        this(model, messages);
        setTools(tools);
    }

    /**
     * 사용할 모델명을 반환합니다.
     */
    public String getModel() {
        return model;
    }

    /**
     * 사용할 모델명을 설정합니다.
     */
    public void setModel(String model) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "model must not be blank"
            );
        }

        this.model = model.trim();
    }

    /**
     * 대화 메시지 목록을 반환합니다.
     */
    public List<OllamaChatMessage> getMessages() {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(messages);
    }

    /**
     * 대화 메시지 목록을 설정합니다.
     */
    public void setMessages(
            List<OllamaChatMessage> messages) {

        Objects.requireNonNull(
                messages,
                "messages must not be null"
        );

        List<OllamaChatMessage> copied =
                new ArrayList<>(messages.size());

        for (OllamaChatMessage message : messages) {
            copied.add(
                    Objects.requireNonNull(
                            message,
                            "messages must not contain null"
                    )
            );
        }

        this.messages = copied;
    }

    /**
     * 대화 메시지를 추가합니다.
     */
    public void addMessage(
            OllamaChatMessage message) {

        ensureMessages();

        messages.add(
                Objects.requireNonNull(
                        message,
                        "message must not be null"
                )
        );
    }

    /**
     * Tool 정의 목록을 반환합니다.
     */
    public List<OllamaToolDefinition> getTools() {
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(tools);
    }

    /**
     * Tool 정의 목록을 설정합니다.
     */
    public void setTools(
            List<OllamaToolDefinition> tools) {

        if (tools == null || tools.isEmpty()) {
            this.tools = new ArrayList<>();
            return;
        }

        List<OllamaToolDefinition> copied =
                new ArrayList<>(tools.size());

        for (OllamaToolDefinition tool : tools) {
            copied.add(
                    Objects.requireNonNull(
                            tool,
                            "tools must not contain null"
                    )
            );
        }

        this.tools = copied;
    }

    /**
     * Tool 정의를 추가합니다.
     */
    public void addTool(
            OllamaToolDefinition tool) {

        ensureTools();

        tools.add(
                Objects.requireNonNull(
                        tool,
                        "tool must not be null"
                )
        );
    }

    /**
     * 생성 옵션을 반환합니다.
     *
     * @return 수정할 수 없는 옵션 Map
     */
    public Map<String, Object> getOptions() {
        if (options == null || options.isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(options);
    }

    /**
     * 생성 옵션을 설정합니다.
     */
    public void setOptions(
            Map<String, Object> options) {

        if (options == null || options.isEmpty()) {
            this.options = new LinkedHashMap<>();
            return;
        }

        this.options = new LinkedHashMap<>(options);
    }

    /**
     * 생성 옵션 하나를 설정합니다.
     */
    public void setOption(
            String name,
            Object value) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "option name must not be blank"
            );
        }

        ensureOptions();
        options.put(name.trim(), value);
    }

    /**
     * 생성 옵션을 제거합니다.
     */
    public Object removeOption(String name) {
        if (name == null || options == null) {
            return null;
        }

        return options.remove(name);
    }

    /**
     * 스트리밍 사용 여부를 반환합니다.
     */
    public boolean isStream() {
        return stream;
    }

    /**
     * 스트리밍 사용 여부를 설정합니다.
     */
    public void setStream(boolean stream) {
        this.stream = stream;
    }

    /**
     * 출력 형식을 반환합니다.
     *
     * <p>문자열 {@code "json"} 또는 JSON Schema 객체를 사용할 수
     * 있습니다.</p>
     */
    public Object getFormat() {
        return format;
    }

    /**
     * 출력 형식을 설정합니다.
     */
    public void setFormat(Object format) {
        this.format = format;
    }

    /**
     * Thinking 모드 설정을 반환합니다.
     */
    public Boolean getThink() {
        return think;
    }

    /**
     * Thinking 모드 사용 여부를 설정합니다.
     */
    public void setThink(Boolean think) {
        this.think = think;
    }

    /**
     * 모델 유지 설정을 반환합니다.
     *
     * <p>문자열 또는 숫자 형식을 사용할 수 있습니다.</p>
     */
    public Object getKeepAlive() {
        return keepAlive;
    }

    /**
     * 모델 유지 설정을 지정합니다.
     */
    public void setKeepAlive(Object keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * Tool 정의가 존재하는지 확인합니다.
     */
    public boolean hasTools() {
        return tools != null
                && !tools.isEmpty();
    }

    /**
     * 생성 옵션이 존재하는지 확인합니다.
     */
    public boolean hasOptions() {
        return options != null
                && !options.isEmpty();
    }

    /**
     * 메시지가 존재하는지 확인합니다.
     */
    public boolean hasMessages() {
        return messages != null
                && !messages.isEmpty();
    }

    /**
     * 요청 객체를 검증합니다.
     */
    public void validate() {
        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                    "Ollama request model is missing"
            );
        }

        if (!hasMessages()) {
            throw new IllegalStateException(
                    "Ollama request messages are missing"
            );
        }

        for (OllamaChatMessage message : messages) {
            message.validate();
        }

        if (hasTools()) {
            for (OllamaToolDefinition tool : tools) {
                tool.validate();
            }
        }
    }

    private void ensureMessages() {
        if (messages == null) {
            messages = new ArrayList<>();
        }
    }

    private void ensureTools() {
        if (tools == null) {
            tools = new ArrayList<>();
        }
    }

    private void ensureOptions() {
        if (options == null) {
            options = new LinkedHashMap<>();
        }
    }

    @Override
    public String toString() {
        return "OllamaChatRequest{"
                + "model='" + model + '\''
                + ", messageCount="
                + (messages == null ? 0 : messages.size())
                + ", toolCount="
                + (tools == null ? 0 : tools.size())
                + ", optionCount="
                + (options == null ? 0 : options.size())
                + ", stream=" + stream
                + ", format=" + format
                + ", think=" + think
                + ", keepAlive=" + keepAlive
                + '}';
    }
}