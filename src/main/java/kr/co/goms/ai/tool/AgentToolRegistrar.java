/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.tool;

/**
 * Registers Agent Tools into a {@link ToolRegistry}.
 */
public interface AgentToolRegistrar {

    /**
     * Registers Agent Tools.
     *
     * @param registry
     *        target Tool registry
     */
    void registerTools(ToolRegistry registry);
}