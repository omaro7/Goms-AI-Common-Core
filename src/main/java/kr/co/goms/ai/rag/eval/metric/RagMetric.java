/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.metric;

import kr.co.goms.ai.rag.eval.RagEvaluationContext;
import kr.co.goms.ai.rag.eval.RagMetricResult;

/**
 * RAG 품질 평가 Metric의 공통 인터페이스.
 *
 * 각 Metric은 동일한 Evaluation Context를 입력받아
 * 0.0 ~ 1.0 범위의 평가 결과를 반환한다.
 */
public interface RagMetric {

    /**
     * Metric의 고유 이름을 반환한다.
     *
     * @return metric name
     */
    String getName();

    /**
     * 주어진 RAG Context를 평가한다.
     *
     * @param context 평가 대상 context
     * @return metric evaluation result
     */
    RagMetricResult evaluate(RagEvaluationContext context);
}