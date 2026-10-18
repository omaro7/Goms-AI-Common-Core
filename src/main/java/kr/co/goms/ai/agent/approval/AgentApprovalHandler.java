/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.agent.approval;

/**
 * 승인 완료 후 실제 작업을 실행하는 Handler.
 */
@FunctionalInterface
public interface AgentApprovalHandler {

    void execute(AgentApproval approval);
}