/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Ollama Chat API의 메시지 객체입니다.
 *
 * <p>다음 메시지 유형을 지원합니다.</p>
 *
 * <ul>
 *     <li>system: 시스템 지시사항</li>
 *     <li>user: 사용자 요청</li>
 *     <li>assistant: LLM 응답 또는 Tool Call</li>
 *     <li>tool: Tool 실행 결과</li>
 * </ul>
 *
 * <p>Ollama JSON 예시:</p>
 *
 * <pre>
 * {
 *   "role": "assistant",
 *   "content": "",
 *   "thinking": "",
 *   "tool_calls": [
 *     {
 *       "type": "function",
 *       "function": {
 *         "name": "generate_xhtml",
 *         "arguments": {
 *           "title": "제1장"
 *         }
 *       }
 *     }
 *   ]
 * }
 * </pre>
 */
public final class OllamaChatMessage {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_TOOL = "tool";

    private String role;
    private String content;
    private String thinking;

    private List<String> images;

    @SerializedName("tool_calls")
    private List<OllamaToolCall> toolCalls;

    @SerializedName("tool_name")
    private String toolName;

    /**
     * Gson 역직렬화를 위한 기본 생성자입니다.
     */
    public OllamaChatMessage() {
        this.content = "";
        this.images = new ArrayList<>();
        this.toolCalls = new ArrayList<>();
    }

    /**
     * 일반 메시지를 생성합니다.
     *
     * @param role    메시지 역할
     * @param content 메시지 내용
     */
    public OllamaChatMessage(
            String role,
            String content) {

        this();
        setRole(role);
        setContent(content);
    }

    /**
     * Tool 결과 메시지를 생성합니다.
     *
     * @param toolName Tool 이름
     * @param content  Tool 실행 결과
     * @return Tool 역할 메시지
     */
    public static OllamaChatMessage toolResult(
            String toolName,
            String content) {

        OllamaChatMessage message =
                new OllamaChatMessage(
                        ROLE_TOOL,
                        content
                );

        message.setToolName(toolName);

        return message;
    }

    /**
     * Assistant Tool Call 메시지를 생성합니다.
     *
     * @param toolCalls Tool Call 목록
     * @return Assistant 역할 메시지
     */
    public static OllamaChatMessage assistantToolCalls(
            List<OllamaToolCall> toolCalls) {

        OllamaChatMessage message =
                new OllamaChatMessage(
                        ROLE_ASSISTANT,
                        ""
                );

        message.setToolCalls(toolCalls);

        return message;
    }

    /**
     * 메시지 역할을 반환합니다.
     */
    public String getRole() {
        return role;
    }

    /**
     * 메시지 역할을 설정합니다.
     */
    public void setRole(String role) {
        this.role = requireRole(role);
    }

    /**
     * 메시지 본문을 반환합니다.
     */
    public String getContent() {
        return content == null ? "" : content;
    }

    /**
     * 메시지 본문을 설정합니다.
     */
    public void setContent(String content) {
        this.content = content == null
                ? ""
                : content;
    }

    /**
     * 모델의 별도 추론 내용을 반환합니다.
     *
     * <p>Ollama 요청에서 {@code think} 옵션을 활성화한 경우
     * 설정될 수 있습니다.</p>
     */
    public String getThinking() {
        return thinking == null ? "" : thinking;
    }

    /**
     * 모델의 추론 내용을 설정합니다.
     */
    public void setThinking(String thinking) {
        this.thinking = normalizeOptional(thinking);
    }

    /**
     * Base64 이미지 목록을 반환합니다.
     *
     * @return 수정할 수 없는 이미지 목록
     */
    public List<String> getImages() {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(images);
    }

    /**
     * 이미지 목록을 설정합니다.
     */
    public void setImages(List<String> images) {
        this.images = copyNonBlankStrings(
                images,
                "images"
        );
    }

    /**
     * 이미지를 추가합니다.
     *
     * @param image Base64 이미지 문자열
     */
    public void addImage(String image) {
        if (image == null || image.isBlank()) {
            throw new IllegalArgumentException(
                    "image must not be blank"
            );
        }

        ensureImages();
        images.add(image.trim());
    }

    /**
     * 이미지가 포함되어 있는지 확인합니다.
     */
    public boolean hasImages() {
        return images != null
                && !images.isEmpty();
    }

    /**
     * Tool Call 목록을 반환합니다.
     *
     * @return 수정할 수 없는 Tool Call 목록
     */
    public List<OllamaToolCall> getToolCalls() {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(toolCalls);
    }

    /**
     * Tool Call 목록을 설정합니다.
     */
    public void setToolCalls(
            List<OllamaToolCall> toolCalls) {

        if (toolCalls == null || toolCalls.isEmpty()) {
            this.toolCalls = new ArrayList<>();
            return;
        }

        List<OllamaToolCall> copied =
                new ArrayList<>(toolCalls.size());

        for (OllamaToolCall toolCall : toolCalls) {
            copied.add(
                    Objects.requireNonNull(
                            toolCall,
                            "toolCalls must not contain null"
                    )
            );
        }

        this.toolCalls = copied;
    }

    /**
     * Tool Call을 추가합니다.
     */
    public void addToolCall(
            OllamaToolCall toolCall) {

        ensureToolCalls();

        toolCalls.add(
                Objects.requireNonNull(
                        toolCall,
                        "toolCall must not be null"
                )
        );
    }

    /**
     * Tool Call이 포함되어 있는지 확인합니다.
     */
    public boolean hasToolCalls() {
        return toolCalls != null
                && !toolCalls.isEmpty();
    }

    /**
     * Tool Call 개수를 반환합니다.
     */
    public int getToolCallCount() {
        return toolCalls == null
                ? 0
                : toolCalls.size();
    }

    /**
     * Tool 결과 메시지의 Tool 이름을 반환합니다.
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Tool 결과 메시지의 Tool 이름을 설정합니다.
     */
    public void setToolName(String toolName) {
        this.toolName = normalizeOptional(toolName);
    }

    /**
     * 시스템 메시지인지 확인합니다.
     */
    public boolean isSystem() {
        return ROLE_SYSTEM.equals(role);
    }

    /**
     * 사용자 메시지인지 확인합니다.
     */
    public boolean isUser() {
        return ROLE_USER.equals(role);
    }

    /**
     * Assistant 메시지인지 확인합니다.
     */
    public boolean isAssistant() {
        return ROLE_ASSISTANT.equals(role);
    }

    /**
     * Tool 실행 결과 메시지인지 확인합니다.
     */
    public boolean isTool() {
        return ROLE_TOOL.equals(role);
    }

    /**
     * 메시지 본문이 있는지 확인합니다.
     */
    public boolean hasContent() {
        return content != null
                && !content.isBlank();
    }

    /**
     * Thinking 내용이 있는지 확인합니다.
     */
    public boolean hasThinking() {
        return thinking != null
                && !thinking.isBlank();
    }

    /**
     * 메시지 상태를 검증합니다.
     */
    public void validate() {
        requireRole(role);

        if (isTool()
                && (toolName == null || toolName.isBlank())) {

            throw new IllegalStateException(
                    "Tool message must contain toolName"
            );
        }

        if (hasToolCalls() && !isAssistant()) {
            throw new IllegalStateException(
                    "toolCalls are only allowed "
                            + "for assistant messages"
            );
        }

        if (hasImages()
                && !isUser()
                && !isAssistant()) {

            throw new IllegalStateException(
                    "images are only allowed for "
                            + "user or assistant messages"
            );
        }
    }

    private void ensureImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
    }

    private void ensureToolCalls() {
        if (toolCalls == null) {
            toolCalls = new ArrayList<>();
        }
    }

    private static String requireRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "role must not be blank"
            );
        }

        String normalized = role.trim().toLowerCase();

        if (!ROLE_SYSTEM.equals(normalized)
                && !ROLE_USER.equals(normalized)
                && !ROLE_ASSISTANT.equals(normalized)
                && !ROLE_TOOL.equals(normalized)) {

            throw new IllegalArgumentException(
                    "Unsupported Ollama message role: "
                            + role
            );
        }

        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static List<String> copyNonBlankStrings(
            List<String> values,
            String fieldName) {

        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> copied =
                new ArrayList<>(values.size());

        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        fieldName
                                + " must not contain blank values"
                );
            }

            copied.add(value.trim());
        }

        return copied;
    }

    @Override
    public String toString() {
        return "OllamaChatMessage{"
                + "role='" + role + '\''
                + ", contentLength="
                + (content == null ? 0 : content.length())
                + ", thinkingLength="
                + (thinking == null ? 0 : thinking.length())
                + ", imageCount="
                + (images == null ? 0 : images.size())
                + ", toolCallCount="
                + (toolCalls == null ? 0 : toolCalls.size())
                + ", toolName='" + toolName + '\''
                + '}';
    }
}
