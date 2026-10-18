/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.regression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RAG Evaluation의 기준 점수를 보관한다.
 * 
 * List<RagEvaluationBaseline.MetricBaseline> metrics = List.of(
 *         new RagEvaluationBaseline.MetricBaseline(
 *                 "faithfulness",
 *                 0.93,
 *                 0.03),
 * 
 *         new RagEvaluationBaseline.MetricBaseline(
 *                 "answer_relevancy",
 *                 0.90,
 *                 0.05),
 * 
 *         new RagEvaluationBaseline.MetricBaseline(
 *                 "context_precision",
 *                 0.84,
 *                 0.05),
 * 
 *         new RagEvaluationBaseline.MetricBaseline(
 *                 "context_recall",
 *                 0.92,
 *                 0.03));
 * 
 * RagEvaluationBaseline baseline =
 *         new RagEvaluationBaseline(
 *                 "RAG-BASELINE-001",
 *                 "lunchwork-seoul-v1",
 *                 metrics);
 *                 
 * Baseline Faithfulness = 0.93
 * Tolerance             = 0.03
 * Minimum Accepted      = 0.90
 * 이라면 0.91은 PASS, 0.88은 Regression으로 볼 수 있습니다.                
 */
public final class RagEvaluationBaseline {

    private final String name;
    private final String datasetName;
    private final List<MetricBaseline> metrics;

    public RagEvaluationBaseline(String name, String datasetName, List<MetricBaseline> metrics) {
        this.name = requireText(name, "name");
        this.datasetName = requireText(datasetName, "datasetName");
        this.metrics = normalizeMetrics(metrics);
    }

    public String getName() {
        return name;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public List<MetricBaseline> getMetrics() {
        return metrics;
    }

    public MetricBaseline getMetric(String metricName) {
        if (metricName == null || metricName.trim().isEmpty()) {
            return null;
        }

        for (MetricBaseline metric : metrics) {
            if (metricName.equals(metric.getMetricName())) {
                return metric;
            }
        }

        return null;
    }

    private static List<MetricBaseline> normalizeMetrics(List<MetricBaseline> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return Collections.emptyList();
        }

        List<MetricBaseline> normalized = new ArrayList<>();

        for (MetricBaseline metric : metrics) {
            if (metric != null) {
                normalized.add(metric);
            }
        }

        return Collections.unmodifiableList(normalized);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null) {
            throw new NullPointerException(fieldName + " must not be null");
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "RagEvaluationBaseline{" +
                "name='" + name + '\'' +
                ", datasetName='" + datasetName + '\'' +
                ", metrics=" + metrics.size() +
                '}';
    }

    /**
     * 개별 Metric의 기준 점수.
     */
    public static final class MetricBaseline {

        private final String metricName;
        private final double score;
        private final double tolerance;

        public MetricBaseline(String metricName, double score, double tolerance) {
            this.metricName = requireText(metricName, "metricName");
            this.score = validateScore(score, "score");
            this.tolerance = validateScore(tolerance, "tolerance");
        }

        public String getMetricName() {
            return metricName;
        }

        public double getScore() {
            return score;
        }

        public double getTolerance() {
            return tolerance;
        }

        public double getMinimumAcceptedScore() {
            return Math.max(0.0, score - tolerance);
        }

        private static double validateScore(double value, String fieldName) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException(fieldName + " must be a finite number");
            }

            if (value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException(
                        fieldName + " must be between 0.0 and 1.0: " + value);
            }

            return value;
        }

        @Override
        public String toString() {
            return "MetricBaseline{" +
                    "metricName='" + metricName + '\'' +
                    ", score=" + score +
                    ", tolerance=" + tolerance +
                    '}';
        }
    }
}