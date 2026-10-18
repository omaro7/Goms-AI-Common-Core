/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.regression;

import java.util.ArrayList;
import java.util.List;

import kr.co.goms.ai.rag.eval.RagMetricResult;
import kr.co.goms.ai.rag.eval.report.RagEvaluationReport;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationBaseline.MetricBaseline;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationRegressionResult.MetricResult;

/**
 * RAG Evaluation 결과를 Baseline과 비교하여
 * Regression 여부를 판정한다.
 * 
 * Baseline
 * faithfulness
 * score     = 0.93
 * tolerance = 0.03
 * >> minimum = 0.90
 * 
 * Case 1 = 0.94
 * Case 2 = 0.92
 * Case 3 = 0.88
 * >> Average = 0.9133
 * 
 * 결과
 * 0.9133 >= 0.90
 * PASS
 */
public final class RagEvaluationRegressionChecker {

    public RagEvaluationRegressionResult check(RagEvaluationBaseline baseline, RagEvaluationReport report) {
        if (baseline == null) {
            throw new NullPointerException("baseline must not be null");
        }

        if (report == null) {
            throw new NullPointerException("report must not be null");
        }

        validateDataset(baseline, report);

        List<MetricResult> results = new ArrayList<>();

        for (MetricBaseline metricBaseline : baseline.getMetrics()) {
            results.add(compareMetric(metricBaseline, report));
        }

        return new RagEvaluationRegressionResult(
                baseline.getName(),
                report.getDatasetName(),
                results);
    }

    private MetricResult compareMetric(MetricBaseline baseline, RagEvaluationReport report) {
        double currentScore = report.getAverageMetricScore(baseline.getMetricName());

        return new MetricResult(
                baseline.getMetricName(),
                baseline.getScore(),
                currentScore,
                baseline.getMinimumAcceptedScore());
    }

    private void validateDataset(RagEvaluationBaseline baseline, RagEvaluationReport report) {
        if (!baseline.getDatasetName().equals(report.getDatasetName())) {
            throw new IllegalArgumentException(
                    "Dataset mismatch. baseline="
                            + baseline.getDatasetName()
                            + ", report="
                            + report.getDatasetName());
        }
    }
}