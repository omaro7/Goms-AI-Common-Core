/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.prompt;

/**
 * PromptAugmentor 처리 단계입니다.
 */
public enum PromptAugmentationOperation {

    /**
     * 사용자 프롬프트 또는 RAG 컨텍스트 검증.
     */
    VALIDATE,

    /**
     * RAG 컨텍스트 문자열 생성.
     */
    FORMAT_CONTEXT,

    /**
     * 사용자 요청과 컨텍스트 결합.
     */
    AUGMENT,

    /**
     * 최종 프롬프트 길이 검사.
     */
    LENGTH_CHECK,

    /**
     * 정의되지 않은 작업.
     */
    UNKNOWN
}