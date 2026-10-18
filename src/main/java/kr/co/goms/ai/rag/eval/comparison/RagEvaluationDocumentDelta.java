/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.comparison;

import kr.co.goms.ai.rag.util.RagUtil;

public final class RagEvaluationDocumentDelta {

	private final String sourcePath;
	private final boolean expectedDocument;
	private final Integer baselineRank;
	private final Integer candidateRank;
	private final Integer rankDelta;
	private final boolean added;
	private final boolean removed;
	private final boolean graphCandidate;
	private final boolean newGraphCandidate;
	private final String baselineRetrievalSource;
	private final String candidateRetrievalSource;

	public RagEvaluationDocumentDelta(String sourcePath, boolean expectedDocument, Integer baselineRank, Integer candidateRank, boolean graphCandidate, String baselineRetrievalSource, String candidateRetrievalSource) {
		this.sourcePath = RagUtil.requireText(RagUtil.normalizeDocumentPath(sourcePath), "sourcePath");
		this.expectedDocument = expectedDocument;
		this.baselineRank = normalizeRank(baselineRank);
		this.candidateRank = normalizeRank(candidateRank);
		this.rankDelta = this.baselineRank != null && this.candidateRank != null ? this.candidateRank - this.baselineRank : null;
		this.added = this.baselineRank == null && this.candidateRank != null;
		this.removed = this.baselineRank != null && this.candidateRank == null;
		this.graphCandidate = graphCandidate;
		this.newGraphCandidate = this.added && graphCandidate;
		this.baselineRetrievalSource = normalize(baselineRetrievalSource);
		this.candidateRetrievalSource = normalize(candidateRetrievalSource);
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public boolean isExpectedDocument() {
		return expectedDocument;
	}

	public Integer getBaselineRank() {
		return baselineRank;
	}

	public Integer getCandidateRank() {
		return candidateRank;
	}

	public Integer getRankDelta() {
		return rankDelta;
	}

	public boolean isAdded() {
		return added;
	}

	public boolean isRemoved() {
		return removed;
	}

	public boolean isGraphCandidate() {
		return graphCandidate;
	}

	public boolean isNewGraphCandidate() {
		return newGraphCandidate;
	}

	public String getBaselineRetrievalSource() {
		return baselineRetrievalSource;
	}

	public String getCandidateRetrievalSource() {
		return candidateRetrievalSource;
	}

	public boolean isRankImproved() {
		return rankDelta != null && rankDelta < 0;
	}

	public boolean isRankRegressed() {
		return rankDelta != null && rankDelta > 0;
	}

	private static Integer normalizeRank(Integer rank) {
		if (rank == null) return null;
		if (rank <= 0) throw new IllegalArgumentException("rank must be greater than zero");
		return rank;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}
}