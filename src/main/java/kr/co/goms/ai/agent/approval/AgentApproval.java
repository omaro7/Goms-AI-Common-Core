/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent.approval;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AgentApproval {

    private final String approvalId;
    private final String runId;
    private final String projectId;
    private final String action;
    private final String title;
    private final String message;
    private final String fileName;
    private final String content;
    private final Instant createdAt;
    private AgentApprovalStatus status;

    public AgentApproval(String runId, String projectId, String action, String title, String message, String fileName, String content) {
        this.approvalId = UUID.randomUUID().toString();
        this.runId = requireText(runId, "runId");
        this.projectId = requireText(projectId, "projectId");
        this.action = requireText(action, "action");
        this.title = requireText(title, "title");
        this.message = message;
        this.fileName = requireText(fileName, "fileName");
        this.content = content;
        this.createdAt = Instant.now();
        this.status = AgentApprovalStatus.PENDING;
    }

    public String getApprovalId() { return approvalId; }

    public String getRunId() { return runId; }

    public String getProjectId() { return projectId; }

    public String getAction() { return action; }

    public String getTitle() { return title; }

    public String getMessage() { return message; }

    public String getFileName() { return fileName; }

    public String getContent() { return content; }

    public Instant getCreatedAt() { return createdAt; }

    public AgentApprovalStatus getStatus() { return status; }

    public boolean isPending() { return status == AgentApprovalStatus.PENDING; }

    public boolean isApproved() { return status == AgentApprovalStatus.APPROVED; }

    public boolean isRejected() { return status == AgentApprovalStatus.REJECTED; }

    public boolean isExpired() { return status == AgentApprovalStatus.EXPIRED; }

    public void approve() {
        ensurePending();
        status = AgentApprovalStatus.APPROVED;
    }

    public void reject() {
        ensurePending();
        status = AgentApprovalStatus.REJECTED;
    }

    public void expire() {
        ensurePending();
        status = AgentApprovalStatus.EXPIRED;
    }

    private void ensurePending() {
        if (!isPending()) throw new IllegalStateException("Approval is not pending: " + approvalId + ", status=" + status);
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}