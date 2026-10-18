/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.path;

import java.nio.file.Path;

import kr.co.goms.ai.rag.eval.profile.RagEvaluationProfile;

/**
 * RAG Evaluation 경로 Resolver.
 *
 * Golden Dataset 버전과 Evaluation Profile 버전을 분리하여 관리한다.
 *
 * <pre>
 * Dataset
 * rag-{projectId}-golden-v1.json
 * rag-{projectId}-golden-v2.json
 *
 * Report
 * reports/
 * ├─ vector-only-v1/
 * │  └─ rag-{projectId}-vector-only-v1-report.json
 * ├─ vector-graph-v1/
 * │  └─ rag-{projectId}-vector-graph-v1-report.json
 * └─ vector-graph-v2/
 *    └─ rag-{projectId}-vector-graph-v2-report.json
 * </pre>
 */
public interface RagEvaluationPathResolver {

    Path resolveEvalDirectory(String projectId);

    Path resolveGoldenReport(String projectId, RagEvaluationProfile profile);

    default Path resolveGoldenDataset(String projectId) {
        return resolveGoldenDataset(projectId, 1);
    }

    Path resolveGoldenDataset(String projectId, int version);

    int resolveLatestGoldenVersion(String projectId);

    default Path resolveLatestGoldenDataset(String projectId) {
        return resolveGoldenDataset(projectId, resolveLatestGoldenVersion(projectId));
    }

    Path resolveResultDirectory(String projectId);

    default Path resolveGoldenReport(String projectId) {
        return resolveGoldenReport(projectId, 1);
    }

    Path resolveGoldenReport(String projectId, int version);

    Path resolveBaselineDirectory(String projectId);

    default Path resolveGoldenBaseline(String projectId) {
        return resolveGoldenBaseline(projectId, 1);
    }

    Path resolveGoldenBaseline(String projectId, int version);

    Path resolveRegressionDirectory(String projectId);

    default Path resolveGoldenRegression(String projectId) {
        return resolveGoldenRegression(projectId, 1);
    }

    Path resolveGoldenRegression(String projectId, int version);

    Path resolveBenchmarkDirectory(String projectId);

    Path resolveVectorStoreBenchmarkReport(String projectId);
}