/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.report;

/**
 * RAG Answer Evaluation 집계 결과.
 *
 * Retrieval Evaluation과 Answer Evaluation을 분리하기 위해
 * LLM 기반 Answer Score 집계를 독립적으로 관리한다.
 *
 * Primary:
 * - Retrieval Evaluation
 *
 * Secondary:
 * - Answer Evaluation
 */
public final class RagEvaluationAnswerSummary {

	private final int evaluatedCases;
	private final double averageScore;

	public RagEvaluationAnswerSummary(int evaluatedCases, double averageScore) {
		this.evaluatedCases = requireEvaluatedCases(evaluatedCases);
		this.averageScore = requireScore(averageScore, "averageScore");
	}

	public static RagEvaluationAnswerSummary empty() {
		return new RagEvaluationAnswerSummary(0, 0.0);
	}

	public int getEvaluatedCases() {
		return evaluatedCases;
	}

	public double getAverageScore() {
		return averageScore;
	}

	public boolean isEmpty() {
		return evaluatedCases == 0;
	}

	private static int requireEvaluatedCases(int evaluatedCases) {
		if (evaluatedCases < 0) throw new IllegalArgumentException("evaluatedCases must not be negative");
		return evaluatedCases;
	}

	private static double requireScore(double score, String fieldName) {
		if (!Double.isFinite(score)) throw new IllegalArgumentException(fieldName + " must be finite");
		if (score < 0.0 || score > 1.0) throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
		return score;
	}

	@Override
	public String toString() {
		return "RagEvaluationAnswerSummary{evaluatedCases=" + evaluatedCases + ", averageScore=" + averageScore + '}';
	}
}