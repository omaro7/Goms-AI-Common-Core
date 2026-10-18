/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.agent.approval;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 기본 Agent Approval Handler Registry.
 */
public final class DefaultAgentApprovalHandlerRegistry
        implements AgentApprovalHandlerRegistry {

    private final Map<String, AgentApprovalHandler> handlers =
            new ConcurrentHashMap<>();


    @Override
    public void register(
            String toolName,
            AgentApprovalHandler handler) {

        String normalizedToolName =
                requireToolName(
                        toolName);


        Objects.requireNonNull(
                handler,
                "handler must not be null");


        AgentApprovalHandler previous =
                handlers.putIfAbsent(
                        normalizedToolName,
                        handler);


        if (previous != null) {

            throw new IllegalStateException(
                    "Approval handler already registered: "
                            + normalizedToolName);
        }
    }


    @Override
    public boolean contains(
            String toolName) {

        if (toolName == null
                || toolName.isBlank()) {

            return false;
        }


        return handlers.containsKey(
                toolName.trim());
    }


    @Override
    public AgentApprovalHandler get(
            String toolName) {

        String normalizedToolName =
                requireToolName(
                        toolName);


        AgentApprovalHandler handler =
                handlers.get(
                        normalizedToolName);


        if (handler == null) {

            throw new IllegalArgumentException(
                    "Unsupported approval tool: "
                            + normalizedToolName);
        }


        return handler;
    }


    @Override
    public void execute(
            String toolName,
            AgentApproval approval) {

        Objects.requireNonNull(
                approval,
                "approval must not be null");


        AgentApprovalHandler handler =
                get(
                        toolName);


        handler.execute(
                approval);
    }


    private String requireToolName(
            String toolName) {

        if (toolName == null
                || toolName.isBlank()) {

            throw new IllegalArgumentException(
                    "toolName must not be blank.");
        }


        return toolName.trim();
    }
}