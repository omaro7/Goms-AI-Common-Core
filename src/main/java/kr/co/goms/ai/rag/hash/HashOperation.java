/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.hash;

/**
 * HashService 처리 단계입니다.
 */
public enum HashOperation {

    INITIALIZE,

    HASH_STRING,

    HASH_BYTES,

    ENCODE,

    COMPARE,

    UNKNOWN
}