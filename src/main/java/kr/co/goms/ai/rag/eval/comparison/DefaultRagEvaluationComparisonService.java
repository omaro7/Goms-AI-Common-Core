/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.comparison;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import kr.co.goms.ai.project.CurrentProjectProvider;
import kr.co.goms.ai.project.EpubProjectContext;
import kr.co.goms.ai.rag.eval.path.RagEvaluationPathResolver;
import kr.co.goms.ai.rag.util.RagUtil;

public final class DefaultRagEvaluationComparisonService implements RagEvaluationComparisonService {

	private static final String REPORTS_DIRECTORY = "reports";
	private static final String COMPARISONS_DIRECTORY = "comparisons";

	private final CurrentProjectProvider projectProvider;
	private final RagEvaluationPathResolver pathResolver;
	private final RagEvaluationReportComparator comparator;
	private final RagEvaluationComparisonWriter writer;

	public DefaultRagEvaluationComparisonService(CurrentProjectProvider projectProvider, RagEvaluationPathResolver pathResolver, RagEvaluationReportComparator comparator, RagEvaluationComparisonWriter writer) {
		this.projectProvider = Objects.requireNonNull(projectProvider, "projectProvider must not be null");
		this.pathResolver = Objects.requireNonNull(pathResolver, "pathResolver must not be null");
		this.comparator = Objects.requireNonNull(comparator, "comparator must not be null");
		this.writer = Objects.requireNonNull(writer, "writer must not be null");
	}

	@Override
	public RagEvaluationComparisonResult compare(String baselineExperimentId, String candidateExperimentId) throws IOException {

		String projectId = resolveProjectId();
		String normalizedBaselineExperimentId = normalizeExperimentId(baselineExperimentId);
		String normalizedCandidateExperimentId = normalizeExperimentId(candidateExperimentId);

		if (normalizedBaselineExperimentId.equals(normalizedCandidateExperimentId)) throw new IllegalArgumentException("baselineExperimentId and candidateExperimentId must be different.");

		String baselineReportName = toReportName(normalizedBaselineExperimentId);
		String candidateReportName = toReportName(normalizedCandidateExperimentId);
		Path baselineReportPath = resolveReportPath(projectId, baselineReportName);
		Path candidateReportPath = resolveReportPath(projectId, candidateReportName);

		validateReportPath(baselineReportPath, normalizedBaselineExperimentId);
		validateReportPath(candidateReportPath, normalizedCandidateExperimentId);

		RagEvaluationComparison comparison = comparator.compare(baselineReportPath, normalizedBaselineExperimentId, candidateReportPath, normalizedCandidateExperimentId);
		Path comparisonPath = resolveComparisonPath(projectId, baselineReportName, candidateReportName);

		writer.write(comparison, comparisonPath);

		System.out.println("[RAG-COMPARISON] baseline=" + normalizedBaselineExperimentId);
		System.out.println("[RAG-COMPARISON] candidate=" + normalizedCandidateExperimentId);
		System.out.println("[RAG-COMPARISON] output=" + comparisonPath);
		System.out.println("[RAG-COMPARISON] HIT_TO_MISS=" + comparison.getSummary().getHitToMissCount() + ", MISS_TO_HIT=" + comparison.getSummary().getMissToHitCount());

		return new RagEvaluationComparisonResult(projectId, normalizedBaselineExperimentId, normalizedCandidateExperimentId, baselineReportPath, candidateReportPath, comparisonPath, comparison);
	}

	private Path resolveReportPath(String projectId, String reportName) {
		return pathResolver.resolveEvalDirectory(projectId).resolve(REPORTS_DIRECTORY).resolve(reportName).resolve("rag-" + projectId + "-" + reportName + "-report.json").normalize();
	}

	private Path resolveComparisonPath(String projectId, String baselineReportName, String candidateReportName) {

		String comparisonName = baselineReportName + "-vs-" + candidateReportName;

		return pathResolver.resolveEvalDirectory(projectId).resolve(COMPARISONS_DIRECTORY).resolve(comparisonName).resolve("rag-" + projectId + "-" + comparisonName + "-comparison.json").normalize();
	}

	private String resolveProjectId() {

		EpubProjectContext project = projectProvider.getCurrentProject();

		if (project == null) throw new IllegalStateException("Current EPUB project is not available.");

		return RagUtil.requireProjectId(project.getProjectId());
	}

	private String normalizeExperimentId(String experimentId) {

		String normalized = RagUtil.requireText(experimentId, "experimentId").toUpperCase(Locale.ROOT);

		if (!normalized.matches("[A-Z0-9]+(?:_[A-Z0-9]+)*")) throw new IllegalArgumentException("Invalid RAG evaluation experimentId: " + experimentId);

		return normalized;
	}

	private String toReportName(String experimentId) {
		return experimentId.toLowerCase(Locale.ROOT).replace('_', '-');
	}

	private void validateReportPath(Path reportPath, String experimentId) {

		if (!Files.isRegularFile(reportPath)) throw new IllegalStateException("RAG evaluation report not found for " + experimentId + ": " + reportPath);
	}
}