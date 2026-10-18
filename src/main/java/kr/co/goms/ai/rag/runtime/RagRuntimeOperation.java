/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.runtime;

/**
 * RagRuntime에서 수행되는 작업 유형입니다.
 */
public enum RagRuntimeOperation {

    START,

    OPEN_PROJECT,

    CLOSE_PROJECT,

    LOAD_DOCUMENT,

    INDEX,

    EXECUTE,

    REMOVE_INDEX,

    CLEAR_INDEX,

    CLOSE,

    UNKNOWN
}