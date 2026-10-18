/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval;

/**
 * RAG 전체 평가를 수행하는 Evaluator 인터페이스.
 * 
 * RagEvaluationContext
 *         ↓
 * RagEvaluator
 *         ↓
 * 여러 RagMetric 실행
 *         ↓
 * RagEvaluationResult
 * 
 *  
 * RagEvaluator
 *     ↑
 * DefaultRagEvaluator
 *     ├─ FaithfulnessMetric
 *     ├─ AnswerRelevancyMetric
 *     ├─ ContextPrecisionMetric
 *     └─ ContextRecallMetric
 */
public interface RagEvaluator {

    /**
     * 주어진 RAG Evaluation Context를 평가한다.
     *
     * @param context 평가 대상 context
     * @return 전체 RAG 평가 결과
     */
    RagEvaluationResult evaluate(RagEvaluationContext context);
}