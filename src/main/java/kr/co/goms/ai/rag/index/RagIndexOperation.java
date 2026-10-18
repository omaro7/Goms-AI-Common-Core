/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

/**
 * RAG 인덱싱 처리 단계입니다.
 */
public enum RagIndexOperation {

    VALIDATE,

    CHUNK_DOCUMENT,

    CHECK_EXISTING,

    CREATE_HASH,

    EMBED,

    CREATE_RECORD,

    SAVE,

    DELETE,

    CLEAR,

    UNKNOWN
}