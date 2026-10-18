/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.regression;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kr.co.goms.ai.rag.eval.regression.RagEvaluationRegressionResult.MetricResult;

/**
 * RAG Evaluation Regression 결과를 JSON 형식으로 저장한다.
 * 
 * RagEvaluationRegressionWriter writer = new RagEvaluationRegressionWriter();
 * writer.write( regressionResult, Path.of("eval/result/rag-regression-result.json"));
 * 
 * {
 *   "baselineName": "RAG-BASELINE-001",
 *   "datasetName": "lunchwork-seoul-v1",
 *   "passed": false,
 *   "metrics": [
 *     {
 *       "metricName": "faithfulness",
 *       "baselineScore": 0.93,
 *       "currentScore": 0.91,
 *       "minimumAcceptedScore": 0.90,
 *       "difference": -0.02,
 *       "passed": true
 *     },
 *     {
 *       "metricName": "context_precision",
 *       "baselineScore": 0.84,
 *       "currentScore": 0.77,
 *       "minimumAcceptedScore": 0.79,
 *       "difference": -0.07,
 *       "passed": false
 *     }
 *   ]
 * }
 */
public final class RagEvaluationRegressionWriter {

    private final Gson gson;

    public RagEvaluationRegressionWriter() {
        this(new GsonBuilder().setPrettyPrinting().create());
    }

    public RagEvaluationRegressionWriter(Gson gson) {
        if (gson == null) {
            throw new NullPointerException("gson must not be null");
        }

        this.gson = gson;
    }

    public void write(RagEvaluationRegressionResult result, Path path) throws IOException {
        if (result == null) {
            throw new NullPointerException("result must not be null");
        }

        if (path == null) {
            throw new NullPointerException("path must not be null");
        }

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            write(result, writer);
        }
    }

    public void write(RagEvaluationRegressionResult result, Writer writer) {
        if (result == null) {
            throw new NullPointerException("result must not be null");
        }

        if (writer == null) {
            throw new NullPointerException("writer must not be null");
        }

        RegressionJson regressionJson = convert(result);

        gson.toJson(regressionJson, writer);
    }

    private RegressionJson convert(RagEvaluationRegressionResult result) {
        RegressionJson regressionJson = new RegressionJson();

        regressionJson.baselineName = result.getBaselineName();
        regressionJson.datasetName = result.getDatasetName();
        regressionJson.passed = result.isPassed();
        regressionJson.metrics = new ArrayList<>();

        for (MetricResult metricResult : result.getMetricResults()) {
            regressionJson.metrics.add(convertMetric(metricResult));
        }

        return regressionJson;
    }

    private MetricJson convertMetric(MetricResult result) {
        MetricJson metricJson = new MetricJson();

        metricJson.metricName = result.getMetricName();
        metricJson.baselineScore = result.getBaselineScore();
        metricJson.currentScore = result.getCurrentScore();
        metricJson.minimumAcceptedScore = result.getMinimumAcceptedScore();
        metricJson.difference = result.getDifference();
        metricJson.passed = result.isPassed();

        return metricJson;
    }

    private static final class RegressionJson {
        private String baselineName;
        private String datasetName;
        private boolean passed;
        private List<MetricJson> metrics;
    }

    private static final class MetricJson {
        private String metricName;
        private double baselineScore;
        private double currentScore;
        private double minimumAcceptedScore;
        private double difference;
        private boolean passed;
    }
}