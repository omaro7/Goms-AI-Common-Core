/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval;

import java.util.Collections;
import java.util.List;

/**
 * 하나의 RAG Evaluation에 대한 전체 결과.
 * 
 * FaithfulnessResult
 * AnswerRelevancyResult
 * ContextPrecisionResult
 * ContextRecallResult
 *         ↓
 * RagEvaluationResult
 *        ↓
 * overallScore (최종 스코어 가지고 옴)
 * 
 * RagMetricResult faithfulness =
 *         new RagMetricResult(
 *                 "faithfulness",
 *                 0.95,
 *                 "Most claims are supported.");
 * RagMetricResult relevancy =
 *         new RagMetricResult(
 *                 "answer_relevancy",
 *                 0.90,
 *                 "Answer directly addresses the question.");
 * 
 * RagEvaluationResult result =
 *         new RagEvaluationResult(
 *                 List.of(
 *                         faithfulness,
 *                         relevancy
 *                 )
 *         );
 * 
 * System.out.println(result.getOverallScore());	// 0.925
 */
public final class RagEvaluationResult {

    private final List<RagMetricResult> metricResults;
    private final double overallScore;

    public RagEvaluationResult(List<RagMetricResult> metricResults) {
        this.metricResults = normalize(metricResults);
        this.overallScore = calculateOverallScore(this.metricResults);
    }

    public List<RagMetricResult> getMetricResults() {
        return metricResults;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public RagMetricResult getMetricResult(String metricName) {
        if (metricName == null || metricName.trim().isEmpty()) {
            return null;
        }

        for (RagMetricResult result : metricResults) {
            if (metricName.equals(result.getMetricName())) {
                return result;
            }
        }

        return null;
    }

    private static List<RagMetricResult> normalize(
            List<RagMetricResult> metricResults) {

        if (metricResults == null || metricResults.isEmpty()) {
            return Collections.emptyList();
        }

        return List.copyOf(metricResults);
    }

    private static double calculateOverallScore(List<RagMetricResult> metricResults) {
        if (metricResults.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        int count = 0;

        for (RagMetricResult result : metricResults) {
            if (!result.isApplicable()) {
                continue;
            }

            total += result.getScore();
            count++;
        }

        if (count == 0) {
            return 0.0;
        }

        return total / count;
    }
    
    @Override
    public String toString() {
        return "RagEvaluationResult{" +
                "metricResults=" + metricResults +
                ", overallScore=" + overallScore +
                '}';
    }
}