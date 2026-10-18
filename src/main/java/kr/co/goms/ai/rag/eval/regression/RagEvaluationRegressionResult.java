/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.regression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RAG Evaluation Regression 비교 결과.
 * 
 * faithfulness 		답변 충실성
 * baseline = 0.93
 * current  = 0.91
 * minimum  = 0.90
 * difference = -0.02
 * PASS
 * 
 * context_precision	문맥 정확도
 * baseline = 0.84
 * current  = 0.77
 * minimum  = 0.79
 * difference = -0.07
 * FAIL
 * 
 * 하나라도 FAIL이면 전체 결과는: result.isPassed() == false
 */
public final class RagEvaluationRegressionResult {

    private final String baselineName;
    private final String datasetName;
    private final List<MetricResult> metricResults;
    private final boolean passed;

    public RagEvaluationRegressionResult(
            String baselineName,
            String datasetName,
            List<MetricResult> metricResults) {

        this.baselineName = requireText(baselineName, "baselineName");
        this.datasetName = requireText(datasetName, "datasetName");
        this.metricResults = normalizeMetricResults(metricResults);
        this.passed = calculatePassed(this.metricResults);
    }

    public String getBaselineName() {
        return baselineName;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public List<MetricResult> getMetricResults() {
        return metricResults;
    }

    public boolean isPassed() {
        return passed;
    }

    public MetricResult getMetricResult(String metricName) {
        if (metricName == null || metricName.trim().isEmpty()) {
            return null;
        }

        for (MetricResult result : metricResults) {
            if (metricName.equals(result.getMetricName())) {
                return result;
            }
        }

        return null;
    }

    private static List<MetricResult> normalizeMetricResults(List<MetricResult> metricResults) {
        if (metricResults == null || metricResults.isEmpty()) {
            return Collections.emptyList();
        }

        List<MetricResult> normalized = new ArrayList<>();

        for (MetricResult result : metricResults) {
            if (result != null) {
                normalized.add(result);
            }
        }

        return Collections.unmodifiableList(normalized);
    }

    private static boolean calculatePassed(List<MetricResult> metricResults) {
        if (metricResults.isEmpty()) {
            return false;
        }

        for (MetricResult result : metricResults) {
            if (!result.isPassed()) {
                return false;
            }
        }

        return true;
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
        return "RagEvaluationRegressionResult{" +
                "baselineName='" + baselineName + '\'' +
                ", datasetName='" + datasetName + '\'' +
                ", passed=" + passed +
                '}';
    }

    /**
     * 개별 Metric의 Regression 비교 결과.
     */
    public static final class MetricResult {

        private final String metricName;
        private final double baselineScore;
        private final double currentScore;
        private final double minimumAcceptedScore;
        private final double difference;
        private final boolean passed;

        public MetricResult(
                String metricName,
                double baselineScore,
                double currentScore,
                double minimumAcceptedScore) {

            this.metricName = requireText(metricName, "metricName");
            this.baselineScore = validateScore(baselineScore, "baselineScore");
            this.currentScore = validateScore(currentScore, "currentScore");
            this.minimumAcceptedScore = validateScore(
                    minimumAcceptedScore,
                    "minimumAcceptedScore");

            this.difference = currentScore - baselineScore;
            this.passed = currentScore >= minimumAcceptedScore;
        }

        public String getMetricName() {
            return metricName;
        }

        public double getBaselineScore() {
            return baselineScore;
        }

        public double getCurrentScore() {
            return currentScore;
        }

        public double getMinimumAcceptedScore() {
            return minimumAcceptedScore;
        }

        public double getDifference() {
            return difference;
        }

        public boolean isPassed() {
            return passed;
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
            return "MetricResult{" +
                    "metricName='" + metricName + '\'' +
                    ", baselineScore=" + baselineScore +
                    ", currentScore=" + currentScore +
                    ", difference=" + difference +
                    ", passed=" + passed +
                    '}';
        }
    }
}