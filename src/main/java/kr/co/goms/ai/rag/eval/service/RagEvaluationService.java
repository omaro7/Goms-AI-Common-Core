/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.service;

import java.io.IOException;

import kr.co.goms.ai.rag.eval.report.RagEvaluationReport;

/**
 * RAG Evaluation Service.
 */
public interface RagEvaluationService {

	RagEvaluationReport evaluateGolden() throws IOException;

	RagEvaluationReport evaluateGolden(int version) throws IOException;

	RagEvaluationReport evaluateLatestGolden() throws IOException;
}