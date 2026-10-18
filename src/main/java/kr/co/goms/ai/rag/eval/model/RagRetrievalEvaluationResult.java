/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.model;

/**
 * RAG Retrieval 평가 결과.
	RagRetrievalEvaluationResult
 	├─ applicable
 	├─ hitAtK
 	├─ recallAtK
 	└─ mrr 
 */
public final class RagRetrievalEvaluationResult {

	private final boolean applicable;
	private final boolean hitAtK;
	private final double recallAtK;
	private final double mrr;

	public RagRetrievalEvaluationResult(boolean applicable, boolean hitAtK, double recallAtK, double mrr) {
		this.applicable = applicable;
		this.hitAtK = hitAtK;
		this.recallAtK = validateScore(recallAtK, "recallAtK");
		this.mrr = validateScore(mrr, "mrr");
	}

	public static RagRetrievalEvaluationResult of(boolean hitAtK, double recallAtK, double mrr) {
		return new RagRetrievalEvaluationResult(true, hitAtK, recallAtK, mrr);
	}

	public static RagRetrievalEvaluationResult notApplicable() {
		return new RagRetrievalEvaluationResult(false, false, 0.0, 0.0);
	}

	public boolean isApplicable() {
		return applicable;
	}

	public boolean isHitAtK() {
		return hitAtK;
	}

	public double getRecallAtK() {
		return recallAtK;
	}

	public double getMrr() {
		return mrr;
	}

	private static double validateScore(double value, String fieldName) {
		if (Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException(fieldName + " must be a finite value");
		if (value < 0.0 || value > 1.0) throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
		return value;
	}

	@Override
	public String toString() {
		return "RagRetrievalEvaluationResult{applicable=" + applicable + ", hitAtK=" + hitAtK + ", recallAtK=" + recallAtK + ", mrr=" + mrr + "}";
	}
}