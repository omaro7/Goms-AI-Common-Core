/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.model;

/**
 * Dataset 단위 Retrieval 평가 집계 결과.
 */
public final class RagRetrievalSummary {

	private final int evaluatedCases;
	private final int hitCount;
	private final double hitRateAtK;
	private final double averageRecallAtK;
	private final double meanReciprocalRank;

	public RagRetrievalSummary(int evaluatedCases, int hitCount, double hitRateAtK, double averageRecallAtK, double meanReciprocalRank) {
		if (evaluatedCases < 0) throw new IllegalArgumentException("evaluatedCases must not be negative");
		if (hitCount < 0 || hitCount > evaluatedCases) throw new IllegalArgumentException("hitCount must be between 0 and evaluatedCases");
		this.evaluatedCases = evaluatedCases;
		this.hitCount = hitCount;
		this.hitRateAtK = validateScore(hitRateAtK, "hitRateAtK");
		this.averageRecallAtK = validateScore(averageRecallAtK, "averageRecallAtK");
		this.meanReciprocalRank = validateScore(meanReciprocalRank, "meanReciprocalRank");
	}

	public static RagRetrievalSummary empty() {
		return new RagRetrievalSummary(0, 0, 0.0, 0.0, 0.0);
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

	private static double validateScore(double value, String fieldName) {
		if (Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException(fieldName + " must be a finite value");
		if (value < 0.0 || value > 1.0) throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
		return value;
	}

	@Override
	public String toString() {
		return "RagRetrievalSummary{evaluatedCases=" + evaluatedCases + ", hitCount=" + hitCount + ", hitRateAtK=" + hitRateAtK + ", averageRecallAtK=" + averageRecallAtK + ", meanReciprocalRank=" + meanReciprocalRank + "}";
	}
}