/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.embedding;

/**
 * RAG 문서 및 검색 질의에 사용할 임베딩 모델명을 제공합니다.
 */
public interface EmbeddingModelProvider {

    /**
     * 현재 사용할 임베딩 모델명을 반환합니다.
     *
     * @return 임베딩 모델명
     */
    String getModel();
}