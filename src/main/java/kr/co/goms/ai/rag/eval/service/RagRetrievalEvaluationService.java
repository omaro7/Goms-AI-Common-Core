/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.service;

import java.io.IOException;
import java.util.List;

import kr.co.goms.ai.rag.eval.report.RagRetrievalEvaluationReport;

public interface RagRetrievalEvaluationService {

	RagRetrievalEvaluationReport evaluate() throws IOException;

	RagRetrievalEvaluationReport evaluate(List<String> caseIds) throws IOException;
}