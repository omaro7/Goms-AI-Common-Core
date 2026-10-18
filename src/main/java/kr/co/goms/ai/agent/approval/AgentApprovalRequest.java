/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent.approval;

public class AgentApprovalRequest {

    private String approvalId;
    private boolean approved;

    public String getApprovalId() { return approvalId; }

    public void setApprovalId(String approvalId) { this.approvalId = approvalId; }

    public boolean isApproved() { return approved; }

    public void setApproved(boolean approved) { this.approved = approved; }
}