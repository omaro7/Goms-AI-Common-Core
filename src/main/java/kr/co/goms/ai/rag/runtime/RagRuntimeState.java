/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.runtime;

/**
 * RagRuntime의 현재 생명주기 상태입니다.
 */
public enum RagRuntimeState {

    CREATED,

    STARTING,

    RUNNING,

    STOPPING,

    CLOSED,

    FAILED
}