/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.benchmark;

/**
 * Vector Store Benchmark의 Latency 통계입니다.
 */
public final class VectorBenchmarkLatency {

    private final int sampleCount;
    private final double averageMs;
    private final double minMs;
    private final double maxMs;
    private final double p50Ms;
    private final double p95Ms;
    private final double p99Ms;

    public VectorBenchmarkLatency(int sampleCount, double averageMs, double minMs, double maxMs, double p50Ms, double p95Ms, double p99Ms) {
        this.sampleCount = sampleCount;
        this.averageMs = averageMs;
        this.minMs = minMs;
        this.maxMs = maxMs;
        this.p50Ms = p50Ms;
        this.p95Ms = p95Ms;
        this.p99Ms = p99Ms;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public double getAverageMs() {
        return averageMs;
    }

    public double getMinMs() {
        return minMs;
    }

    public double getMaxMs() {
        return maxMs;
    }

    public double getP50Ms() {
        return p50Ms;
    }

    public double getP95Ms() {
        return p95Ms;
    }

    public double getP99Ms() {
        return p99Ms;
    }

    @Override
    public String toString() {
        return "VectorBenchmarkLatency{" + "sampleCount=" + sampleCount + ", averageMs=" + averageMs + ", minMs=" + minMs + ", maxMs=" + maxMs + ", p50Ms=" + p50Ms + ", p95Ms=" + p95Ms + ", p99Ms=" + p99Ms + '}';
    }
}