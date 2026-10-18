/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.runner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import kr.co.goms.ai.project.CurrentProjectProvider;
import kr.co.goms.ai.project.EpubProjectContext;
import kr.co.goms.ai.rag.eval.RagEvaluationCase;
import kr.co.goms.ai.rag.eval.dataset.RagEvaluationDataset;
import kr.co.goms.ai.rag.eval.mapper.RagRetrievalResultMapper;
import kr.co.goms.ai.rag.eval.model.RagEvaluationRetrievedDocument;
import kr.co.goms.ai.rag.eval.model.RagRetrievalEvaluationResult;
import kr.co.goms.ai.rag.eval.model.RagRetrievalResult;
import kr.co.goms.ai.rag.eval.report.RagRetrievalEvaluationReport;
import kr.co.goms.ai.rag.eval.retrieval.RagRetrievalEvaluator;
import kr.co.goms.ai.rag.index.ProjectIndexResult;
import kr.co.goms.ai.rag.index.ProjectRagIndexer;
import kr.co.goms.ai.rag.retrieval.RetrievalException;
import kr.co.goms.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;
import kr.co.goms.ai.rag.retrieval.Retriever;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

public final class RagRetrievalEvaluationRunner {

	private static final int DEFAULT_TOP_K = 5;

	private final CurrentProjectProvider projectProvider;
	private final ProjectRagIndexer projectRagIndexer;
	private final Retriever retriever;
	private final RagRetrievalResultMapper retrievalResultMapper;
	private final RagRetrievalEvaluator retrievalEvaluator;

	public RagRetrievalEvaluationRunner(CurrentProjectProvider projectProvider, ProjectRagIndexer projectRagIndexer, Retriever retriever, RagRetrievalResultMapper retrievalResultMapper, RagRetrievalEvaluator retrievalEvaluator) {
		this.projectProvider = Objects.requireNonNull(projectProvider, "projectProvider must not be null");
		this.projectRagIndexer = Objects.requireNonNull(projectRagIndexer, "projectRagIndexer must not be null");
		this.retriever = Objects.requireNonNull(retriever, "retriever must not be null");
		this.retrievalResultMapper = Objects.requireNonNull(retrievalResultMapper, "retrievalResultMapper must not be null");
		this.retrievalEvaluator = Objects.requireNonNull(retrievalEvaluator, "retrievalEvaluator must not be null");
	}

	public RagRetrievalEvaluationReport run(RagEvaluationDataset dataset) throws RetrievalException {
		return run(dataset, List.of());
	}

	public RagRetrievalEvaluationReport run(RagEvaluationDataset dataset, List<String> caseIds) throws RetrievalException {

		Objects.requireNonNull(dataset, "dataset must not be null");

		EpubProjectContext project = requireCurrentProject();

		validateDatasetProject(dataset, project);

		ProjectIndexResult indexResult = synchronize(project);
		String vectorProjectId = requireText(indexResult.getProjectId(), "projectId");
		List<RagEvaluationCase> evaluationCases = selectCases(dataset.getCases(), caseIds);
		List<RagRetrievalEvaluationReport.Entry> entries = new ArrayList<>();

		System.out.println("[RAG-RETRIEVAL-EVAL] Dataset = " + dataset.getName());
		System.out.println("[RAG-RETRIEVAL-EVAL] Vector Project ID = " + vectorProjectId);
		System.out.println("[RAG-RETRIEVAL-EVAL] Cases = " + evaluationCases.size());

		for (RagEvaluationCase evaluationCase : evaluationCases) entries.add(evaluateCase(vectorProjectId, evaluationCase));

		return new RagRetrievalEvaluationReport(dataset.getName(), dataset.getCases().size(), entries);
	}

	private RagRetrievalEvaluationReport.Entry evaluateCase(String projectId, RagEvaluationCase evaluationCase) throws RetrievalException {

		System.out.println("[RAG-RETRIEVAL-EVAL] Case Start = " + evaluationCase.getId());

		RetrievalRequest request = RetrievalRequest.builder().projectId(projectId).query(evaluationCase.getQuestion()).topK(DEFAULT_TOP_K).build();
		RetrievalResult retrievalResult = retriever.retrieve(request);

		if (retrievalResult == null) throw new IllegalStateException("Retriever returned null result: " + evaluationCase.getId());

		RagRetrievalResult mappedResult = retrievalResultMapper.map(retrievalResult);
		RagRetrievalEvaluationResult evaluationResult = retrievalEvaluator.evaluate(evaluationCase.getExpectedDocuments(), mappedResult);
		List<RagEvaluationRetrievedDocument> retrievedDocuments = createRetrievedDocuments(retrievalResult);

		System.out.println("[RAG-RETRIEVAL-EVAL] Case Complete = " + evaluationCase.getId() + ", hit=" + evaluationResult.isHitAtK() + ", recall=" + evaluationResult.getRecallAtK() + ", mrr=" + evaluationResult.getMrr());

		return new RagRetrievalEvaluationReport.Entry(evaluationCase.getId(), evaluationCase.getQuestion(), evaluationCase.getExpectedDocuments(), retrievedDocuments, evaluationResult);
	}

	private List<RagEvaluationRetrievedDocument> createRetrievedDocuments(RetrievalResult retrievalResult) {

		if (retrievalResult == null || retrievalResult.isEmpty()) return List.of();

		List<RagEvaluationRetrievedDocument> documents = new ArrayList<>();

		for (VectorSearchResult result : retrievalResult.getSearchResults()) {
			if (result != null && result.getChunk() != null) documents.add(RagEvaluationRetrievedDocument.from(result));
		}

		return List.copyOf(documents);
	}

	private List<RagEvaluationCase> selectCases(List<RagEvaluationCase> cases, List<String> caseIds) {

		Set<String> requestedIds = normalizeCaseIds(caseIds);
		List<RagEvaluationCase> selected = new ArrayList<>();
		Set<String> foundIds = new LinkedHashSet<>();

		for (RagEvaluationCase evaluationCase : cases) {

			if (evaluationCase == null || !evaluationCase.hasExpectedDocuments()) continue;
			if (!requestedIds.isEmpty() && !requestedIds.contains(evaluationCase.getId())) continue;

			selected.add(evaluationCase);
			foundIds.add(evaluationCase.getId());
		}

		if (!requestedIds.isEmpty()) {

			Set<String> missingIds = new LinkedHashSet<>(requestedIds);

			missingIds.removeAll(foundIds);

			if (!missingIds.isEmpty()) throw new IllegalArgumentException("RAG retrieval evaluation case IDs not found or not retrieval-evaluable: " + missingIds);
		}

		return List.copyOf(selected);
	}

	private Set<String> normalizeCaseIds(List<String> caseIds) {

		if (caseIds == null || caseIds.isEmpty()) return Set.of();

		Set<String> normalized = new LinkedHashSet<>();

		for (String caseId : caseIds) {
			if (caseId != null && !caseId.isBlank()) normalized.add(caseId.trim());
		}

		return Set.copyOf(normalized);
	}

	private EpubProjectContext requireCurrentProject() {

		EpubProjectContext project = projectProvider.getCurrentProject();

		if (project == null) throw new IllegalStateException("Current EPUB project is not available");

		return project;
	}

	private void validateDatasetProject(RagEvaluationDataset dataset, EpubProjectContext project) {

		String expectedProjectId = requireText(dataset.getProjectId(), "dataset.projectId");
		String currentProjectId = requireText(project.getProjectName(), "projectName");

		if (!expectedProjectId.equals(currentProjectId)) throw new IllegalStateException("[RAG-RETRIEVAL-EVAL] Project mismatch. Dataset Project ID = " + expectedProjectId + " Current Project ID = " + currentProjectId);
	}

	private ProjectIndexResult synchronize(EpubProjectContext project) {

		try {

			ProjectIndexResult result = projectRagIndexer.synchronize(project);

			if (result == null) throw new IllegalStateException("Project RAG index synchronization returned no result");

			return result;

		} catch (Exception exception) {

			throw new IllegalStateException("Failed to synchronize project RAG index", exception);
		}
	}

	private static String requireText(String value, String fieldName) {

		if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");

		return value.trim();
	}
}