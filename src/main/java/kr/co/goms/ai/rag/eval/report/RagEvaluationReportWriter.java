/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.report;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kr.co.goms.ai.rag.eval.RagMetricResult;
import kr.co.goms.ai.rag.eval.model.RagEvaluationRetrievedDocument;
import kr.co.goms.ai.rag.eval.model.RagRetrievalEvaluationResult;

/**
 * RAG Evaluation Report를 JSON 형식으로 저장한다.
 *
 * <pre>
 * {
 *   "datasetName": "rag-golden-v1",
 *   "averageScore": 0.8098,
 *
 *   "retrievalSummary": {
 *     "evaluatedCases": 34,
 *     "hitCount": 31,
 *     "hitRateAtK": 0.9118,
 *     "averageRecallAtK": 0.9118,
 *     "meanReciprocalRank": 0.8627
 *   },
 *
 *   "answerSummary": {
 *     "evaluatedCases": 40,
 *     "averageScore": 0.8098
 *   },
 *
 *   "entries": [
 *     {
 *       "caseId": "RAG-GOLD-001",
 *       "question": "...",
 *       "overallScore": 1.0,
 *       "metrics": [
 *         ...
 *       ],
 *       "retrieval": {
 *         "applicable": true,
 *         "hitAtK": true,
 *         "recallAtK": 1.0,
 *         "mrr": 1.0
 *       },
 *       "expectedDocuments": [
 *         "OEBPS/Text/chapter10_1.xhtml"
 *       ],
 *       "retrievedDocuments": [
 *         {
 *           "chunkId": "...",
 *           "sourcePath": "OEBPS/Text/chapter10_1.xhtml",
 *           "title": "...",
 *           "rank": 1,
 *           "score": 0.95,
 *           "retrievalSource": "VECTOR",
 *           "vectorScore": 0.95,
 *           "graphScore": null,
 *           "graphWeight": null,
 *           "finalScore": 0.95,
 *           "metadata": {
 *             ...
 *           }
 *         }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * Retrieval Evaluation은 Primary Metric,
 * Answer Evaluation은 Secondary Metric으로 분리하여 기록한다.
 *
 * 기존 averageScore는 과거 Report 및 Comparison과의
 * backward compatibility를 위해 유지한다.
 */
public final class RagEvaluationReportWriter {

	private final Gson gson;

	public RagEvaluationReportWriter() {
		this(new GsonBuilder().setPrettyPrinting().create());
	}

	public RagEvaluationReportWriter(Gson gson) {
		this.gson = Objects.requireNonNull(gson, "gson must not be null");
	}

	public void write(RagEvaluationReport report, Path path) throws IOException {

		Objects.requireNonNull(report, "report must not be null");
		Objects.requireNonNull(path, "path must not be null");

		Path parent = path.getParent();

		if (parent != null) Files.createDirectories(parent);

		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			write(report, writer);
		}
	}

	public void write(RagEvaluationReport report, Writer writer) {

		Objects.requireNonNull(report, "report must not be null");
		Objects.requireNonNull(writer, "writer must not be null");

		gson.toJson(convert(report), writer);
	}

	private ReportJson convert(RagEvaluationReport report) {

		ReportJson result = new ReportJson();

		result.datasetName = report.getDatasetName();
		result.averageScore = report.getAverageScore();
		result.retrievalSummary = convertRetrievalSummary(report.getRetrievalSummary());
		result.answerSummary = convertAnswerSummary(report.getAnswerSummary());
		result.entries = new ArrayList<>();

		for (RagEvaluationReport.Entry entry : report.getEntries()) result.entries.add(convertEntry(entry));

		return result;
	}

	private RetrievalSummaryJson convertRetrievalSummary(RagEvaluationReport.RetrievalSummary summary) {

		if (summary == null) return null;

		RetrievalSummaryJson result = new RetrievalSummaryJson();

		result.evaluatedCases = summary.getEvaluatedCases();
		result.hitCount = summary.getHitCount();
		result.hitRateAtK = summary.getHitRateAtK();
		result.averageRecallAtK = summary.getAverageRecallAtK();
		result.meanReciprocalRank = summary.getMeanReciprocalRank();

		return result;
	}

	private AnswerSummaryJson convertAnswerSummary(RagEvaluationAnswerSummary summary) {

		if (summary == null) return null;

		AnswerSummaryJson result = new AnswerSummaryJson();

		result.evaluatedCases = summary.getEvaluatedCases();
		result.averageScore = summary.getAverageScore();

		return result;
	}

	private EntryJson convertEntry(RagEvaluationReport.Entry entry) {

		EntryJson result = new EntryJson();

		result.caseId = entry.getCaseId();
		result.question = entry.getQuestion();
		result.overallScore = entry.getResult().getOverallScore();
		result.metrics = convertMetrics(entry);
		result.retrieval = convertRetrieval(entry.getRetrievalEvaluationResult());
		result.expectedDocuments = copyStrings(entry.getExpectedDocuments());
		result.retrievedDocuments = convertRetrievedDocuments(entry.getRetrievedDocuments());

		return result;
	}

	private List<MetricJson> convertMetrics(RagEvaluationReport.Entry entry) {

		List<MetricJson> result = new ArrayList<>();

		if (entry.getResult().getMetricResults() == null) return result;

		for (RagMetricResult metricResult : entry.getResult().getMetricResults()) {

			if (metricResult == null) continue;

			MetricJson metric = new MetricJson();

			metric.name = metricResult.getMetricName();
			metric.score = metricResult.getScore();
			metric.reason = metricResult.getReason();

			result.add(metric);
		}

		return result;
	}

	private RetrievalJson convertRetrieval(RagRetrievalEvaluationResult retrievalResult) {

		if (retrievalResult == null) return null;

		RetrievalJson result = new RetrievalJson();

		result.applicable = retrievalResult.isApplicable();
		result.hitAtK = retrievalResult.isHitAtK();
		result.recallAtK = retrievalResult.getRecallAtK();
		result.mrr = retrievalResult.getMrr();

		return result;
	}

	private List<RetrievedDocumentJson> convertRetrievedDocuments(List<RagEvaluationRetrievedDocument> documents) {

		List<RetrievedDocumentJson> result = new ArrayList<>();

		if (documents == null || documents.isEmpty()) return result;

		for (RagEvaluationRetrievedDocument document : documents) {

			if (document == null) continue;

			result.add(convertRetrievedDocument(document));
		}

		return result;
	}

	private RetrievedDocumentJson convertRetrievedDocument(RagEvaluationRetrievedDocument document) {

		RetrievedDocumentJson result = new RetrievedDocumentJson();

		result.chunkId = document.getChunkId();
		result.sourcePath = document.getSourcePath();
		result.title = document.getTitle();
		result.rank = document.getRank();
		result.score = document.getScore();
		result.retrievalSource = document.getRetrievalSource();
		result.vectorScore = document.getVectorScore();
		result.graphScore = document.getGraphScore();
		result.graphWeight = document.getGraphWeight();
		result.finalScore = document.getFinalScore();
		result.metadata = copyMetadata(document.getMetadata());

		return result;
	}

	private List<String> copyStrings(List<String> values) {

		if (values == null || values.isEmpty()) return new ArrayList<>();

		return new ArrayList<>(values);
	}

	private Map<String, String> copyMetadata(Map<String, String> metadata) {

		if (metadata == null || metadata.isEmpty()) return new LinkedHashMap<>();

		return new LinkedHashMap<>(metadata);
	}

	private static final class ReportJson {

		private String datasetName;
		private double averageScore;
		private RetrievalSummaryJson retrievalSummary;
		private AnswerSummaryJson answerSummary;
		private List<EntryJson> entries;
	}

	private static final class RetrievalSummaryJson {

		private int evaluatedCases;
		private int hitCount;
		private double hitRateAtK;
		private double averageRecallAtK;
		private double meanReciprocalRank;
	}

	private static final class AnswerSummaryJson {

		private int evaluatedCases;
		private double averageScore;
	}

	private static final class EntryJson {

		private String caseId;
		private String question;
		private double overallScore;
		private List<MetricJson> metrics;
		private RetrievalJson retrieval;
		private List<String> expectedDocuments;
		private List<RetrievedDocumentJson> retrievedDocuments;
	}

	private static final class MetricJson {

		private String name;
		private double score;
		private String reason;
	}

	private static final class RetrievalJson {

		private boolean applicable;
		private boolean hitAtK;
		private double recallAtK;
		private double mrr;
	}

	private static final class RetrievedDocumentJson {

		private String chunkId;
		private String sourcePath;
		private String title;
		private int rank;
		private double score;
		private String retrievalSource;
		private Double vectorScore;
		private Double graphScore;
		private Double graphWeight;
		private double finalScore;
		private Map<String, String> metadata;
	}
}