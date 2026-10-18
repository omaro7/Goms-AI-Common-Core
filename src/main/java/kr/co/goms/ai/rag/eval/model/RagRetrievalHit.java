/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.model;

import java.util.Objects;

public final class RagRetrievalHit {

	private final String source;
	private final int rank;
	private final double score;

	public RagRetrievalHit(String source, int rank, double score) {
		this.source = Objects.requireNonNull(source, "source must not be null");
		if (rank <= 0) throw new IllegalArgumentException("rank must be greater than 0");
		this.rank = rank;
		this.score = score;
	}

	public String getSource() {
		return source;
	}

	public int getRank() {
		return rank;
	}

	public double getScore() {
		return score;
	}

	@Override
	public String toString() {
		return "RagRetrievalHit{source='" + source + "', rank=" + rank + ", score=" + score + "}";
	}
}