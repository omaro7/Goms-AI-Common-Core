/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent.event;

import java.time.Instant;

import kr.co.goms.ai.agent.approval.AgentApproval;

public class AgentEvent {

    private final String runId;
    private final AgentEventType type;
    private final String message;
    private final Object data;
    private final String approvalId;
    private final String title;
    private final String fileName;
    private final String content;
    private final String approveLabel;
    private final String rejectLabel;
    private final Instant createdAt;

    public AgentEvent(String runId, AgentEventType type, String message) {
        this(runId, type, message, null, null, null, null, null, null, null);
    }

    public AgentEvent(String runId, AgentEventType type, String message, Object data, String approvalId, String title, String fileName, String content, String approveLabel, String rejectLabel) {
        this.runId = runId;
        this.type = type;
        this.message = message;
        this.data = data;
        this.approvalId = approvalId;
        this.title = title;
        this.fileName = fileName;
        this.content = content;
        this.approveLabel = approveLabel;
        this.rejectLabel = rejectLabel;
        this.createdAt = Instant.now();
    }

    public String getRunId() { return runId; }

    public AgentEventType getType() { return type; }

    public String getMessage() { return message; }
    
    public Object getData() { return data; }

    public String getApprovalId() { return approvalId; }

    public String getTitle() { return title; }

    public String getFileName() { return fileName; }

    public String getContent() { return content; }

    public String getApproveLabel() { return approveLabel; }

    public String getRejectLabel() { return rejectLabel; }

    public Instant getCreatedAt() { return createdAt; }

    public boolean isApprovalRequired() { return type == AgentEventType.APPROVAL_REQUIRED; }

    public static AgentEvent message(String runId, String message) {
        return new AgentEvent(runId, AgentEventType.MESSAGE, message);
    }

    public static AgentEvent ragProgress(
            String runId,
            String message,
            Object data) {

        return new AgentEvent(
                runId,
                AgentEventType.RAG_PROGRESS,
                message,
                data,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
    
    public static AgentEvent approvalRequired(AgentApproval approval) {
        return new AgentEvent(
                approval.getRunId(),
                AgentEventType.APPROVAL_REQUIRED,
                approval.getMessage(),
                null,
                approval.getApprovalId(),
                approval.getTitle(),
                approval.getFileName(),
                approval.getContent(),
                "승인",
                "취소");
    }
}