/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.benchmark;

import java.util.Objects;

/**
 * 단일 Vector Store Benchmark 결과입니다.
 */
public final class VectorBenchmarkResult {

    private final String experimentId;
    private final String backend;
    private final String projectId;
    private final String model;
    private final int vectorCount;
    private final int queryCount;
    private final int warmupIterations;
    private final int measurementIterations;
    private final VectorBenchmarkLatency latency;

    public VectorBenchmarkResult(String experimentId, String backend, String projectId, String model, int vectorCount, int queryCount, int warmupIterations, int measurementIterations, VectorBenchmarkLatency latency) {
        this.experimentId = requireText(experimentId, "experimentId");
        this.backend = requireText(backend, "backend");
        this.projectId = requireText(projectId, "projectId");
        this.model = requireText(model, "model");
        this.vectorCount = vectorCount;
        this.queryCount = queryCount;
        this.warmupIterations = warmupIterations;
        this.measurementIterations = measurementIterations;
        this.latency = Objects.requireNonNull(latency, "latency must not be null");
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getBackend() {
        return backend;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getModel() {
        return model;
    }

    public int getVectorCount() {
        return vectorCount;
    }

    public int getQueryCount() {
        return queryCount;
    }

    public int getWarmupIterations() {
        return warmupIterations;
    }

    public int getMeasurementIterations() {
        return measurementIterations;
    }

    public VectorBenchmarkLatency getLatency() {
        return latency;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return value.trim();
    }
}