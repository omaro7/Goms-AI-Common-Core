/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval;

/**
 * RAG Metric의 단일 평가 결과.
 */
public final class RagMetricResult {

    private final String metricName;
    private final double score;
    private final String reason;
    private final boolean applicable;

    public RagMetricResult(String metricName, double score, String reason) {
        this(metricName, score, reason, true);
    }

    private RagMetricResult(String metricName, double score, String reason, boolean applicable) {
        this.metricName = requireText(metricName, "metricName");
        this.score = applicable ? validateScore(score) : 0.0;
        this.reason = normalizeReason(reason);
        this.applicable = applicable;
    }

    public static RagMetricResult notApplicable(String metricName, String reason) {
        return new RagMetricResult(metricName, 0.0, reason, false);
    }

    public String getMetricName() {
        return metricName;
    }

    public double getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }

    public boolean isApplicable() {
        return applicable;
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

    private static double validateScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            throw new IllegalArgumentException("score must be a finite number");
        }

        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(
                    "score must be between 0.0 and 1.0: " + score);
        }

        return score;
    }

    private static String normalizeReason(String reason) {
        return reason == null ? "" : reason.trim();
    }

    @Override
    public String toString() {
        return "RagMetricResult{" +
                "metricName='" + metricName + '\'' +
                ", score=" + (applicable ? score : "N/A") +
                ", applicable=" + applicable +
                ", reason='" + reason + '\'' +
                '}';
    }
}