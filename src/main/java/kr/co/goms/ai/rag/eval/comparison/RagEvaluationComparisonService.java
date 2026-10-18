/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.comparison;

import java.io.IOException;

public interface RagEvaluationComparisonService {

	RagEvaluationComparisonResult compare(String baselineExperimentId, String candidateExperimentId) throws IOException;
}