/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent;

import kr.co.goms.ai.tool.ToolResult;

public interface AgentToolResultListener {

    void onToolResult(ToolResult result);
}