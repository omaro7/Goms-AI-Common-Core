/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.benchmark;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import kr.co.goms.ai.project.CurrentProjectProvider;
import kr.co.goms.ai.project.EpubProjectContext;
import kr.co.goms.ai.rag.embedding.EmbeddingException;
import kr.co.goms.ai.rag.eval.path.RagEvaluationPathResolver;
import kr.co.goms.ai.rag.util.RagUtil;
import kr.co.goms.ai.rag.vector.VectorStoreException;

/**
 * 현재 열린 GomsBook 프로젝트의 최신 Golden Dataset을 사용하여
 * Vector Store Benchmark를 실행하고 JSON Report를 저장합니다.
 */
public final class DefaultVectorStoreBenchmarkExecutionService
        implements VectorStoreBenchmarkExecutionService {

    private final CurrentProjectProvider currentProjectProvider;
    private final RagEvaluationPathResolver pathResolver;
    private final VectorStoreBenchmarkService benchmarkService;
    private final VectorStoreBenchmarkReportWriter reportWriter;

    public DefaultVectorStoreBenchmarkExecutionService(
            CurrentProjectProvider currentProjectProvider,
            RagEvaluationPathResolver pathResolver,
            VectorStoreBenchmarkService benchmarkService,
            VectorStoreBenchmarkReportWriter reportWriter) {

        this.currentProjectProvider = Objects.requireNonNull(currentProjectProvider, "currentProjectProvider must not be null");
        this.pathResolver = Objects.requireNonNull(pathResolver, "pathResolver must not be null");
        this.benchmarkService = Objects.requireNonNull(benchmarkService, "benchmarkService must not be null");
        this.reportWriter = Objects.requireNonNull(reportWriter, "reportWriter must not be null");
    }

    @Override
    public VectorStoreBenchmarkExecutionResult execute()
            throws IOException, VectorStoreException, EmbeddingException {

        EpubProjectContext project = currentProjectProvider.getCurrentProject();

        if (project == null) throw new IllegalStateException("Current EPUB project is not available.");

        Path projectRoot = Objects.requireNonNull(project.getProjectRoot(), "projectRoot must not be null").toAbsolutePath().normalize();

        Path projectFileName = projectRoot.getFileName();

        if (projectFileName == null) throw new IllegalStateException("Unable to resolve projectId from project root: " + projectRoot);

        String projectId = RagUtil.requireProjectId(projectFileName.toString());

        Path datasetPath = pathResolver.resolveLatestGoldenDataset(projectId);
        Path reportPath = pathResolver.resolveVectorStoreBenchmarkReport(projectId);

        VectorStoreBenchmarkReport report = benchmarkService.benchmark(datasetPath);

        reportWriter.write(report, reportPath);

        return new VectorStoreBenchmarkExecutionResult(datasetPath, reportPath, report);
    }
}