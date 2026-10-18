/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent.event;

/**
 * GomsBook-AI-Agent-Core 내부
* Agent Engine 내부에서 발생하는 실행 이벤트 유형입니다.
* API/SSE 외부 계약과 독립적으로 관리합니다.
*/
public enum AgentEventType {

    STARTED,

    THINKING,

    TOOL_CALLING,

    TOOL_RESULT,

    MESSAGE,

    RAG_STARTED,
    
    RAG_PROGRESS,
    
    RAG_CONTEXT,
    
    RAG_COMPLETED,
    
    APPROVAL_REQUIRED,

    COMPLETED,

    ERROR
}