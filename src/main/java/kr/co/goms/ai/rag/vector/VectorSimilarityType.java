/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.vector;

/**
 * 벡터 검색 점수 계산 방식입니다.
 */
public enum VectorSimilarityType {

    /**
     * 코사인 유사도입니다.
     *
     * 텍스트 임베딩 검색에서 기본 방식으로 사용합니다.
     * 값이 클수록 유사합니다.
     */
    COSINE,

    /**
     * 내적 점수입니다.
     *
     * L2 정규화된 벡터에서는 코사인 유사도와 같은 값이 됩니다.
     * 값이 클수록 유사합니다.
     */
    DOT_PRODUCT,

    /**
     * 유클리드 거리입니다.
     *
     * 원래는 값이 작을수록 유사하므로 VectorStore에서 검색 점수로
     * 변환할 경우 별도 변환 규칙이 필요합니다.
     */
    EUCLIDEAN
}