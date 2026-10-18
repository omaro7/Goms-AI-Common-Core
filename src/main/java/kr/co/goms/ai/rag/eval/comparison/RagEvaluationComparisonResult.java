/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.comparison;

import java.nio.file.Path;
import java.util.Objects;

public final class RagEvaluationComparisonResult {

	private final String projectId;
	private final String baselineExperimentId;
	private final String candidateExperimentId;
	private final Path baselineReportPath;
	private final Path candidateReportPath;
	private final Path comparisonPath;
	private final RagEvaluationComparison comparison;

	public RagEvaluationComparisonResult(String projectId, String baselineExperimentId, String candidateExperimentId, Path baselineReportPath, Path candidateReportPath, Path comparisonPath, RagEvaluationComparison comparison) {
		this.projectId = requireText(projectId, "projectId");
		this.baselineExperimentId = requireText(baselineExperimentId, "baselineExperimentId");
		this.candidateExperimentId = requireText(candidateExperimentId, "candidateExperimentId");
		this.baselineReportPath = Objects.requireNonNull(baselineReportPath, "baselineReportPath must not be null");
		this.candidateReportPath = Objects.requireNonNull(candidateReportPath, "candidateReportPath must not be null");
		this.comparisonPath = Objects.requireNonNull(comparisonPath, "comparisonPath must not be null");
		this.comparison = Objects.requireNonNull(comparison, "comparison must not be null");
	}

	public String getProjectId() {
		return projectId;
	}

	public String getBaselineExperimentId() {
		return baselineExperimentId;
	}

	public String getCandidateExperimentId() {
		return candidateExperimentId;
	}

	public Path getBaselineReportPath() {
		return baselineReportPath;
	}

	public Path getCandidateReportPath() {
		return candidateReportPath;
	}

	public Path getComparisonPath() {
		return comparisonPath;
	}

	public RagEvaluationComparison getComparison() {
		return comparison;
	}

	private static String requireText(String value, String fieldName) {

		if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");

		return value.trim();
	}
}