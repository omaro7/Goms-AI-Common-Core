/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.llm.LlmClient;
import kr.co.goms.ai.llm.LlmRequest;
import kr.co.goms.ai.llm.LlmResponse;
import kr.co.goms.ai.project.CurrentProjectProvider;
import kr.co.goms.ai.project.EpubProjectContext;
import kr.co.goms.ai.rag.eval.mapper.RagRetrievalResultMapper;
import kr.co.goms.ai.rag.eval.model.RagRetrievalResult;
import kr.co.goms.ai.rag.expansion.ContextExpander;
import kr.co.goms.ai.rag.expansion.ContextExpansionRequest;
import kr.co.goms.ai.rag.index.ProjectIndexException;
import kr.co.goms.ai.rag.index.ProjectIndexResult;
import kr.co.goms.ai.rag.index.ProjectRagIndexer;
import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.retrieval.RetrievalException;
import kr.co.goms.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;
import kr.co.goms.ai.rag.retrieval.RetrievedDocument;
import kr.co.goms.ai.rag.retrieval.Retriever;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * GomsBook RAG Core와 Evaluation Runner를 연결하는 기본 Adapter.
 */
public final class DefaultRagExecutionAdapter implements RagExecutionAdapter {

	private static final int DEFAULT_TOP_K = 5;
	private static final double DEFAULT_TEMPERATURE = 0.0;
	private static final int DEFAULT_MAX_TOKENS = 1024;

	private final CurrentProjectProvider projectProvider;
	private final ProjectRagIndexer projectRagIndexer;
	private final Retriever retriever;
	private final RagRetrievalResultMapper retrievalResultMapper;
	private final ContextExpander contextExpander;
	private final LlmClient llmClient;
	private final String model;

	public DefaultRagExecutionAdapter(CurrentProjectProvider projectProvider, ProjectRagIndexer projectRagIndexer, Retriever retriever, RagRetrievalResultMapper retrievalResultMapper, ContextExpander contextExpander, LlmClient llmClient, String model) {
		this.projectProvider = Objects.requireNonNull(projectProvider, "projectProvider must not be null");
		this.projectRagIndexer = Objects.requireNonNull(projectRagIndexer, "projectRagIndexer must not be null");
		this.retriever = Objects.requireNonNull(retriever, "retriever must not be null");
		this.retrievalResultMapper = Objects.requireNonNull(retrievalResultMapper, "retrievalResultMapper must not be null");
		this.contextExpander = Objects.requireNonNull(contextExpander, "contextExpander must not be null");
		this.llmClient = Objects.requireNonNull(llmClient, "llmClient must not be null");
		this.model = normalizeOptional(model);
	}

	@Override
	public RagExecutionResult execute(String question) {
		String normalizedQuestion = requireText(question, "question");
		EpubProjectContext project = projectProvider.getCurrentProject();

		if (project == null) throw new IllegalStateException("Current EPUB project is not available");

		System.out.println("[RAG-EVAL] Current Project Root = " + project.getProjectRoot());

		try {
			ProjectIndexResult indexResult = projectRagIndexer.synchronize(project);

			if (indexResult == null) throw new IllegalStateException("Project RAG index result must not be null");

			String projectId = requireText(indexResult.getProjectId(), "projectId");

			System.out.println("[RAG-EVAL] Current Project ID   = " + projectId);

			RetrievalRequest request = RetrievalRequest.builder().projectId(projectId).query(normalizedQuestion).topK(DEFAULT_TOP_K).build();
			RetrievalResult retrievalResult = retriever.retrieve(request);

			if (retrievalResult == null) throw new IllegalStateException("Retriever returned null result");

			RagRetrievalResult ragRetrievalResult = retrievalResultMapper.map(retrievalResult);

			List<RetrievedDocument> retrievedDocuments = toRetrievedDocuments(projectId, retrievalResult);
			ContextExpansionRequest expansionRequest = new ContextExpansionRequest(projectId, retrievedDocuments, 1, 1);
			List<RetrievedDocument> expandedDocuments = contextExpander.expand(expansionRequest);
			List<String> retrievedContexts = extractContexts(expandedDocuments);
			String answer = generateAnswer(normalizedQuestion, retrievedContexts);

			return new RagExecutionResult(retrievedContexts, answer, ragRetrievalResult, retrievalResult);

		} catch (ProjectIndexException e) {
			throw new IllegalStateException("Failed to synchronize project RAG index: " + e.getMessage(), e);

		} catch (RetrievalException e) {
			throw new IllegalStateException("Failed to retrieve RAG contexts: " + e.getMessage(), e);
		}
	}

	@Override
	public void validateProject(String expectedProjectId) {

		EpubProjectContext project = projectProvider.getCurrentProject();

		if (project == null) throw new IllegalStateException("Current EPUB project is not available");

		String datasetProjectId = requireText(expectedProjectId, "expectedProjectId");
		String currentProjectId = requireText(project.getProjectName(), "projectName");

		System.out.println("[RAG-EVAL] Dataset Project ID = " + datasetProjectId);
		System.out.println("[RAG-EVAL] Current Project ID = " + currentProjectId);

		if (!datasetProjectId.equals(currentProjectId)) throw new IllegalStateException("[RAG-EVAL] Project mismatch. Dataset Project ID = " + datasetProjectId + " Current Project ID = " + currentProjectId);
	}

	private String generateAnswer(String question, List<String> contexts) {
		LlmRequest.Builder builder = LlmRequest.builder().systemMessage(buildSystemPrompt()).userMessage(buildUserPrompt(question, contexts)).temperature(DEFAULT_TEMPERATURE).maxTokens(DEFAULT_MAX_TOKENS).stream(false);

		if (model != null) builder.model(model);

		llmClient.requireAvailable();

		LlmResponse response = llmClient.chat(builder.build());

		if (response == null) throw new IllegalStateException("LLM returned null response");
		if (!response.hasContent()) throw new IllegalStateException("LLM returned empty response");

		return requireText(response.getContent(), "answer");
	}

	private String buildSystemPrompt() {
		return "You are a RAG question answering assistant.\n"
				+ "Answer the user's question using only the retrieved project contexts.\n"
				+ "Do not use external knowledge.\n"
				+ "If the contexts do not contain enough information, clearly say so.";
	}

	private String buildUserPrompt(String question, List<String> contexts) {
		StringBuilder builder = new StringBuilder();

		builder.append("[RETRIEVED CONTEXTS]\n\n");

		for (int i = 0; i < contexts.size(); i++) {
			builder.append("[Context ");
			builder.append(i + 1);
			builder.append("]\n");
			builder.append(contexts.get(i));
			builder.append("\n\n");
		}

		builder.append("[QUESTION]\n");
		builder.append(question);

		return builder.toString();
	}

	private static String requireText(String value, String fieldName) {
		if (value == null) throw new NullPointerException(fieldName + " must not be null");

		String normalized = value.trim();

		if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");

		return normalized;
	}

	private static String normalizeOptional(String value) {
		if (value == null) return null;

		String normalized = value.trim();

		return normalized.isEmpty() ? null : normalized;
	}

	private List<RetrievedDocument> toRetrievedDocuments(String projectId, RetrievalResult retrievalResult) {
		if (retrievalResult == null || retrievalResult.isEmpty()) return List.of();

		List<RetrievedDocument> documents = new ArrayList<>();

		for (VectorSearchResult result : retrievalResult.getSearchResults()) {
			if (result == null || result.getChunk() == null) continue;

			DocumentChunk chunk = result.getChunk();
			RetrievedDocument document = new RetrievedDocument(projectId, chunk.getId(), chunk.getSourcePath(), chunk.getSequence(), chunk.getTitle(), chunk.getContent(), result.getScore());

			documents.add(document);
		}

		return List.copyOf(documents);
	}

	private List<String> extractContexts(List<RetrievedDocument> documents) {
		if (documents == null || documents.isEmpty()) return Collections.emptyList();

		List<String> contexts = new ArrayList<>();

		for (RetrievedDocument document : documents) {
			if (document == null) continue;

			String text = normalizeOptional(document.getText());

			if (text != null) contexts.add(text);
		}

		return Collections.unmodifiableList(contexts);
	}
}