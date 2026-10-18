/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.vector;

/**
 * VectorStore에서 수행되는 작업 유형입니다.
 */
public enum VectorStoreOperation {

    SAVE,

    SAVE_ALL,

    SEARCH,

    FIND,

    DELETE,

    CLEAR,

    COUNT,

    INITIALIZE,

    CLOSE,

    UNKNOWN
}