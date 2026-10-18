/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.retrieval;

/**
 * Retriever 처리 단계입니다.
 */
public enum RetrievalOperation {

    VALIDATE,

    EMBED_QUERY,

    VECTOR_SEARCH,

    RESULT_MAPPING,

    UNKNOWN
}