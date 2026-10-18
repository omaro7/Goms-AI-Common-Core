/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.retrieval;

import java.util.List;

import kr.co.goms.ai.rag.eval.model.RagRetrievalEvaluationResult;
import kr.co.goms.ai.rag.eval.model.RagRetrievalResult;

/**
 * RAG Retrieval 평가기.
 RagRetrievalEvaluator
        │
        └─ evaluate(
             expectedDocuments,
             retrievalResult
           )
                │
                ▼
       RagRetrievalEvaluationResult
        ├─ hitAtK
        ├─ recallAtK
        └─ mrr
 */
public interface RagRetrievalEvaluator {

	RagRetrievalEvaluationResult evaluate(List<String> expectedDocuments, RagRetrievalResult retrievalResult);
}