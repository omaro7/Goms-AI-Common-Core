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
 * LLM 대화 메시지입니다.
 *
 * <p>
 * 일반적인 system, user, assistant 메시지뿐 아니라
 * Assistant의 Tool Call 요청, Tool 실행 결과 메시지,
 * 이미지·PDF·EPUB 등의 Attachment를 함께 표현합니다.
 * </p>
 *
 * <p>
 * Attachment는 Provider 독립적인 형태로 보관하며,
 * 실제 OpenAI, Ollama, Gemini 등의 Provider 구현체가
 * 각 Provider API 형식으로 변환합니다.
 * </p>
 *
 * <p>예시:</p>
 *
 * <pre>
 * LlmAttachment image =
 *         LlmAttachment.image(
 *                 Path.of("cover.png")
 *         );
 *
 * LlmMessage message =
 *         LlmMessage
 *                 .user("이미지를 분석해주세요.")
 *                 .withAttachment(image);
 * </pre>
 */
public final class LlmMessage {

    /**
     * 메시지 역할.
     */
    private final LlmRole role;

    /**
     * 메시지 본문.
     */
    private final String content;

    /**
     * 메시지 또는 Tool 이름.
     *
     * <p>
     * 일반 메시지에서는 {@code null}이며,
     * Tool 결과 메시지에서는 실행한 Tool 이름을 저장합니다.
     * </p>
     */
    private final String name;

    /**
     * Tool Call 식별자.
     *
     * <p>
     * Tool 실행 결과를 Assistant의 Tool Call과 연결할 때 사용합니다.
     * </p>
     */
    private final String toolCallId;

    /**
     * Assistant가 요청한 Tool Call 목록.
     */
    private final List<LlmToolCall> toolCalls;

    /**
     * 메시지에 포함된 첨부 파일 목록.
     *
     * <p>
     * 이미지, PDF, EPUB, Markdown 등의 입력을 표현합니다.
     * 일반적으로 USER 메시지에 사용됩니다.
     * </p>
     */
    private final List<LlmAttachment> attachments;

    /**
     * 일반 LLM 메시지를 생성합니다.
     *
     * @param role    메시지 역할
     * @param content 메시지 본문
     */
    public LlmMessage(
            LlmRole role,
            String content) {

        this(
                role,
                content,
                null,
                null,
                List.of(),
                List.of()
        );
    }

    /**
     * 기존 Tool Calling 호환성을 위한 생성자입니다.
     *
     * @param role       메시지 역할
     * @param content    메시지 본문
     * @param name       Tool 또는 메시지 이름
     * @param toolCallId Tool Call 식별자
     * @param toolCalls  Assistant Tool Call 목록
     */
    public LlmMessage(
            LlmRole role,
            String content,
            String name,
            String toolCallId,
            List<LlmToolCall> toolCalls) {

        this(
                role,
                content,
                name,
                toolCallId,
                toolCalls,
                List.of()
        );
    }

    /**
     * 전체 정보를 포함하는 LLM 메시지를 생성합니다.
     *
     * @param role        메시지 역할
     * @param content     메시지 본문
     * @param name        Tool 또는 메시지 이름
     * @param toolCallId  Tool Call 식별자
     * @param toolCalls   Assistant Tool Call 목록
     * @param attachments 첨부 파일 목록
     */
    public LlmMessage(
            LlmRole role,
            String content,
            String name,
            String toolCallId,
            List<LlmToolCall> toolCalls,
            List<LlmAttachment> attachments) {

        this.role =
                Objects.requireNonNull(
                        role,
                        "role must not be null"
                );

        this.content =
                content == null
                        ? ""
                        : content;

        this.name =
                normalizeOptional(
                        name
                );

        this.toolCallId =
                normalizeOptional(
                        toolCallId
                );

        this.toolCalls =
                immutableToolCalls(
                        toolCalls
                );

        this.attachments =
                immutableAttachments(
                        attachments
                );

        validate();
    }

    /**
     * 시스템 메시지를 생성합니다.
     */
    public static LlmMessage system(
            String content) {

        return new LlmMessage(
                LlmRole.SYSTEM,
                requireContent(
                        content,
                        "system content"
                )
        );
    }

    /**
     * 사용자 메시지를 생성합니다.
     */
    public static LlmMessage user(
            String content) {

        return new LlmMessage(
                LlmRole.USER,
                requireContent(
                        content,
                        "user content"
                )
        );
    }

    /**
     * Attachment를 포함하는 사용자 메시지를 생성합니다.
     *
     * @param content    사용자 메시지
     * @param attachment 첨부 파일
     * @return Attachment가 포함된 사용자 메시지
     */
    public static LlmMessage user(
            String content,
            LlmAttachment attachment) {

        return user(content)
                .withAttachment(
                        attachment
                );
    }

    /**
     * 여러 Attachment를 포함하는 사용자 메시지를 생성합니다.
     *
     * @param content     사용자 메시지
     * @param attachments 첨부 파일 목록
     * @return Attachment가 포함된 사용자 메시지
     */
    public static LlmMessage user(
            String content,
            List<LlmAttachment> attachments) {

        return user(content)
                .withAttachments(
                        attachments
                );
    }

    /**
     * Assistant 일반 응답 메시지를 생성합니다.
     */
    public static LlmMessage assistant(
            String content) {

        return new LlmMessage(
                LlmRole.ASSISTANT,
                requireContent(
                        content,
                        "assistant content"
                )
        );
    }

    /**
     * Assistant Tool Call 메시지를 생성합니다.
     *
     * <p>
     * Tool Calling 응답에서는 본문이 비어 있을 수 있습니다.
     * </p>
     *
     * @param toolCalls Tool Call 목록
     */
    public static LlmMessage assistantToolCalls(
            List<LlmToolCall> toolCalls) {

        return assistantToolCalls(
                "",
                toolCalls
        );
    }

    /**
     * 설명 본문과 Tool Call을 함께 포함하는 Assistant 메시지를 생성합니다.
     *
     * @param content   Assistant 본문
     * @param toolCalls Tool Call 목록
     */
    public static LlmMessage assistantToolCalls(
            String content,
            List<LlmToolCall> toolCalls) {

        return new LlmMessage(
                LlmRole.ASSISTANT,
                content,
                null,
                null,
                toolCalls,
                List.of()
        );
    }

    /**
     * Tool 실행 결과 메시지를 생성합니다.
     *
     * @param toolCallId 원본 Tool Call 식별자
     * @param toolName   실행한 Tool 이름
     * @param content    Tool 실행 결과
     */
    public static LlmMessage toolResult(
            String toolCallId,
            String toolName,
            String content) {

        return new LlmMessage(
                LlmRole.TOOL,
                requireContent(
                        content,
                        "tool result content"
                ),
                requireText(
                        toolName,
                        "toolName"
                ),
                normalizeOptional(
                        toolCallId
                ),
                List.of(),
                List.of()
        );
    }

    /**
     * Tool Call ID가 없는 Tool 실행 결과 메시지를 생성합니다.
     */
    public static LlmMessage toolResult(
            String toolName,
            String content) {

        return toolResult(
                null,
                toolName,
                content
        );
    }

    /**
     * 메시지 역할을 반환합니다.
     */
    public LlmRole getRole() {
        return role;
    }

    /**
     * 메시지 본문을 반환합니다.
     */
    public String getContent() {
        return content;
    }

    /**
     * Tool 또는 메시지 이름을 반환합니다.
     */
    public String getName() {
        return name;
    }

    /**
     * Tool 이름을 반환합니다.
     *
     * <p>
     * {@link #getName()}과 동일하며 Tool 메시지 처리 코드의
     * 가독성을 위해 제공합니다.
     * </p>
     */
    public String getToolName() {
        return name;
    }

    /**
     * Tool Call 식별자를 반환합니다.
     */
    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * Assistant Tool Call 목록을 반환합니다.
     */
    public List<LlmToolCall> getToolCalls() {
        return toolCalls;
    }

    /**
     * 첨부 파일 목록을 반환합니다.
     *
     * @return 수정할 수 없는 Attachment 목록
     */
    public List<LlmAttachment> getAttachments() {
        return attachments;
    }

    /**
     * Tool 또는 메시지 이름이 있는지 확인합니다.
     */
    public boolean hasName() {
        return name != null;
    }

    /**
     * Tool Call 식별자가 있는지 확인합니다.
     */
    public boolean hasToolCallId() {
        return toolCallId != null;
    }

    /**
     * Tool Call 목록이 있는지 확인합니다.
     */
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    /**
     * Attachment가 존재하는지 확인합니다.
     */
    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    /**
     * 이미지 Attachment가 존재하는지 확인합니다.
     */
    public boolean hasImageAttachments() {

        for (LlmAttachment attachment
                : attachments) {

            if (attachment.isImage()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 본문이 있는지 확인합니다.
     */
    public boolean hasContent() {
        return content != null
                && !content.isBlank();
    }

    /**
     * 시스템 메시지인지 확인합니다.
     */
    public boolean isSystem() {
        return role == LlmRole.SYSTEM;
    }

    /**
     * 사용자 메시지인지 확인합니다.
     */
    public boolean isUser() {
        return role == LlmRole.USER;
    }

    /**
     * Assistant 메시지인지 확인합니다.
     */
    public boolean isAssistant() {
        return role == LlmRole.ASSISTANT;
    }

    /**
     * Tool 결과 메시지인지 확인합니다.
     */
    public boolean isTool() {
        return role == LlmRole.TOOL;
    }

    /**
     * Assistant Tool Call 메시지인지 확인합니다.
     */
    public boolean isAssistantToolCall() {
        return isAssistant()
                && hasToolCalls();
    }

    /**
     * 현재 메시지에 Attachment 하나를 추가한 새로운 메시지를 반환합니다.
     *
     * <p>
     * {@code LlmMessage}는 불변 객체이므로 현재 인스턴스는 변경하지 않습니다.
     * </p>
     *
     * @param attachment 추가할 Attachment
     * @return 새로운 LlmMessage
     */
    public LlmMessage withAttachment(
            LlmAttachment attachment) {

        Objects.requireNonNull(
                attachment,
                "attachment must not be null"
        );

        List<LlmAttachment> updated =
                new ArrayList<>(
                        attachments.size() + 1
                );

        updated.addAll(
                attachments
        );

        updated.add(
                attachment
        );

        return new LlmMessage(
                role,
                content,
                name,
                toolCallId,
                toolCalls,
                updated
        );
    }

    /**
     * 현재 메시지에 여러 Attachment를 추가한 새로운 메시지를 반환합니다.
     *
     * @param attachments 추가할 Attachment 목록
     * @return 새로운 LlmMessage
     */
    public LlmMessage withAttachments(
            List<LlmAttachment> attachments) {

        Objects.requireNonNull(
                attachments,
                "attachments must not be null"
        );

        if (attachments.isEmpty()) {
            return this;
        }

        List<LlmAttachment> updated =
                new ArrayList<>(
                        this.attachments.size()
                                + attachments.size()
                );

        updated.addAll(
                this.attachments
        );

        for (LlmAttachment attachment
                : attachments) {

            updated.add(
                    Objects.requireNonNull(
                            attachment,
                            "attachments must not contain null"
                    )
            );
        }

        return new LlmMessage(
                role,
                content,
                name,
                toolCallId,
                toolCalls,
                updated
        );
    }

    /**
     * 현재 메시지의 Attachment를 모두 제거한 새로운 메시지를 반환합니다.
     *
     * @return Attachment가 없는 새로운 메시지
     */
    public LlmMessage withoutAttachments() {

        if (attachments.isEmpty()) {
            return this;
        }

        return new LlmMessage(
                role,
                content,
                name,
                toolCallId,
                toolCalls,
                List.of()
        );
    }

    /**
     * 메시지 상태를 검증합니다.
     */
    private void validate() {

        /*
         * Tool Call은 Assistant 메시지에만 허용한다.
         */
        if (hasToolCalls()
                && !isAssistant()) {

            throw new IllegalArgumentException(
                    "toolCalls are only allowed "
                            + "for assistant messages"
            );
        }

        /*
         * Tool 결과 메시지는 Tool 이름과 본문이 필수다.
         */
        if (isTool()) {

            if (name == null) {
                throw new IllegalArgumentException(
                        "tool message must contain tool name"
                );
            }

            if (!hasContent()) {
                throw new IllegalArgumentException(
                        "tool message content must not be blank"
                );
            }
        }

        /*
         * name / toolCallId는 Tool 메시지에만 허용한다.
         */
        if (!isTool()
                && (name != null
                || toolCallId != null)) {

            throw new IllegalArgumentException(
                    "name and toolCallId are only allowed "
                            + "for tool messages"
            );
        }

        /*
         * System 메시지는 반드시 본문이 있어야 한다.
         */
        if (isSystem()
                && !hasContent()) {

            throw new IllegalArgumentException(
                    "system message content must not be blank"
            );
        }

        /*
         * User 메시지는 본문 또는 Attachment 중 하나 이상이
         * 있어야 한다.
         *
         * 현재 user(String) 팩토리는 본문을 필수로 하지만,
         * 생성자를 통한 multimodal 확장을 위해 Attachment만 있는
         * 메시지도 허용한다.
         */
        if (isUser()
                && !hasContent()
                && !hasAttachments()) {

            throw new IllegalArgumentException(
                    "user message must contain "
                            + "content or attachments"
            );
        }

        /*
         * Assistant 메시지는 본문 또는 Tool Call 중 하나 이상이
         * 있어야 한다.
         */
        if (isAssistant()
                && !hasContent()
                && !hasToolCalls()) {

            throw new IllegalArgumentException(
                    "assistant message must contain "
                            + "content or tool calls"
            );
        }

        /*
         * Tool 결과 메시지에는 Attachment를 허용하지 않는다.
         */
        if (isTool()
                && hasAttachments()) {

            throw new IllegalArgumentException(
                    "attachments are not allowed "
                            + "for tool result messages"
            );
        }
    }

    /**
     * Tool Call 목록을 불변 리스트로 복사합니다.
     */
    private static List<LlmToolCall> immutableToolCalls(
            List<LlmToolCall> toolCalls) {

        if (toolCalls == null
                || toolCalls.isEmpty()) {

            return List.of();
        }

        List<LlmToolCall> copied =
                new ArrayList<>(
                        toolCalls.size()
                );

        for (LlmToolCall toolCall
                : toolCalls) {

            copied.add(
                    Objects.requireNonNull(
                            toolCall,
                            "toolCalls must not contain null"
                    )
            );
        }

        return Collections.unmodifiableList(
                copied
        );
    }

    /**
     * Attachment 목록을 불변 리스트로 복사합니다.
     */
    private static List<LlmAttachment> immutableAttachments(
            List<LlmAttachment> attachments) {

        if (attachments == null
                || attachments.isEmpty()) {

            return List.of();
        }

        List<LlmAttachment> copied =
                new ArrayList<>(
                        attachments.size()
                );

        for (LlmAttachment attachment
                : attachments) {

            copied.add(
                    Objects.requireNonNull(
                            attachment,
                            "attachments must not contain null"
                    )
            );
        }

        return Collections.unmodifiableList(
                copied
        );
    }

    /**
     * 내용이 반드시 존재해야 하는 값을 검증합니다.
     */
    private static String requireContent(
            String content,
            String fieldName) {

        if (content == null
                || content.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " must not be blank"
            );
        }

        return content;
    }

    /**
     * 필수 문자열을 검증하고 앞뒤 공백을 제거합니다.
     */
    private static String requireText(
            String value,
            String fieldName) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " must not be blank"
            );
        }

        return value.trim();
    }

    /**
     * 선택 문자열을 정규화합니다.
     */
    private static String normalizeOptional(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    @Override
    public String toString() {

        return "LlmMessage{"
                + "role=" + role
                + ", contentLength=" + content.length()
                + ", name='" + name + '\''
                + ", toolCallId='" + toolCallId + '\''
                + ", toolCallCount=" + toolCalls.size()
                + ", attachmentCount=" + attachments.size()
                + '}';
    }
}