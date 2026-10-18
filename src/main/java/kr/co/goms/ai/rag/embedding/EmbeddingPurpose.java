/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.embedding;

/**
 * 임베딩 입력의 사용 목적입니다.
 *
 * 일부 임베딩 모델은 검색 질의와 문서를 서로 다른 형식이나
 * 접두어로 처리하므로 목적을 명시적으로 구분합니다.
 */
public enum EmbeddingPurpose {

    /**
     * VectorStore에 저장할 문서 또는 Chunk 임베딩.
     */
    DOCUMENT,

    /**
     * 유사 문서를 검색하기 위한 사용자 질의 임베딩.
     */
    QUERY
}