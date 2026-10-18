/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.benchmark;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Vector Store Benchmark 실행 결과입니다.
 */
public final class VectorStoreBenchmarkExecutionResult {

    private final Path datasetPath;
    private final Path reportPath;
    private final VectorStoreBenchmarkReport report;

    public VectorStoreBenchmarkExecutionResult(Path datasetPath, Path reportPath, VectorStoreBenchmarkReport report) {
        this.datasetPath = Objects.requireNonNull(datasetPath, "datasetPath must not be null");
        this.reportPath = Objects.requireNonNull(reportPath, "reportPath must not be null");
        this.report = Objects.requireNonNull(report, "report must not be null");
    }

    public Path getDatasetPath() {
        return datasetPath;
    }

    public Path getReportPath() {
        return reportPath;
    }

    public VectorStoreBenchmarkReport getReport() {
        return report;
    }
}