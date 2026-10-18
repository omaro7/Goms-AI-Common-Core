/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.judge;

/**
 * RAG 평가용 Judge 인터페이스.
 *
 * Metric이 특정 LLM 구현에 직접 의존하지 않도록
 * 평가 요청과 응답 사이의 추상 계층을 제공한다.
 */
public interface RagJudge {

    /**
     * 평가 프롬프트를 전달하고 Judge 결과를 반환한다.
     *
     * @param systemPrompt 평가 기준을 정의하는 system prompt
     * @param evaluationPrompt 실제 평가 대상 prompt
     * @return judge result
     */
    RagJudgeResult judge(String systemPrompt, String evaluationPrompt);
}