/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.tool;

import java.nio.file.Path;
import java.util.Objects;

import kr.co.goms.ai.agent.approval.AgentApprovalService;
import kr.co.goms.ai.rag.RagService;

import com.google.gson.Gson;

/**
 * Core 공통 Agent Tool을 등록하는 기본 구현체입니다.
 */
public final class DefaultAgentToolRegistrar implements AgentToolRegistrar {
	
    private final AgentApprovalService approvalService;
    
    
    private final Gson gson;
    
    private final RagService ragService;
    
    public DefaultAgentToolRegistrar(
            Gson gson,
            AgentApprovalService approvalService,
            RagService ragService
            ) {
        this.approvalService = Objects.requireNonNull(approvalService, "approvalService must not be null");
        this.gson = Objects.requireNonNull(gson, "gson must not be null");
        this.ragService = Objects.requireNonNull(ragService, "ragService must not be null");
     }


    @Override
    public void registerTools(ToolRegistry registry) {

        Objects.requireNonNull(registry, "registry must not be null");

        registerCoreTools(registry);
    }

    private void registerCoreTools(ToolRegistry registry) {

        registerIfAbsent(registry, new EchoTool());
    }

    private void registerIfAbsent(ToolRegistry registry, AgentTool tool) {

        Objects.requireNonNull(tool, "tool must not be null");

        if (registry.contains(tool.getName())) return;

        registry.register(tool);
    }
}