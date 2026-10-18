/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent.approval;

public interface AgentApprovalService {

    AgentApproval create(String runId, String projectId, String action, String title, String message, String fileName, String content);

    AgentApproval get(String approvalId);

    AgentApproval approve(String approvalId);

    AgentApproval reject(String approvalId);

    AgentApproval expire(String approvalId);
}