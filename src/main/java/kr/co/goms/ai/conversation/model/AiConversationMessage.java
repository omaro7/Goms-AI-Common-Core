/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.conversation.model;

import java.time.LocalDateTime;

public class AiConversationMessage {

    private final String messageId;

    private final String conversationId;

    private final String runId;

    private final AiConversationMessageRole role;

    private final String content;

    private final long sequenceNo;

    private final LocalDateTime createdAt;

    public AiConversationMessage(
            String messageId,
            String conversationId,
            String runId,
            AiConversationMessageRole role,
            String content,
            long sequenceNo,
            LocalDateTime createdAt) {

        this.messageId = messageId;
        this.conversationId = conversationId;
        this.runId = runId;
        this.role = role;
        this.content = content;
        this.sequenceNo = sequenceNo;
        this.createdAt = createdAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getRunId() {
        return runId;
    }

    public AiConversationMessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}