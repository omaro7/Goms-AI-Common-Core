/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.rag.eval.RagEvaluationCase;
import kr.co.goms.ai.rag.eval.RagEvaluationContext;
import kr.co.goms.ai.rag.eval.RagEvaluationResult;
import kr.co.goms.ai.rag.eval.RagEvaluator;
import kr.co.goms.ai.rag.eval.dataset.RagEvaluationDataset;
import kr.co.goms.ai.rag.eval.model.RagRetrievalEvaluationResult;
import kr.co.goms.ai.rag.eval.report.RagEvaluationReport;
import kr.co.goms.ai.rag.eval.retrieval.RagRetrievalEvaluator;
import kr.co.goms.ai.rag.eval.model.RagEvaluationRetrievedDocument;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * Golden Dataset 전체를 대상으로 RAG 평가를 실행한다.
 * 기존 모델
 * RagEvaluationDataset
 *         │
 *         ▼
 * RagEvaluationRunner
 *         │
 *         ├─ Case 1
 *         │     ↓
 *         │  RagExecutionAdapter
 *         │     ↓
 *         │  Retriever + RAG Answer
 *         │     ↓
 *         │  RagEvaluationContext
 *         │     ↓
 *         │  RagEvaluator
 *         │
 *         ├─ Case 2
 *         │     ↓
 *         │    ...
 *         │
 *         └─ Case N
 *               ↓
 *       RagEvaluationReport    
 *       
 * TO-BE
 * RagEvaluationDataset
 *         │
 *         ▼
 * RagEvaluationRunner 
 *         │ 
       RagEvaluationCase
		 ├─ question
		 ├─ referenceAnswer
		 └─ expectedDocuments
		          │
		          │
		          ▼
		RagExecutionAdapter
		          │
		          ├─ answer
		          ├─ retrievedContexts
		          └─ retrievalResult
		                    │
		        ┌───────────┴───────────┐
		        ▼                       ▼
		 RagEvaluator          RagRetrievalEvaluator
		        │                       │
		        ▼                       ├─ Hit@K
		RagEvaluationResult             ├─ Recall@K
		                                └─ MRR
		                                  │
		                                  ▼
		                    RagRetrievalEvaluationResult
                    
 */
/**
 * Golden Dataset 전체를 대상으로 RAG 평가를 실행한다.
 */
public final class RagEvaluationRunner {

	private final RagEvaluator evaluator;
	private final RagRetrievalEvaluator retrievalEvaluator;
	private final RagExecutionAdapter executionAdapter;

	public RagEvaluationRunner(RagEvaluator evaluator, RagRetrievalEvaluator retrievalEvaluator, RagExecutionAdapter executionAdapter) {
		this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
		this.retrievalEvaluator = Objects.requireNonNull(retrievalEvaluator, "retrievalEvaluator must not be null");
		this.executionAdapter = Objects.requireNonNull(executionAdapter, "executionAdapter must not be null");
	}

	public RagEvaluationReport run(RagEvaluationDataset dataset) {
		Objects.requireNonNull(dataset, "dataset must not be null");

		executionAdapter.validateProject(dataset.getProjectId());

		List<RagEvaluationReport.Entry> entries = new ArrayList<>();

		for (RagEvaluationCase evaluationCase : dataset.getCases()) {
			entries.add(evaluateCase(evaluationCase));
		}

		return new RagEvaluationReport(dataset.getName(), entries);
	}

	private RagEvaluationReport.Entry evaluateCase(RagEvaluationCase evaluationCase) {

		Objects.requireNonNull(evaluationCase, "evaluationCase must not be null");

		RagExecutionResult executionResult = executionAdapter.execute(evaluationCase.getQuestion());

		if (executionResult == null) throw new IllegalStateException("RAG execution result must not be null: " + evaluationCase.getId());

		RagEvaluationResult evaluationResult = evaluateAnswer(evaluationCase, executionResult);
		RagRetrievalEvaluationResult retrievalEvaluationResult = evaluateRetrieval(evaluationCase, executionResult);
		List<RagEvaluationRetrievedDocument> retrievedDocuments = createRetrievedDocuments(executionResult);

		return new RagEvaluationReport.Entry(evaluationCase.getId(), evaluationCase.getQuestion(), evaluationResult, retrievalEvaluationResult, evaluationCase.getExpectedDocuments(), retrievedDocuments);
	}
	
	private List<RagEvaluationRetrievedDocument> createRetrievedDocuments(RagExecutionResult executionResult) {

		if (executionResult == null || !executionResult.hasRawRetrievalResult()) return List.of();

		RetrievalResult retrievalResult = executionResult.getRawRetrievalResult();

		if (retrievalResult == null || retrievalResult.isEmpty()) return List.of();

		List<RagEvaluationRetrievedDocument> retrievedDocuments = new ArrayList<>();

		for (VectorSearchResult searchResult : retrievalResult.getSearchResults()) {

			if (searchResult == null || searchResult.getChunk() == null) continue;

			retrievedDocuments.add(RagEvaluationRetrievedDocument.from(searchResult));
		}

		return List.copyOf(retrievedDocuments);
	}
	
	private RagEvaluationResult evaluateAnswer(RagEvaluationCase evaluationCase, RagExecutionResult executionResult) {
		RagEvaluationContext context = new RagEvaluationContext(evaluationCase.getType(), evaluationCase.getQuestion(), executionResult.getRetrievedContexts(), executionResult.getAnswer(), evaluationCase.getReferenceAnswer());
		return evaluator.evaluate(context);
	}

	private RagRetrievalEvaluationResult evaluateRetrieval(RagEvaluationCase evaluationCase, RagExecutionResult executionResult) {
		if (!executionResult.hasRetrievalResult()) return RagRetrievalEvaluationResult.notApplicable();
		return retrievalEvaluator.evaluate(evaluationCase.getExpectedDocuments(), executionResult.getRetrievalResult());
	}
}