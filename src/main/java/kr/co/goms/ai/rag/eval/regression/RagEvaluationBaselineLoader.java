/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.regression;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import kr.co.goms.ai.rag.eval.regression.RagEvaluationBaseline.MetricBaseline;

/**
 * JSON 파일에서 RAG Evaluation Baseline을 로드한다.
 * 
 * 
 * RagEvaluationBaselineLoader loader = new RagEvaluationBaselineLoader();
 * RagEvaluationBaseline baseline = loader.load(Path.of("eval/baseline/rag-baseline-001.json"));
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
 *     },
 *     {
 *       "metricName": "context_precision",
 *       "score": 0.84,
 *       "tolerance": 0.05
 *     },
 *     {
 *       "metricName": "context_recall",
 *       "score": 0.92,
 *       "tolerance": 0.03
 *     }
 *   ]
 * }
 */
public final class RagEvaluationBaselineLoader {

    private final Gson gson;

    public RagEvaluationBaselineLoader() {
        this(new Gson());
    }

    public RagEvaluationBaselineLoader(Gson gson) {
        if (gson == null) {
            throw new NullPointerException("gson must not be null");
        }

        this.gson = gson;
    }

    public RagEvaluationBaseline load(Path path) throws IOException {
        if (path == null) {
            throw new NullPointerException("path must not be null");
        }

        if (!Files.exists(path)) {
            throw new IOException("Baseline file does not exist: " + path);
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    public RagEvaluationBaseline load(Reader reader) {
        if (reader == null) {
            throw new NullPointerException("reader must not be null");
        }

        try {
            BaselineJson baselineJson = gson.fromJson(reader, BaselineJson.class);

            if (baselineJson == null) {
                throw new IllegalArgumentException("Baseline JSON must not be empty");
            }

            String name = requireText(baselineJson.name, "name");
            String datasetName = requireText(baselineJson.datasetName, "datasetName");
            List<MetricBaseline> metrics = convertMetrics(baselineJson.metrics);

            return new RagEvaluationBaseline(name, datasetName, metrics);

        } catch (JsonParseException e) {
            throw new IllegalArgumentException(
                    "Failed to parse RAG evaluation baseline JSON",
                    e);
        }
    }

    private List<MetricBaseline> convertMetrics(List<MetricJson> metricJsonList) {
        if (metricJsonList == null || metricJsonList.isEmpty()) {
            return Collections.emptyList();
        }

        List<MetricBaseline> metrics = new ArrayList<>();

        for (MetricJson metricJson : metricJsonList) {
            if (metricJson == null) {
                continue;
            }

            metrics.add(new MetricBaseline(
                    metricJson.metricName,
                    metricJson.score,
                    metricJson.tolerance));
        }

        return metrics;
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