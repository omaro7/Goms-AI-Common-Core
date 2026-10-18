/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.agent.event;

import kr.co.goms.ai.agent.event.payload.RagContextPayload;

public interface AgentRagEventListener {

    void onStarted(String runId);

    void onContext(String runId, RagContextPayload payload);

    void onCompleted(String runId);
}