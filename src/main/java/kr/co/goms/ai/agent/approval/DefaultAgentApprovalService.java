/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent.approval;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultAgentApprovalService implements AgentApprovalService {

    private final Map<String, AgentApproval> approvals = new ConcurrentHashMap<>();

    @Override
    public AgentApproval create(String runId, String projectId, String action, String title, String message, String fileName, String content) {
        AgentApproval approval = new AgentApproval(runId, projectId, action, title, message, fileName, content);
        approvals.put(approval.getApprovalId(), approval);
        return approval;
    }

    @Override
    public AgentApproval get(String approvalId) {
        return findRequired(approvalId);
    }

    @Override
    public AgentApproval approve(String approvalId) {
        AgentApproval approval = findRequired(approvalId);
        approval.approve();
        return approval;
    }

    @Override
    public AgentApproval reject(String approvalId) {
        AgentApproval approval = findRequired(approvalId);
        approval.reject();
        return approval;
    }

    @Override
    public AgentApproval expire(String approvalId) {
        AgentApproval approval = findRequired(approvalId);
        approval.expire();
        return approval;
    }

    private AgentApproval findRequired(String approvalId) {
        Objects.requireNonNull(approvalId, "approvalId must not be null");
        if (approvalId.isBlank()) throw new IllegalArgumentException("approvalId must not be blank");
        AgentApproval approval = approvals.get(approvalId);
        if (approval == null) throw new IllegalArgumentException("Approval was not found: " + approvalId);
        return approval;
    }
}