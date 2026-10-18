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

import kr.co.goms.ai.rag.eval.regression.RagEvaluationBaseline.MetricBaseline;

/**
 * RAG Evaluation Baseline을 JSON 형식으로 저장한다.
 * 
 * RagEvaluationBaselineWriter writer = new RagEvaluationBaselineWriter();
 * writer.write( baseline, Path.of("eval/baseline/rag-baseline-001.json"));
 * 
 * {
 *   "name": "RAG-BASELINE-001",
 *   "datasetName": "lunchwork-seoul-v1",
 *   "metrics": [
 *     {
 *       "metricName": "faithfulness",
 *       "score": 0.93,
 *       "tolerance": 0.03
 *     },
 *     {
 *       "metricName": "answer_relevancy",
 *       "score": 0.90,
 *       "tolerance": 0.05
 *     }
 *   ]
 * }
 */
public final class RagEvaluationBaselineWriter {

    private final Gson gson;

    public RagEvaluationBaselineWriter() {
        this(new GsonBuilder().setPrettyPrinting().create());
    }

    public RagEvaluationBaselineWriter(Gson gson) {
        if (gson == null) {
            throw new NullPointerException("gson must not be null");
        }

        this.gson = gson;
    }

    public void write(RagEvaluationBaseline baseline, Path path) throws IOException {
        if (baseline == null) {
            throw new NullPointerException("baseline must not be null");
        }

        if (path == null) {
            throw new NullPointerException("path must not be null");
        }

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            write(baseline, writer);
        }
    }

    public void write(RagEvaluationBaseline baseline, Writer writer) {
        if (baseline == null) {
            throw new NullPointerException("baseline must not be null");
        }

        if (writer == null) {
            throw new NullPointerException("writer must not be null");
        }

        BaselineJson baselineJson = convert(baseline);

        gson.toJson(baselineJson, writer);
    }

    private BaselineJson convert(RagEvaluationBaseline baseline) {
        BaselineJson result = new BaselineJson();

        result.name = baseline.getName();
        result.datasetName = baseline.getDatasetName();
        result.metrics = new ArrayList<>();

        for (MetricBaseline metric : baseline.getMetrics()) {
            MetricJson metricJson = new MetricJson();

            metricJson.metricName = metric.getMetricName();
            metricJson.score = metric.getScore();
            metricJson.tolerance = metric.getTolerance();

            result.metrics.add(metricJson);
        }

        return result;
    }

    private static final class BaselineJson {
        private String name;
        private String datasetName;
        private List<MetricJson> metrics;
    }

    private static final class MetricJson {
        private String metricName;
        private double score;
        private double tolerance;
    }
}