/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.conversation.model;

import java.time.LocalDateTime;

public class AiConversation {

    private final String conversationId;

    private final String projectId;

    private String title;

    private String status;

    private final LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public AiConversation(
            String conversationId,
            String projectId,
            String title,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        this.conversationId = conversationId;
        this.projectId = projectId;
        this.title = title;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getProjectId() {
        return projectId;
    }
    
    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}