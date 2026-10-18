/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.rag.eval.RagEvaluationResult;
import kr.co.goms.ai.rag.eval.RagMetricResult;
import kr.co.goms.ai.rag.eval.model.RagRetrievalEvaluationResult;
import kr.co.goms.ai.rag.util.RagUtil;
import kr.co.goms.ai.rag.eval.model.RagEvaluationRetrievedDocument;

/**
 * RAG Evaluation 전체 Report.
 *
 * Retrieval Evaluation과 Answer Evaluation을 분리하여 집계한다.
 *
 * <pre>
 * RagEvaluationReport
 * │
 * ├─ retrievalSummary
 * │   ├─ evaluatedCases
 * │   ├─ hitCount
 * │   ├─ hitRateAtK
 * │   ├─ averageRecallAtK
 * │   └─ meanReciprocalRank
 * │
 * ├─ answerSummary
 * │   ├─ evaluatedCases
 * │   └─ averageScore
 * │
 * └─ entries
 * </pre>
 */
public final class RagEvaluationReport {

	private final String datasetName;
	private final List<Entry> entries;
	private final RetrievalSummary retrievalSummary;
	private final RagEvaluationAnswerSummary answerSummary;

	/**
	 * 기존 Report JSON 및 Comparison 호환용.
	 *
	 * 신규 코드에서는 answerSummary.averageScore 사용을 권장한다.
	 */
	private final double averageScore;

	public RagEvaluationReport(String datasetName, List<Entry> entries) {
		this.datasetName = RagUtil.requireText(datasetName, "datasetName");
		this.entries = normalizeEntries(entries);
		this.retrievalSummary = createRetrievalSummary(this.entries);
		this.answerSummary = createAnswerSummary(this.entries);
		this.averageScore = this.answerSummary.getAverageScore();
	}

	public String getDatasetName() {
		return datasetName;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public RetrievalSummary getRetrievalSummary() {
		return retrievalSummary;
	}

	public RagEvaluationAnswerSummary getAnswerSummary() {
		return answerSummary;
	}

	public int size() {
		return entries.size();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	/**
	 * 기존 코드 호환용.
	 *
	 * @return answerSummary.averageScore
	 */
	public double getAverageScore() {
		return averageScore;
	}

	public double getAverageMetricScore(String metricName) {

		String normalizedMetricName = RagUtil.requireText(metricName, "metricName");

		double total = 0.0;
		int count = 0;

		for (Entry entry : entries) {

			RagMetricResult metricResult = entry.getResult().getMetricResult(normalizedMetricName);

			if (metricResult == null) continue;

			total += metricResult.getScore();
			count++;
		}

		if (count == 0) throw new IllegalStateException("Metric not found in evaluation report: " + normalizedMetricName);

		return total / count;
	}

	public Entry getEntry(String caseId) {

		if (caseId == null || caseId.isBlank()) return null;

		String normalizedCaseId = caseId.trim();

		for (Entry entry : entries) {
			if (normalizedCaseId.equals(entry.getCaseId())) return entry;
		}

		return null;
	}

	private static List<Entry> normalizeEntries(List<Entry> entries) {

		if (entries == null || entries.isEmpty()) return Collections.emptyList();

		List<Entry> normalized = new ArrayList<>();

		for (Entry entry : entries) {
			if (entry != null) normalized.add(entry);
		}

		return Collections.unmodifiableList(normalized);
	}

	private static RagEvaluationAnswerSummary createAnswerSummary(List<Entry> entries) {

		if (entries == null || entries.isEmpty()) return RagEvaluationAnswerSummary.empty();

		double totalScore = 0.0;

		for (Entry entry : entries) totalScore += entry.getResult().getOverallScore();

		return new RagEvaluationAnswerSummary(entries.size(), totalScore / entries.size());
	}

	private static RetrievalSummary createRetrievalSummary(List<Entry> entries) {

		if (entries == null || entries.isEmpty()) return RetrievalSummary.empty();

		int evaluatedCases = 0;
		int hitCount = 0;
		double totalRecall = 0.0;
		double totalMrr = 0.0;

		for (Entry entry : entries) {

			RagRetrievalEvaluationResult retrievalResult = entry.getRetrievalEvaluationResult();

			if (retrievalResult == null || !retrievalResult.isApplicable()) continue;

			evaluatedCases++;

			if (retrievalResult.isHitAtK()) hitCount++;

			totalRecall += retrievalResult.getRecallAtK();
			totalMrr += retrievalResult.getMrr();
		}

		if (evaluatedCases == 0) return RetrievalSummary.empty();

		double hitRateAtK = (double) hitCount / evaluatedCases;
		double averageRecallAtK = totalRecall / evaluatedCases;
		double meanReciprocalRank = totalMrr / evaluatedCases;

		return new RetrievalSummary(evaluatedCases, hitCount, hitRateAtK, averageRecallAtK, meanReciprocalRank);
	}

	@Override
	public String toString() {
		return "RagEvaluationReport{datasetName='" + datasetName + '\'' + ", size=" + entries.size() + ", retrievalSummary=" + retrievalSummary + ", answerSummary=" + answerSummary + ", averageScore=" + averageScore + '}';
	}

	/**
	 * Retrieval Evaluation 전체 집계.
	 */
	public static final class RetrievalSummary {

		private final int evaluatedCases;
		private final int hitCount;
		private final double hitRateAtK;
		private final double averageRecallAtK;
		private final double meanReciprocalRank;

		public RetrievalSummary(int evaluatedCases, int hitCount, double hitRateAtK, double averageRecallAtK, double meanReciprocalRank) {
			this.evaluatedCases = requireNonNegative(evaluatedCases, "evaluatedCases");
			this.hitCount = requireNonNegative(hitCount, "hitCount");
			this.hitRateAtK = requireScore(hitRateAtK, "hitRateAtK");
			this.averageRecallAtK = requireScore(averageRecallAtK, "averageRecallAtK");
			this.meanReciprocalRank = requireScore(meanReciprocalRank, "meanReciprocalRank");
			if (hitCount > evaluatedCases) throw new IllegalArgumentException("hitCount must not be greater than evaluatedCases");
		}

		public static RetrievalSummary empty() {
			return new RetrievalSummary(0, 0, 0.0, 0.0, 0.0);
		}

		public int getEvaluatedCases() {
			return evaluatedCases;
		}

		public int getHitCount() {
			return hitCount;
		}

		public double getHitRateAtK() {
			return hitRateAtK;
		}

		public double getAverageRecallAtK() {
			return averageRecallAtK;
		}

		public double getMeanReciprocalRank() {
			return meanReciprocalRank;
		}

		public boolean isEmpty() {
			return evaluatedCases == 0;
		}

		private static int requireNonNegative(int value, String fieldName) {
			if (value < 0) throw new IllegalArgumentException(fieldName + " must not be negative");
			return value;
		}

		private static double requireScore(double value, String fieldName) {
			if (!Double.isFinite(value)) throw new IllegalArgumentException(fieldName + " must be finite");
			if (value < 0.0 || value > 1.0) throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
			return value;
		}

		@Override
		public String toString() {
			return "RetrievalSummary{evaluatedCases=" + evaluatedCases + ", hitCount=" + hitCount + ", hitRateAtK=" + hitRateAtK + ", averageRecallAtK=" + averageRecallAtK + ", meanReciprocalRank=" + meanReciprocalRank + '}';
		}
	}

	/**
	 * 개별 Evaluation Case의 Report Entry.
	 */
	public static final class Entry {

		private final String caseId;
		private final String question;
		private final RagEvaluationResult result;
		private final RagRetrievalEvaluationResult retrievalEvaluationResult;
		private final List<String> expectedDocuments;
		private final List<RagEvaluationRetrievedDocument> retrievedDocuments;

		public Entry(String caseId, String question, RagEvaluationResult result) {
			this(caseId, question, result, RagRetrievalEvaluationResult.notApplicable(), List.of(), List.of());
		}

		public Entry(String caseId, String question, RagEvaluationResult result, RagRetrievalEvaluationResult retrievalEvaluationResult) {
			this(caseId, question, result, retrievalEvaluationResult, List.of(), List.of());
		}

		public Entry(String caseId, String question, RagEvaluationResult result, RagRetrievalEvaluationResult retrievalEvaluationResult, List<String> expectedDocuments, List<RagEvaluationRetrievedDocument> retrievedDocuments) {
			this.caseId = RagUtil.requireText(caseId, "caseId");
			this.question = RagUtil.requireText(question, "question");
			this.result = Objects.requireNonNull(result, "result must not be null");
			this.retrievalEvaluationResult = Objects.requireNonNull(retrievalEvaluationResult, "retrievalEvaluationResult must not be null");
			this.expectedDocuments = normalizeExpectedDocuments(expectedDocuments);
			this.retrievedDocuments = normalizeRetrievedDocuments(retrievedDocuments);
		}

		public String getCaseId() {
			return caseId;
		}

		public String getQuestion() {
			return question;
		}

		public RagEvaluationResult getResult() {
			return result;
		}

		public RagRetrievalEvaluationResult getRetrievalEvaluationResult() {
			return retrievalEvaluationResult;
		}

		public List<String> getExpectedDocuments() {
			return expectedDocuments;
		}

		public List<RagEvaluationRetrievedDocument> getRetrievedDocuments() {
			return retrievedDocuments;
		}

		private static List<String> normalizeExpectedDocuments(List<String> documents) {

			if (documents == null || documents.isEmpty()) return List.of();

			List<String> normalized = new ArrayList<>();

			for (String document : documents) {

				String normalizedDocument = RagUtil.normalizeDocumentPath(document);

				if (normalizedDocument == null || normalizedDocument.isBlank()) continue;

				normalized.add(normalizedDocument);
			}

			return List.copyOf(normalized);
		}

		private static List<RagEvaluationRetrievedDocument> normalizeRetrievedDocuments(List<RagEvaluationRetrievedDocument> documents) {

			if (documents == null || documents.isEmpty()) return List.of();

			List<RagEvaluationRetrievedDocument> normalized = new ArrayList<>();

			for (RagEvaluationRetrievedDocument document : documents) {
				if (document != null) normalized.add(document);
			}

			return List.copyOf(normalized);
		}

		@Override
		public String toString() {
			return "Entry{caseId='" + caseId + '\'' + ", question='" + question + '\'' + ", overallScore=" + result.getOverallScore() + ", retrievalEvaluationResult=" + retrievalEvaluationResult + ", expectedDocuments=" + expectedDocuments + ", retrievedDocuments=" + retrievedDocuments.size() + '}';
		}
	}
}