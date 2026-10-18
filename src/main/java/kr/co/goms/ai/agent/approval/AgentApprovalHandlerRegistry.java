/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.agent.approval;

/**
 * Tool Name 기준으로 승인 Handler를 관리하는 Registry.
 */
public interface AgentApprovalHandlerRegistry {

    void register(
            String toolName,
            AgentApprovalHandler handler);


    boolean contains(
            String toolName);


    AgentApprovalHandler get(
            String toolName);


    void execute(
            String toolName,
            AgentApproval approval);
}