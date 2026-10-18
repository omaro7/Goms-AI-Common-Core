/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kr.co.goms.ai.rag.eval.metric.RagMetric;

/**
 * 기본 RAG Evaluator 구현체.
 *
 * 등록된 Metric을 순서대로 실행하고 전체 평가 결과를 생성한다.
 * 
 * RagEvaluationContext
 *        │
 *        ▼
 * DefaultRagEvaluator
 *         │
 *         ├── FaithfulnessMetric ──────┐
 *         ├── AnswerRelevancyMetric ───┤
 *         ├── ContextPrecisionMetric ──┤
 *         └── ContextRecallMetric ─────┤
 *                                      ▼
 *                                RagJudge
 *                                      │
 *                                      ▼
 *                                LlmRagJudge
 *                                      │
 *                                      ▼
 *                                 LlmClient
 *         │
 *         ▼
 * List<RagMetricResult>
 *         │
 *         ▼
 * RagEvaluationResult
 *         │
 *         ├── metricResults
 *         └── overallScore
 */
public final class DefaultRagEvaluator implements RagEvaluator {

    private final List<RagMetric> metrics;

    public DefaultRagEvaluator(List<RagMetric> metrics) {
        this.metrics = normalizeMetrics(metrics);
    }

    @Override
    public RagEvaluationResult evaluate(RagEvaluationContext context) {
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }

        List<RagMetricResult> results = new ArrayList<>();

        for (RagMetric metric : metrics) {
            RagMetricResult result = metric.evaluate(context);

            if (result != null) {
                results.add(result);
            }
        }

        return new RagEvaluationResult(results);
    }

    public List<RagMetric> getMetrics() {
        return metrics;
    }

    private static List<RagMetric> normalizeMetrics(List<RagMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return Collections.emptyList();
        }

        List<RagMetric> normalized = new ArrayList<>();

        for (RagMetric metric : metrics) {
            if (metric != null) {
                normalized.add(metric);
            }
        }

        return Collections.unmodifiableList(normalized);
    }
}