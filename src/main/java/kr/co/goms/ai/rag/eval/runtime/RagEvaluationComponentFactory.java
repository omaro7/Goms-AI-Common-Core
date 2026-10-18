/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.llm.LlmClient;
import kr.co.goms.ai.project.CurrentProjectProvider;
import kr.co.goms.ai.rag.eval.DefaultRagEvaluator;
import kr.co.goms.ai.rag.eval.RagEvaluator;
import kr.co.goms.ai.rag.eval.dataset.RagEvaluationDatasetLoader;
import kr.co.goms.ai.rag.eval.judge.LlmRagJudge;
import kr.co.goms.ai.rag.eval.judge.RagJudge;
import kr.co.goms.ai.rag.eval.mapper.RagRetrievalResultMapper;
import kr.co.goms.ai.rag.eval.metric.AnswerRelevancyMetric;
import kr.co.goms.ai.rag.eval.metric.ContextPrecisionMetric;
import kr.co.goms.ai.rag.eval.metric.ContextRecallMetric;
import kr.co.goms.ai.rag.eval.metric.FaithfulnessMetric;
import kr.co.goms.ai.rag.eval.metric.NoAnswerDetectionMetric;
import kr.co.goms.ai.rag.eval.metric.RagMetric;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationBaselineLoader;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationRegressionChecker;
import kr.co.goms.ai.rag.eval.regression.RagEvaluationRegressionWriter;
import kr.co.goms.ai.rag.eval.report.RagEvaluationReportWriter;
import kr.co.goms.ai.rag.eval.retrieval.DefaultRagRetrievalEvaluator;
import kr.co.goms.ai.rag.eval.retrieval.RagRetrievalEvaluator;
import kr.co.goms.ai.rag.eval.runner.DefaultRagExecutionAdapter;
import kr.co.goms.ai.rag.eval.runner.RagEvaluationRunner;
import kr.co.goms.ai.rag.eval.runner.RagExecutionAdapter;
import kr.co.goms.ai.rag.expansion.ContextExpander;
import kr.co.goms.ai.rag.index.ProjectRagIndexer;
import kr.co.goms.ai.rag.retrieval.Retriever;

/**
 * RAG Evaluation 계층의 기본 Component Factory.
 * 
 * RagJudge judge = ...;
 * RagExecutionAdapter executionAdapter = ...;
 * 
 * RagEvaluationRuntime runtime = RagEvaluationComponentFactory.createRuntime(
 *                 projectProvider,
 *                 projectRagIndexer,
 *                 retriever,
 *                 contextExpander,
 *                 llmClient,
 *                 "gemma4:31b-cloud");

	RagEvaluationComponentFactory
	        │
	        ├─ DefaultRagEvaluator
	        │
	        ├─ DefaultRagRetrievalEvaluator
	        │
	        └─ DefaultRagExecutionAdapter
	                 │
	                 ├─ Retriever
	                 └─ RagRetrievalResultMapper
	        │
	        ▼
	RagEvaluationRunner
	        ├─ 기존 Answer 평가
	        └─ Retrieval 평가
	             ├─ Hit@K
	             ├─ Recall@K
	             └─ MRR
	             
 */
public final class RagEvaluationComponentFactory {

	private RagEvaluationComponentFactory() {
	}

	public static RagEvaluationRuntime createRuntime(CurrentProjectProvider projectProvider, ProjectRagIndexer projectRagIndexer, Retriever retriever, ContextExpander contextExpander, LlmClient llmClient, String model) {
		Objects.requireNonNull(projectProvider, "projectProvider must not be null");
		Objects.requireNonNull(projectRagIndexer, "projectRagIndexer must not be null");
		Objects.requireNonNull(retriever, "retriever must not be null");
		Objects.requireNonNull(contextExpander, "contextExpander must not be null");
		Objects.requireNonNull(llmClient, "llmClient must not be null");

		RagJudge judge = createJudge(llmClient, model);
		RagEvaluator evaluator = createEvaluator(judge);
		RagRetrievalEvaluator retrievalEvaluator = new DefaultRagRetrievalEvaluator();
		RagExecutionAdapter executionAdapter = createExecutionAdapter(projectProvider, projectRagIndexer, retriever, contextExpander, llmClient, model);
		RagEvaluationRunner runner = new RagEvaluationRunner(evaluator, retrievalEvaluator, executionAdapter);

		return new RagEvaluationRuntime(runner, new RagEvaluationDatasetLoader(), new RagEvaluationReportWriter(), new RagEvaluationBaselineLoader(), new RagEvaluationRegressionChecker(), new RagEvaluationRegressionWriter());
	}

	public static RagJudge createJudge(LlmClient llmClient, String model) {
		Objects.requireNonNull(llmClient, "llmClient must not be null");
		return new LlmRagJudge(llmClient, model);
	}

	public static RagEvaluator createEvaluator(RagJudge judge) {
		Objects.requireNonNull(judge, "judge must not be null");

		List<RagMetric> metrics = new ArrayList<>();

		metrics.add(new FaithfulnessMetric(judge));
		metrics.add(new AnswerRelevancyMetric(judge));
		metrics.add(new ContextPrecisionMetric(judge));
		metrics.add(new ContextRecallMetric(judge));
		metrics.add(new NoAnswerDetectionMetric(judge));

		return new DefaultRagEvaluator(metrics);
	}

	public static RagExecutionAdapter createExecutionAdapter(CurrentProjectProvider projectProvider, ProjectRagIndexer projectRagIndexer, Retriever retriever, ContextExpander contextExpander, LlmClient llmClient, String model) {
		Objects.requireNonNull(projectProvider, "projectProvider must not be null");
		Objects.requireNonNull(projectRagIndexer, "projectRagIndexer must not be null");
		Objects.requireNonNull(retriever, "retriever must not be null");
		Objects.requireNonNull(contextExpander, "contextExpander must not be null");
		Objects.requireNonNull(llmClient, "llmClient must not be null");

		RagRetrievalResultMapper retrievalResultMapper = new RagRetrievalResultMapper();

		return new DefaultRagExecutionAdapter(projectProvider, projectRagIndexer, retriever, retrievalResultMapper, contextExpander, llmClient, model);
	}
}