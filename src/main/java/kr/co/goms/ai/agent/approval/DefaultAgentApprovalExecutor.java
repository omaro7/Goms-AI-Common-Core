/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.agent.approval;

import java.util.Objects;

/**
 * 승인된 Agent 작업을 실제 Handler로 전달하는 기본 Executor.
 */
public final class DefaultAgentApprovalExecutor
        implements AgentApprovalExecutor {

    private final AgentApprovalHandlerRegistry handlerRegistry;


    public DefaultAgentApprovalExecutor(
            AgentApprovalHandlerRegistry handlerRegistry) {

        this.handlerRegistry =
                Objects.requireNonNull(
                        handlerRegistry,
                        "handlerRegistry must not be null");
    }


    @Override
    public void execute(
            AgentApproval approval) {

        Objects.requireNonNull(
                approval,
                "approval must not be null");


        if (!approval.isApproved()) {

            throw new IllegalStateException(
                    "Approval is not approved: "
                            + approval.getApprovalId()
                            + ", status="
                            + approval.getStatus());
        }


        String action =
                requireText(
                        approval.getAction(),
                        "approval.action");


        String toolName =
                AgentApprovalAction.getToolName(
                        action);


        handlerRegistry.execute(
                toolName,
                approval);
    }


    private String requireText(
            String value,
            String name) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    name
                            + " must not be blank.");
        }


        return value.trim();
    }
}