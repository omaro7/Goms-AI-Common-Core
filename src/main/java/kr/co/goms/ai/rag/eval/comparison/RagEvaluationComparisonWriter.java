/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.comparison;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * RAG Evaluation Comparison 결과를 JSON으로 저장한다.
 *
 * <pre>
 * {
 *   "datasetName": "rag-golden-v1",
 *   "baselineExperimentId": "VECTOR_ONLY_V1",
 *   "candidateExperimentId": "VECTOR_GRAPH_V6",
 *
 *   "answerSummary": {
 *     "baselineAverageScore": 0.8200,
 *     "candidateAverageScore": 0.8098,
 *     "averageScoreDelta": -0.0102
 *   },
 *
 *   "baselineAverageScore": 0.8200,
 *   "candidateAverageScore": 0.8098,
 *   "averageScoreDelta": -0.0102,
 *
 *   "summary": {
 *     ...
 *   },
 *
 *   "retrievalSummary": {
 *     ...
 *   },
 *
 *   "entries": [
 *     ...
 *   ]
 * }
 * </pre>
 *
 * retrievalSummary는 Retrieval Comparison의 공식 Summary이다.
 *
 * 기존 summary는 과거 Comparison JSON 및 소비 코드와의
 * backward compatibility를 위해 유지한다.
 */
public final class RagEvaluationComparisonWriter {

	private final Gson gson;

	public RagEvaluationComparisonWriter() {
		this(new GsonBuilder().setPrettyPrinting().create());
	}

	public RagEvaluationComparisonWriter(Gson gson) {
		this.gson = Objects.requireNonNull(gson, "gson must not be null");
	}

	public void write(RagEvaluationComparison comparison, Path path) throws IOException {

		Objects.requireNonNull(comparison, "comparison must not be null");
		Objects.requireNonNull(path, "path must not be null");

		Path parent = path.getParent();

		if (parent != null) Files.createDirectories(parent);

		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			write(comparison, writer);
		}
	}

	public void write(RagEvaluationComparison comparison, Writer writer) {

		Objects.requireNonNull(comparison, "comparison must not be null");
		Objects.requireNonNull(writer, "writer must not be null");

		gson.toJson(convert(comparison), writer);
	}

	private JsonObject convert(RagEvaluationComparison comparison) {

		JsonObject json = gson.toJsonTree(comparison).getAsJsonObject();

		json.add("retrievalSummary", gson.toJsonTree(comparison.getSummary()));

		return json;
	}
}