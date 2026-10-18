/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.project.CurrentProjectProvider;
import kr.co.goms.ai.rag.eval.dataset.RagEvaluationDataset;
import kr.co.goms.ai.rag.eval.dataset.RagEvaluationDatasetLoader;
import kr.co.goms.ai.rag.eval.path.RagEvaluationPathResolver;
import kr.co.goms.ai.rag.eval.report.RagRetrievalEvaluationReport;
import kr.co.goms.ai.rag.eval.runner.RagRetrievalEvaluationRunner;
import kr.co.goms.ai.rag.retrieval.RetrievalException;

public final class DefaultRagRetrievalEvaluationService implements RagRetrievalEvaluationService {

	private final CurrentProjectProvider projectProvider;
	private final RagEvaluationPathResolver pathResolver;
	private final RagEvaluationDatasetLoader datasetLoader;
	private final RagRetrievalEvaluationRunner runner;

	public DefaultRagRetrievalEvaluationService(CurrentProjectProvider projectProvider, RagEvaluationPathResolver pathResolver, RagEvaluationDatasetLoader datasetLoader, RagRetrievalEvaluationRunner runner) {
		this.projectProvider = Objects.requireNonNull(projectProvider, "projectProvider must not be null");
		this.pathResolver = Objects.requireNonNull(pathResolver, "pathResolver must not be null");
		this.datasetLoader = Objects.requireNonNull(datasetLoader, "datasetLoader must not be null");
		this.runner = Objects.requireNonNull(runner, "runner must not be null");
	}

	@Override
	public RagRetrievalEvaluationReport evaluate() throws IOException {
		return evaluate(List.of());
	}

	@Override
	public RagRetrievalEvaluationReport evaluate(List<String> caseIds) throws IOException {

		String projectId = requireProjectId();
		Path datasetPath = pathResolver.resolveGoldenDataset(projectId);

		if (!Files.isRegularFile(datasetPath)) throw new IllegalStateException("RAG Golden Dataset not found: " + datasetPath);

		RagEvaluationDataset dataset = datasetLoader.load(datasetPath);

		try {
			return runner.run(dataset, caseIds);
		} catch (RetrievalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String requireProjectId() {

		if (projectProvider.getCurrentProject() == null) throw new IllegalStateException("Current EPUB project is not available");

		String projectId = projectProvider.getCurrentProject().getProjectId();

		if (projectId == null || projectId.isBlank()) throw new IllegalStateException("Current projectId is not available");

		return projectId.trim();
	}
}