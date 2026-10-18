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
 * InMemory와 Qdrant Vector Store Benchmark 비교 결과입니다.
 */
public final class VectorStoreBenchmarkReport {

    private final String datasetName;
    private final String projectId;
    private final String model;
    private final int topK;
    private final VectorBenchmarkResult memory;
    private final VectorBenchmarkResult qdrant;

    public VectorStoreBenchmarkReport(String datasetName, String projectId, String model, int topK, VectorBenchmarkResult memory, VectorBenchmarkResult qdrant) {
        this.datasetName = requireText(datasetName, "datasetName");
        this.projectId = requireText(projectId, "projectId");
        this.model = requireText(model, "model");
        this.topK = topK;
        this.memory = Objects.requireNonNull(memory, "memory must not be null");
        this.qdrant = Objects.requireNonNull(qdrant, "qdrant must not be null");
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getModel() {
        return model;
    }

    public int getTopK() {
        return topK;
    }

    public VectorBenchmarkResult getMemory() {
        return memory;
    }

    public VectorBenchmarkResult getQdrant() {
        return qdrant;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return value.trim();
    }
}