/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag;

/**
 * RagService 처리 단계입니다.
 */
public enum RagOperation {

    /**
     * 요청값 검증.
     */
    VALIDATE,

    /**
     * 관련 문서 검색.
     */
    RETRIEVE,

    /**
     * RAG 컨텍스트 생성.
     */
    BUILD_CONTEXT,

    /**
     * 사용자 프롬프트 증강.
     */
    AUGMENT_PROMPT,

    /**
     * 최종 응답 모델 생성.
     */
    BUILD_RESPONSE,

    /**
     * 가용성 확인.
     */
    AVAILABILITY_CHECK,

    /**
     * 정의되지 않은 작업.
     */
    UNKNOWN
}