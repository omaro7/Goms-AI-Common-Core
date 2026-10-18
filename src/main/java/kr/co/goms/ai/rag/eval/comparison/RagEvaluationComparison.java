/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.comparison;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 두 RAG Evaluation Report의 비교 결과.
 *
 * <pre>
 * RagEvaluationComparison
 * │
 * ├─ summary
 * │   └─ Retrieval Comparison
 * │
 * ├─ answerSummary
 * │   └─ Answer Evaluation Comparison
 * │
 * ├─ baselineAverageScore
 * ├─ candidateAverageScore
 * ├─ averageScoreDelta
 * │   └─ Legacy compatibility
 * │
 * └─ entries
 * </pre>
 *
 * Retrieval Comparison은 Primary,
 * Answer Comparison은 Secondary Metric으로 관리한다.
 */
public final class RagEvaluationComparison {

	private static final double RANK_DELTA_EPSILON = 0.0000001;

	private final String datasetName;
	private final String baselineExperimentId;
	private final String candidateExperimentId;

	private final AnswerSummary answerSummary;

	/**
	 * Legacy compatibility.
	 *
	 * 신규 코드에서는 answerSummary 사용을 권장한다.
	 */
	private final double baselineAverageScore;
	private final double candidateAverageScore;
	private final double averageScoreDelta;

	private final List<Entry> entries;
	private final Summary summary;

	public RagEvaluationComparison(String datasetName, String baselineExperimentId, String candidateExperimentId, double baselineAverageScore, double candidateAverageScore, List<Entry> entries) {
		this.datasetName = requireText(datasetName, "datasetName");
		this.baselineExperimentId = requireText(baselineExperimentId, "baselineExperimentId");
		this.candidateExperimentId = requireText(candidateExperimentId, "candidateExperimentId");
		this.answerSummary = new AnswerSummary(baselineAverageScore, candidateAverageScore);
		this.baselineAverageScore = this.answerSummary.getBaselineAverageScore();
		this.candidateAverageScore = this.answerSummary.getCandidateAverageScore();
		this.averageScoreDelta = this.answerSummary.getAverageScoreDelta();
		this.entries = normalizeEntries(entries);
		this.summary = Summary.from(this.entries);
	}

	public String getDatasetName() {
		return datasetName;
	}

	public String getBaselineExperimentId() {
		return baselineExperimentId;
	}

	public String getCandidateExperimentId() {
		return candidateExperimentId;
	}

	public AnswerSummary getAnswerSummary() {
		return answerSummary;
	}

	/**
	 * Legacy compatibility.
	 */
	public double getBaselineAverageScore() {
		return baselineAverageScore;
	}

	/**
	 * Legacy compatibility.
	 */
	public double getCandidateAverageScore() {
		return candidateAverageScore;
	}

	/**
	 * Legacy compatibility.
	 */
	public double getAverageScoreDelta() {
		return averageScoreDelta;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public Summary getSummary() {
		return summary;
	}

	public List<Entry> getRegressions() {
		return filter(RagEvaluationComparisonType.HIT_TO_MISS);
	}

	public List<Entry> getImprovements() {
		return filter(RagEvaluationComparisonType.MISS_TO_HIT);
	}

	private List<Entry> filter(RagEvaluationComparisonType type) {

		Objects.requireNonNull(type, "type must not be null");

		List<Entry> result = new ArrayList<>();

		for (Entry entry : entries) {
			if (entry.getType() == type) result.add(entry);
		}

		return Collections.unmodifiableList(result);
	}

	private static List<Entry> normalizeEntries(List<Entry> entries) {

		if (entries == null || entries.isEmpty()) return Collections.emptyList();

		List<Entry> normalized = new ArrayList<>();

		for (Entry entry : entries) {
			if (entry != null) normalized.add(entry);
		}

		return Collections.unmodifiableList(normalized);
	}

	private static String requireText(String value, String fieldName) {

		if (value == null) throw new NullPointerException(fieldName + " must not be null");

		String normalized = value.trim();

		if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");

		return normalized;
	}

	private static double requireScore(double score, String fieldName) {

		if (!Double.isFinite(score)) throw new IllegalArgumentException(fieldName + " must be finite");
		if (score < 0.0 || score > 1.0) throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");

		return score;
	}

	/**
	 * Answer Evaluation 비교 Summary.
	 *
	 * Retrieval 성능과 분리하여 Secondary Metric으로 관리한다.
	 */
	public static final class AnswerSummary {

		private final double baselineAverageScore;
		private final double candidateAverageScore;
		private final double averageScoreDelta;

		public AnswerSummary(double baselineAverageScore, double candidateAverageScore) {
			this.baselineAverageScore = requireScore(baselineAverageScore, "baselineAverageScore");
			this.candidateAverageScore = requireScore(candidateAverageScore, "candidateAverageScore");
			this.averageScoreDelta = candidateAverageScore - baselineAverageScore;
		}

		public double getBaselineAverageScore() {
			return baselineAverageScore;
		}

		public double getCandidateAverageScore() {
			return candidateAverageScore;
		}

		public double getAverageScoreDelta() {
			return averageScoreDelta;
		}

		@Override
		public String toString() {
			return "AnswerSummary{baselineAverageScore=" + baselineAverageScore + ", candidateAverageScore=" + candidateAverageScore + ", averageScoreDelta=" + averageScoreDelta + '}';
		}
	}

	/**
	 * 개별 Golden Case 비교 결과.
	 */
	public static final class Entry {

		private final String caseId;
		private final String question;
		private final RagEvaluationComparisonType type;

		private final boolean baselineApplicable;
		private final boolean candidateApplicable;

		private final boolean baselineHitAtK;
		private final boolean candidateHitAtK;

		private final double baselineRecallAtK;
		private final double candidateRecallAtK;
		private final double recallDelta;

		private final double baselineMrr;
		private final double candidateMrr;
		private final double mrrDelta;

		private final Integer baselineFirstRelevantRank;
		private final Integer candidateFirstRelevantRank;

		private final double baselineOverallScore;
		private final double candidateOverallScore;
		private final double overallScoreDelta;

		public Entry(String caseId, String question, RagEvaluationComparisonType type, boolean baselineApplicable, boolean candidateApplicable, boolean baselineHitAtK, boolean candidateHitAtK, double baselineRecallAtK, double candidateRecallAtK, double baselineMrr, double candidateMrr, double baselineOverallScore, double candidateOverallScore) {
			this.caseId = requireText(caseId, "caseId");
			this.question = requireText(question, "question");
			this.type = Objects.requireNonNull(type, "type must not be null");
			this.baselineApplicable = baselineApplicable;
			this.candidateApplicable = candidateApplicable;
			this.baselineHitAtK = baselineHitAtK;
			this.candidateHitAtK = candidateHitAtK;
			this.baselineRecallAtK = requireScore(baselineRecallAtK, "baselineRecallAtK");
			this.candidateRecallAtK = requireScore(candidateRecallAtK, "candidateRecallAtK");
			this.recallDelta = candidateRecallAtK - baselineRecallAtK;
			this.baselineMrr = requireScore(baselineMrr, "baselineMrr");
			this.candidateMrr = requireScore(candidateMrr, "candidateMrr");
			this.mrrDelta = candidateMrr - baselineMrr;
			this.baselineFirstRelevantRank = resolveFirstRelevantRank(baselineMrr);
			this.candidateFirstRelevantRank = resolveFirstRelevantRank(candidateMrr);
			this.baselineOverallScore = requireScore(baselineOverallScore, "baselineOverallScore");
			this.candidateOverallScore = requireScore(candidateOverallScore, "candidateOverallScore");
			this.overallScoreDelta = candidateOverallScore - baselineOverallScore;
		}

		public String getCaseId() {
			return caseId;
		}

		public String getQuestion() {
			return question;
		}

		public RagEvaluationComparisonType getType() {
			return type;
		}

		public boolean isBaselineApplicable() {
			return baselineApplicable;
		}

		public boolean isCandidateApplicable() {
			return candidateApplicable;
		}

		public boolean isBaselineHitAtK() {
			return baselineHitAtK;
		}

		public boolean isCandidateHitAtK() {
			return candidateHitAtK;
		}

		public double getBaselineRecallAtK() {
			return baselineRecallAtK;
		}

		public double getCandidateRecallAtK() {
			return candidateRecallAtK;
		}

		public double getRecallDelta() {
			return recallDelta;
		}

		public double getBaselineMrr() {
			return baselineMrr;
		}

		public double getCandidateMrr() {
			return candidateMrr;
		}

		public double getMrrDelta() {
			return mrrDelta;
		}

		public Integer getBaselineFirstRelevantRank() {
			return baselineFirstRelevantRank;
		}

		public Integer getCandidateFirstRelevantRank() {
			return candidateFirstRelevantRank;
		}

		public double getBaselineOverallScore() {
			return baselineOverallScore;
		}

		public double getCandidateOverallScore() {
			return candidateOverallScore;
		}

		public double getOverallScoreDelta() {
			return overallScoreDelta;
		}

		public boolean isRankImproved() {
			return baselineHitAtK && candidateHitAtK && mrrDelta > RANK_DELTA_EPSILON;
		}

		public boolean isRankRegressed() {
			return baselineHitAtK && candidateHitAtK && mrrDelta < -RANK_DELTA_EPSILON;
		}

		private static Integer resolveFirstRelevantRank(double mrr) {

			if (mrr <= 0.0) return null;

			return (int) Math.round(1.0 / mrr);
		}

		@Override
		public String toString() {
			return "Entry{caseId='" + caseId + '\'' + ", type=" + type + ", baselineHitAtK=" + baselineHitAtK + ", candidateHitAtK=" + candidateHitAtK + ", baselineMrr=" + baselineMrr + ", candidateMrr=" + candidateMrr + ", mrrDelta=" + mrrDelta + ", baselineOverallScore=" + baselineOverallScore + ", candidateOverallScore=" + candidateOverallScore + '}';
		}
	}

	/**
	 * Retrieval Evaluation 비교 Summary.
	 */
	public static final class Summary {

		private final int totalCases;
		private final int applicableCases;
		private final int missToHitCount;
		private final int hitToMissCount;
		private final int stableHitCount;
		private final int stableMissCount;
		private final int rankImprovedCount;
		private final int rankRegressedCount;
		private final int notApplicableCount;

		private Summary(int totalCases, int applicableCases, int missToHitCount, int hitToMissCount, int stableHitCount, int stableMissCount, int rankImprovedCount, int rankRegressedCount, int notApplicableCount) {
			this.totalCases = totalCases;
			this.applicableCases = applicableCases;
			this.missToHitCount = missToHitCount;
			this.hitToMissCount = hitToMissCount;
			this.stableHitCount = stableHitCount;
			this.stableMissCount = stableMissCount;
			this.rankImprovedCount = rankImprovedCount;
			this.rankRegressedCount = rankRegressedCount;
			this.notApplicableCount = notApplicableCount;
		}

		private static Summary from(List<Entry> entries) {

			if (entries == null || entries.isEmpty()) return new Summary(0, 0, 0, 0, 0, 0, 0, 0, 0);

			int applicableCases = 0;
			int missToHitCount = 0;
			int hitToMissCount = 0;
			int stableHitCount = 0;
			int stableMissCount = 0;
			int rankImprovedCount = 0;
			int rankRegressedCount = 0;
			int notApplicableCount = 0;

			for (Entry entry : entries) {

				switch (entry.getType()) {
					case MISS_TO_HIT -> missToHitCount++;
					case HIT_TO_MISS -> hitToMissCount++;
					case STABLE_HIT -> stableHitCount++;
					case STABLE_MISS -> stableMissCount++;
					case NOT_APPLICABLE -> notApplicableCount++;
				}

				if (entry.isBaselineApplicable() && entry.isCandidateApplicable()) applicableCases++;
				if (entry.isRankImproved()) rankImprovedCount++;
				if (entry.isRankRegressed()) rankRegressedCount++;
			}

			return new Summary(entries.size(), applicableCases, missToHitCount, hitToMissCount, stableHitCount, stableMissCount, rankImprovedCount, rankRegressedCount, notApplicableCount);
		}

		public int getTotalCases() {
			return totalCases;
		}

		public int getApplicableCases() {
			return applicableCases;
		}

		public int getMissToHitCount() {
			return missToHitCount;
		}

		public int getHitToMissCount() {
			return hitToMissCount;
		}

		public int getStableHitCount() {
			return stableHitCount;
		}

		public int getStableMissCount() {
			return stableMissCount;
		}

		public int getRankImprovedCount() {
			return rankImprovedCount;
		}

		public int getRankRegressedCount() {
			return rankRegressedCount;
		}

		public int getNotApplicableCount() {
			return notApplicableCount;
		}

		@Override
		public String toString() {
			return "Summary{totalCases=" + totalCases + ", applicableCases=" + applicableCases + ", missToHitCount=" + missToHitCount + ", hitToMissCount=" + hitToMissCount + ", stableHitCount=" + stableHitCount + ", stableMissCount=" + stableMissCount + ", rankImprovedCount=" + rankImprovedCount + ", rankRegressedCount=" + rankRegressedCount + ", notApplicableCount=" + notApplicableCount + '}';
		}
	}

	@Override
	public String toString() {
		return "RagEvaluationComparison{datasetName='" + datasetName + '\'' + ", baselineExperimentId='" + baselineExperimentId + '\'' + ", candidateExperimentId='" + candidateExperimentId + '\'' + ", answerSummary=" + answerSummary + ", summary=" + summary + '}';
	}
}