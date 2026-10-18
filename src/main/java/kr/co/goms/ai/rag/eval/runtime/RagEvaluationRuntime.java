/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.runtime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import kr.co.goms.ai.rag.eval.dataset.RagEvaluationDataset;
import kr.co.goms.ai.rag.eval.dataset.RagEvaluationDatasetLoader;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationBaseline;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationBaselineLoader;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationRegressionChecker;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationRegressionResult;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationRegressionWriter;
import kr.co.goms.ai.rag.eval.report.RagEvaluationReport;
import kr.co.goms.ai.rag.eval.report.RagEvaluationReportWriter;
import kr.co.goms.ai.rag.eval.runner.RagEvaluationRunner;

/**
 * RAG Evaluation 전체 실행 흐름을 관리한다.
 * Dataset, Runner, Evaluator, Report, Regression 계층을 한 번에 조립해서 실행하는 상위 런타임
 * 
 * RagEvaluationRegressionResult result = runtime.evaluateRegression(
 *         Path.of("eval/dataset/lunchwork-seoul-v1.json"),
 *         Path.of("eval/baseline/rag-baseline-001.json"),
 *         Path.of("eval/result/report.json"),
 *         Path.of("eval/result/regression.json"));
 */
public final class RagEvaluationRuntime {

    private final RagEvaluationRunner runner;
    private final RagEvaluationDatasetLoader datasetLoader;
    private final RagEvaluationReportWriter reportWriter;
    private final RagEvaluationBaselineLoader baselineLoader;
    private final RagEvaluationRegressionChecker regressionChecker;
    private final RagEvaluationRegressionWriter regressionWriter;

    public RagEvaluationRuntime(
            RagEvaluationRunner runner,
            RagEvaluationDatasetLoader datasetLoader,
            RagEvaluationReportWriter reportWriter,
            RagEvaluationBaselineLoader baselineLoader,
            RagEvaluationRegressionChecker regressionChecker,
            RagEvaluationRegressionWriter regressionWriter) {

        this.runner = Objects.requireNonNull(runner, "runner must not be null");
        this.datasetLoader = Objects.requireNonNull(datasetLoader, "datasetLoader must not be null");
        this.reportWriter = Objects.requireNonNull(reportWriter, "reportWriter must not be null");
        this.baselineLoader = Objects.requireNonNull(baselineLoader, "baselineLoader must not be null");
        this.regressionChecker = Objects.requireNonNull(
                regressionChecker,
                "regressionChecker must not be null");
        this.regressionWriter = Objects.requireNonNull(
                regressionWriter,
                "regressionWriter must not be null");
    }

    public RagEvaluationReport evaluate(Path datasetPath) throws IOException {
        RagEvaluationDataset dataset = datasetLoader.load(datasetPath);
        return runner.run(dataset);
    }

    public RagEvaluationReport evaluate(Path datasetPath, Path reportPath) throws IOException {
        RagEvaluationReport report = evaluate(datasetPath);
        reportWriter.write(report, reportPath);
        return report;
    }

    public RagEvaluationRegressionResult evaluateRegression(
            Path datasetPath,
            Path baselinePath,
            Path reportPath,
            Path regressionPath) throws IOException {

        RagEvaluationReport report = evaluate(datasetPath);

        if (reportPath != null) {
            reportWriter.write(report, reportPath);
        }

        RagEvaluationBaseline baseline = baselineLoader.load(baselinePath);
        RagEvaluationRegressionResult result = regressionChecker.check(baseline, report);

        if (regressionPath != null) {
            regressionWriter.write(result, regressionPath);
        }

        return result;
    }
}