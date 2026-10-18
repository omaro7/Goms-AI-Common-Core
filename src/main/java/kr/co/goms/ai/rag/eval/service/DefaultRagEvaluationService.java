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
import java.util.Objects;

import kr.co.goms.ai.project.CurrentProjectProvider;
import kr.co.goms.ai.project.EpubProjectContext;
import kr.co.goms.ai.rag.eval.path.RagEvaluationPathResolver;
import kr.co.goms.ai.rag.eval.profile.RagEvaluationProfile;
import kr.co.goms.ai.rag.eval.report.RagEvaluationReport;
import kr.co.goms.ai.rag.eval.runtime.RagEvaluationRuntime;
import kr.co.goms.ai.rag.util.RagUtil;

/**
 * RAG Evaluation 기본 Service.
 */
public final class DefaultRagEvaluationService implements RagEvaluationService {

	private static final int DEFAULT_GOLDEN_VERSION = 1;

	private final CurrentProjectProvider projectProvider;
	private final RagEvaluationPathResolver pathResolver;
	private final RagEvaluationRuntime runtime;
	private final RagEvaluationProfile evaluationProfile;
	
	public DefaultRagEvaluationService(CurrentProjectProvider projectProvider, RagEvaluationPathResolver pathResolver, RagEvaluationRuntime runtime, RagEvaluationProfile evaluationProfile) {
		this.projectProvider = Objects.requireNonNull(projectProvider, "projectProvider must not be null");
		this.pathResolver = Objects.requireNonNull(pathResolver, "pathResolver must not be null");
		this.runtime = Objects.requireNonNull(runtime, "runtime must not be null");
		this.evaluationProfile = Objects.requireNonNull(evaluationProfile, "evaluationProfile must not be null");
	}
	
	@Override
	public RagEvaluationReport evaluateGolden() throws IOException {
		return evaluateGolden(DEFAULT_GOLDEN_VERSION);
	}

	@Override
	public RagEvaluationReport evaluateGolden(int version) throws IOException {
		String projectId = resolveProjectId();
		return evaluateGolden(projectId, version);
	}

	@Override
	public RagEvaluationReport evaluateLatestGolden() throws IOException {
		String projectId = resolveProjectId();
		int version = pathResolver.resolveLatestGoldenVersion(projectId);
		return evaluateGolden(projectId, version);
	}

	private RagEvaluationReport evaluateGolden(String projectId, int version) throws IOException {
		Path datasetPath = pathResolver.resolveGoldenDataset(projectId, version);
		Path reportPath = pathResolver.resolveGoldenReport(projectId, evaluationProfile);
		return evaluate(datasetPath, reportPath);
	}

	private RagEvaluationReport evaluate(Path datasetPath, Path reportPath) throws IOException {
		if (!Files.isRegularFile(datasetPath)) throw new IllegalStateException("RAG Golden Dataset not found: " + datasetPath);

		Path parent = reportPath.getParent();

		if (parent != null) Files.createDirectories(parent);

		return runtime.evaluate(datasetPath, reportPath);
	}

	private String resolveProjectId() {
		EpubProjectContext project = projectProvider.getCurrentProject();

		if (project == null) throw new IllegalStateException("Current EPUB project is not available.");

		String projectId = RagUtil.normalizeOptional(project.getProjectId());

		if (projectId == null) throw new IllegalStateException("Current projectId is not available.");

		return projectId;
	}
}