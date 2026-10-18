/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.pipeline;

/**
 * RagPipeline 처리 단계입니다.
 */
public enum RagPipelineOperation {

    /**
     * 사용자 요청 및 검색 요청 검증.
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
     * 증강 프롬프트 생성.
     */
    PREPARE_PROMPT,

    /**
     * LLM 요청 모델 생성.
     */
    CREATE_LLM_REQUEST,

    /**
     * LLM 호출.
     */
    CALL_LLM,

    /**
     * LLM 응답 검증.
     */
    VALIDATE_LLM_RESPONSE,

    /**
     * 최종 응답 생성.
     */
    BUILD_RESPONSE,

    /**
     * 구성요소 가용성 확인.
     */
    AVAILABILITY_CHECK,

    /**
     * 정의되지 않은 작업.
     */
    UNKNOWN
}