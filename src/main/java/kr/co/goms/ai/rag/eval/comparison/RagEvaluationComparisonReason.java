/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.comparison;

public enum RagEvaluationComparisonReason {

	HIT_TO_MISS_WITH_NEW_GRAPH_CANDIDATES,

	HIT_TO_MISS,

	MISS_TO_HIT_WITH_GRAPH_CANDIDATES,

	MISS_TO_HIT,

	EXPECTED_RANK_REGRESSED,

	EXPECTED_RANK_IMPROVED,

	EXPECTED_RANK_UNCHANGED,

	STABLE_MISS,

	NOT_APPLICABLE
}