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

import kr.co.goms.ai.rag.eval.model.RagEvaluationRetrievedDocument;
import kr.co.goms.ai.rag.eval.model.RagRetrievalEvaluationResult;

public final class RagRetrievalEvaluationReport {

	private final String datasetName;
	private final List<Entry> entries;
	private final int totalCases;
	private final int evaluatedCases;
	private final double hitRateAtK;
	private final double averageRecallAtK;
	private final double mrr;

	public RagRetrievalEvaluationReport(String datasetName, int totalCases, List<Entry> entries) {
		this.datasetName = requireText(datasetName, "datasetName");
		this.entries = normalizeEntries(entries);
		this.totalCases = requireNonNegative(totalCases, "totalCases");
		this.evaluatedCases = this.entries.size();
		this.hitRateAtK = calculateHitRate(this.entries);
		this.averageRecallAtK = calculateAverageRecall(this.entries);
		this.mrr = calculateAverageMrr(this.entries);
	}

	public String getDatasetName() {
		return datasetName;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public int getTotalCases() {
		return totalCases;
	}

	public int getEvaluatedCases() {
		return evaluatedCases;
	}

	public double getHitRateAtK() {
		return hitRateAtK;
	}

	public double getAverageRecallAtK() {
		return averageRecallAtK;
	}

	public double getMrr() {
		return mrr;
	}

	private static double calculateHitRate(List<Entry> entries) {

		if (entries.isEmpty()) return 0.0;

		int hits = 0;

		for (Entry entry : entries) if (entry.getRetrievalResult().isHitAtK()) hits++;

		return (double) hits / entries.size();
	}

	private static double calculateAverageRecall(List<Entry> entries) {

		if (entries.isEmpty()) return 0.0;

		double total = 0.0;

		for (Entry entry : entries) total += entry.getRetrievalResult().getRecallAtK();

		return total / entries.size();
	}

	private static double calculateAverageMrr(List<Entry> entries) {

		if (entries.isEmpty()) return 0.0;

		double total = 0.0;

		for (Entry entry : entries) total += entry.getRetrievalResult().getMrr();

		return total / entries.size();
	}

	private static List<Entry> normalizeEntries(List<Entry> entries) {

		if (entries == null || entries.isEmpty()) return Collections.emptyList();

		List<Entry> normalized = new ArrayList<>();

		for (Entry entry : entries) if (entry != null) normalized.add(entry);

		return Collections.unmodifiableList(normalized);
	}

	private static String requireText(String value, String fieldName) {

		if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");

		return value.trim();
	}

	private static int requireNonNegative(int value, String fieldName) {

		if (value < 0) throw new IllegalArgumentException(fieldName + " must not be negative");

		return value;
	}

	public static final class Entry {

		private final String caseId;
		private final String question;
		private final List<String> expectedDocuments;
		private final List<RagEvaluationRetrievedDocument> retrievedDocuments;
		private final RagRetrievalEvaluationResult retrievalResult;

		public Entry(String caseId, String question, List<String> expectedDocuments, List<RagEvaluationRetrievedDocument> retrievedDocuments, RagRetrievalEvaluationResult retrievalResult) {
			this.caseId = requireText(caseId, "caseId");
			this.question = requireText(question, "question");
			this.expectedDocuments = expectedDocuments == null ? List.of() : List.copyOf(expectedDocuments);
			this.retrievedDocuments = retrievedDocuments == null ? List.of() : List.copyOf(retrievedDocuments);
			this.retrievalResult = Objects.requireNonNull(retrievalResult, "retrievalResult must not be null");
		}

		public String getCaseId() {
			return caseId;
		}

		public String getQuestion() {
			return question;
		}

		public List<String> getExpectedDocuments() {
			return expectedDocuments;
		}

		public List<RagEvaluationRetrievedDocument> getRetrievedDocuments() {
			return retrievedDocuments;
		}

		public RagRetrievalEvaluationResult getRetrievalResult() {
			return retrievalResult;
		}
	}
}