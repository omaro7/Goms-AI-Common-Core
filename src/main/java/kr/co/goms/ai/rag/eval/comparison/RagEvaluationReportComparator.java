/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.comparison;

import java.io.IOException;
import java.io.Reader;
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

/**
 * 두 RAG Evaluation Report를 비교한다.
 *
 * Answer Evaluation 평균 점수는 다음 우선순위로 읽는다.
 *
 * <pre>
 * 1. answerSummary.averageScore
 * 2. averageScore
 * </pre>
 *
 * 신규 Report에서는 answerSummary.averageScore를 공식 Answer Score로 사용하고,
 * 기존 Report와의 backward compatibility를 위해 최상위 averageScore를 fallback으로 사용한다.
 */
public final class RagEvaluationReportComparator {

	private final Gson gson;

	public RagEvaluationReportComparator() {
		this(new GsonBuilder().create());
	}

	public RagEvaluationReportComparator(Gson gson) {
		this.gson = Objects.requireNonNull(gson, "gson must not be null");
	}

	public RagEvaluationComparison compare(Path baselineReportPath, String baselineExperimentId, Path candidateReportPath, String candidateExperimentId) throws IOException {

		ReportJson baseline = load(baselineReportPath);
		ReportJson candidate = load(candidateReportPath);

		validateDataset(baseline, candidate);

		Map<String, EntryJson> candidateEntries = indexByCaseId(candidate.entries);
		List<RagEvaluationComparison.Entry> entries = new ArrayList<>();

		for (EntryJson baselineEntry : baseline.entries) {

			EntryJson candidateEntry = candidateEntries.get(baselineEntry.caseId);

			if (candidateEntry == null) throw new IllegalStateException("Candidate report case not found: " + baselineEntry.caseId);

			entries.add(compareEntry(baselineEntry, candidateEntry));
		}

		validateCandidateCases(baseline.entries, candidate.entries);

		double baselineAverageScore = resolveAnswerAverageScore(baseline, "baseline");
		double candidateAverageScore = resolveAnswerAverageScore(candidate, "candidate");

		return new RagEvaluationComparison(baseline.datasetName, baselineExperimentId, candidateExperimentId, baselineAverageScore, candidateAverageScore, entries);
	}

	private ReportJson load(Path reportPath) throws IOException {

		Objects.requireNonNull(reportPath, "reportPath must not be null");

		if (!Files.isRegularFile(reportPath)) throw new IllegalStateException("RAG evaluation report not found: " + reportPath);

		try (Reader reader = Files.newBufferedReader(reportPath, StandardCharsets.UTF_8)) {

			ReportJson report = gson.fromJson(reader, ReportJson.class);

			if (report == null) throw new IllegalStateException("RAG evaluation report is empty: " + reportPath);
			if (report.datasetName == null || report.datasetName.isBlank()) throw new IllegalStateException("RAG evaluation report datasetName is missing: " + reportPath);
			if (report.entries == null) throw new IllegalStateException("RAG evaluation report entries are missing: " + reportPath);

			return report;
		}
	}

	private double resolveAnswerAverageScore(ReportJson report, String reportType) {

		Objects.requireNonNull(report, "report must not be null");

		if (report.answerSummary != null && report.answerSummary.averageScore != null) {
			return requireScore(report.answerSummary.averageScore, reportType + ".answerSummary.averageScore");
		}

		if (report.averageScore != null) {
			return requireScore(report.averageScore, reportType + ".averageScore");
		}

		throw new IllegalStateException("RAG evaluation report answer average score is missing: " + reportType);
	}

	private double requireScore(Double score, String fieldName) {

		if (score == null) throw new IllegalStateException(fieldName + " is missing");
		if (!Double.isFinite(score)) throw new IllegalStateException(fieldName + " must be finite");
		if (score < 0.0 || score > 1.0) throw new IllegalStateException(fieldName + " must be between 0.0 and 1.0");

		return score;
	}

	private RagEvaluationComparison.Entry compareEntry(EntryJson baseline, EntryJson candidate) {

		validateCase(baseline, candidate);

		RetrievalJson baselineRetrieval = baseline.retrieval;
		RetrievalJson candidateRetrieval = candidate.retrieval;

		boolean baselineApplicable = baselineRetrieval != null && baselineRetrieval.applicable;
		boolean candidateApplicable = candidateRetrieval != null && candidateRetrieval.applicable;

		RagEvaluationComparisonType type = resolveType(baselineRetrieval, candidateRetrieval);

		return new RagEvaluationComparison.Entry(
				baseline.caseId,
				baseline.question,
				type,
				baselineApplicable,
				candidateApplicable,
				baselineRetrieval != null && baselineRetrieval.hitAtK,
				candidateRetrieval != null && candidateRetrieval.hitAtK,
				baselineRetrieval == null ? 0.0 : baselineRetrieval.recallAtK,
				candidateRetrieval == null ? 0.0 : candidateRetrieval.recallAtK,
				baselineRetrieval == null ? 0.0 : baselineRetrieval.mrr,
				candidateRetrieval == null ? 0.0 : candidateRetrieval.mrr,
				baseline.overallScore,
				candidate.overallScore);
	}

	private RagEvaluationComparisonType resolveType(RetrievalJson baseline, RetrievalJson candidate) {

		if (baseline == null || candidate == null || !baseline.applicable || !candidate.applicable) return RagEvaluationComparisonType.NOT_APPLICABLE;
		if (!baseline.hitAtK && candidate.hitAtK) return RagEvaluationComparisonType.MISS_TO_HIT;
		if (baseline.hitAtK && !candidate.hitAtK) return RagEvaluationComparisonType.HIT_TO_MISS;
		if (baseline.hitAtK) return RagEvaluationComparisonType.STABLE_HIT;

		return RagEvaluationComparisonType.STABLE_MISS;
	}

	private Map<String, EntryJson> indexByCaseId(List<EntryJson> entries) {

		Map<String, EntryJson> result = new LinkedHashMap<>();

		for (EntryJson entry : entries) {

			if (entry == null || entry.caseId == null || entry.caseId.isBlank()) throw new IllegalStateException("RAG evaluation report contains invalid caseId.");
			if (result.put(entry.caseId, entry) != null) throw new IllegalStateException("Duplicate RAG evaluation caseId: " + entry.caseId);
		}

		return result;
	}

	private void validateDataset(ReportJson baseline, ReportJson candidate) {

		if (!baseline.datasetName.equals(candidate.datasetName)) throw new IllegalStateException("RAG evaluation dataset mismatch. baseline=" + baseline.datasetName + ", candidate=" + candidate.datasetName);
	}

	private void validateCase(EntryJson baseline, EntryJson candidate) {

		if (!Objects.equals(baseline.caseId, candidate.caseId)) throw new IllegalStateException("RAG evaluation case mismatch. baseline=" + baseline.caseId + ", candidate=" + candidate.caseId);
	}

	private void validateCandidateCases(List<EntryJson> baselineEntries, List<EntryJson> candidateEntries) {

		Map<String, EntryJson> baselineById = indexByCaseId(baselineEntries);

		for (EntryJson candidateEntry : candidateEntries) {
			if (!baselineById.containsKey(candidateEntry.caseId)) throw new IllegalStateException("Baseline report case not found: " + candidateEntry.caseId);
		}
	}

	private static final class ReportJson {

		private String datasetName;

		/**
		 * Legacy compatibility.
		 *
		 * 구버전 Report에서 사용한다.
		 */
		private Double averageScore;

		/**
		 * 신규 Answer Evaluation Summary.
		 */
		private AnswerSummaryJson answerSummary;

		private List<EntryJson> entries;
	}

	private static final class AnswerSummaryJson {

		private Integer evaluatedCases;
		private Double averageScore;
	}

	private static final class EntryJson {

		private String caseId;
		private String question;
		private double overallScore;
		private RetrievalJson retrieval;
	}

	private static final class RetrievalJson {

		private boolean applicable;
		private boolean hitAtK;
		private double recallAtK;
		private double mrr;
	}
}