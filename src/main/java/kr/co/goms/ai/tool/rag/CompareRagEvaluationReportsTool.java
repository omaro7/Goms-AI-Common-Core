/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.tool.rag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.rag.eval.comparison.RagEvaluationComparison;
import kr.co.goms.ai.rag.eval.comparison.RagEvaluationComparisonResult;
import kr.co.goms.ai.rag.eval.comparison.RagEvaluationComparisonService;
import kr.co.goms.ai.rag.eval.profile.RagEvaluationProfile;
import kr.co.goms.ai.tool.AgentTool;
import kr.co.goms.ai.tool.ToolContext;
import kr.co.goms.ai.tool.ToolIssue;
import kr.co.goms.ai.tool.ToolIssueSeverity;
import kr.co.goms.ai.tool.ToolRequest;
import kr.co.goms.ai.tool.ToolResult;
import kr.co.goms.ai.tool.ToolStatus;
import kr.co.goms.ai.tool.ToolValidationResult;

/**
 * 저장된 두 RAG Evaluation Report를 비교한다.
 *
 * <p>
 * Retrieval Evaluation은 Primary Metric,
 * Answer Evaluation은 Secondary Metric으로 분리하여 반환한다.
 * </p>
 *
 * <pre>
 * compare_rag_evaluation_reports
 * │
 * ├─ retrievalSummary
 * │   ├─ HIT_TO_MISS
 * │   ├─ MISS_TO_HIT
 * │   ├─ STABLE_HIT
 * │   ├─ STABLE_MISS
 * │   ├─ Rank Improved
 * │   └─ Rank Regressed
 * │
 * ├─ answerSummary
 * │   ├─ baselineAverageScore
 * │   ├─ candidateAverageScore
 * │   └─ averageScoreDelta
 * │
 * ├─ summary
 * │   └─ Legacy compatibility
 * │
 * └─ legacy answer score fields
 * </pre>
 *
 * 이 Tool은 기존 Evaluation Report를 읽어 비교할 뿐,
 * RAG Retrieval 또는 LLM Evaluation을 다시 실행하지 않는다.
 */
public final class CompareRagEvaluationReportsTool implements AgentTool {

	public static final String TOOL_NAME = "compare_rag_evaluation_reports";

	private static final String DESCRIPTION =
			"Compares two existing RAG evaluation reports without running retrieval or LLM evaluation again. "
					+ "Retrieval metrics are returned as the primary retrievalSummary and LLM answer scores are returned separately as the secondary answerSummary.";

	private static final String DEFAULT_BASELINE_EXPERIMENT_ID = "VECTOR_ONLY_V1";

	private final RagEvaluationComparisonService comparisonService;
	private final RagEvaluationProfile ragEvaluationProfile;

	public CompareRagEvaluationReportsTool(RagEvaluationComparisonService comparisonService, RagEvaluationProfile ragEvaluationProfile) {
		this.comparisonService = Objects.requireNonNull(comparisonService, "comparisonService must not be null");
		this.ragEvaluationProfile = Objects.requireNonNull(ragEvaluationProfile, "ragEvaluationProfile must not be null");
	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public Map<String, Object> getInputSchema() {

		Map<String, Object> properties = new LinkedHashMap<>();

		properties.put("baselineExperimentId", property("string", "Optional baseline experiment ID. Defaults to VECTOR_ONLY_V1."));
		properties.put("candidateExperimentId", property("string", "Optional candidate experiment ID. Defaults to the currently configured RAG evaluation experiment ID."));

		Map<String, Object> schema = new LinkedHashMap<>();

		schema.put("type", "object");
		schema.put("properties", properties);
		schema.put("required", List.of());
		schema.put("additionalProperties", false);

		return Collections.unmodifiableMap(schema);
	}

	@Override
	public ToolValidationResult validate(ToolRequest request, ToolContext context) {

		List<ToolIssue> issues = new ArrayList<>();

		if (request == null) {
			issues.add(error("request", "Tool request must not be null."));
			return ToolValidationResult.invalid(issues);
		}

		if (context == null) {
			issues.add(error("context", "Tool context must not be null."));
			return ToolValidationResult.invalid(issues);
		}

		Map<String, Object> arguments = safeArguments(request);

		validateOptionalString(arguments, "baselineExperimentId", issues);
		validateOptionalString(arguments, "candidateExperimentId", issues);

		if (!issues.isEmpty()) return ToolValidationResult.invalid(issues);

		return ToolValidationResult.valid();
	}

	@Override
	public ToolResult execute(ToolRequest request, ToolContext context) {

		ToolValidationResult validation = validate(request, context);

		if (!validation.isValid()) {
			return ToolResult.builder()
					.toolName(TOOL_NAME)
					.status(ToolStatus.FAILED)
					.message("Invalid RAG evaluation comparison request.")
					.issues(validation.getIssues())
					.build();
		}

		Map<String, Object> arguments = safeArguments(request);

		String baselineExperimentId = defaultText(readString(arguments, "baselineExperimentId"), DEFAULT_BASELINE_EXPERIMENT_ID);
		String candidateExperimentId = defaultText(readString(arguments, "candidateExperimentId"), ragEvaluationProfile.getExperimentId());

		try {

			RagEvaluationComparisonResult result = comparisonService.compare(baselineExperimentId, candidateExperimentId);

			if (result == null) {
				return ToolResult.builder()
						.toolName(TOOL_NAME)
						.status(ToolStatus.FAILED)
						.message("RAG evaluation comparison service returned no result.")
						.build();
			}

			RagEvaluationComparison comparison = result.getComparison();

			if (comparison == null) {
				return ToolResult.builder()
						.toolName(TOOL_NAME)
						.status(ToolStatus.FAILED)
						.message("RAG evaluation comparison result does not contain comparison data.")
						.build();
			}

			Map<String, Object> data = createOutput(result, comparison);

			return ToolResult.builder()
					.toolName(TOOL_NAME)
					.status(ToolStatus.SUCCESS)
					.message(createResultMessage(comparison))
					.data(data)
					.build();

		} catch (Exception exception) {

			String message = safeMessage(exception);

			return ToolResult.builder()
					.toolName(TOOL_NAME)
					.status(ToolStatus.FAILED)
					.message("RAG evaluation report comparison failed: " + message)
					.errorCode("RAG_EVALUATION_COMPARISON_FAILED")
					.errorMessage(message)
					.cause(exception)
					.build();
		}
	}

	private Map<String, Object> createOutput(RagEvaluationComparisonResult result, RagEvaluationComparison comparison) {

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("projectId", result.getProjectId());
		data.put("baselineExperimentId", result.getBaselineExperimentId());
		data.put("candidateExperimentId", result.getCandidateExperimentId());

		if (result.getBaselineReportPath() != null) data.put("baselineReportPath", result.getBaselineReportPath().toString());
		if (result.getCandidateReportPath() != null) data.put("candidateReportPath", result.getCandidateReportPath().toString());
		if (result.getComparisonPath() != null) data.put("comparisonPath", result.getComparisonPath().toString());

		Map<String, Object> retrievalSummary = createRetrievalSummaryData(comparison.getSummary());
		Map<String, Object> answerSummary = createAnswerSummaryData(comparison.getAnswerSummary());

		/*
		 * 신규 공식 구조.
		 *
		 * Retrieval = Primary
		 * Answer = Secondary
		 */
		data.put("retrievalSummary", retrievalSummary);
		data.put("answerSummary", answerSummary);

		/*
		 * Legacy compatibility.
		 *
		 * 기존 Agent Prompt / React UI / Tool 소비 코드가
		 * summary 및 최상위 Average Score를 사용하고 있을 수 있으므로
		 * 당분간 유지한다.
		 */
		data.put("summary", retrievalSummary);
		data.put("baselineAverageScore", comparison.getBaselineAverageScore());
		data.put("candidateAverageScore", comparison.getCandidateAverageScore());
		data.put("averageScoreDelta", comparison.getAverageScoreDelta());

		data.put("regressions", createEntryData(comparison.getRegressions()));
		data.put("improvements", createEntryData(comparison.getImprovements()));
		data.put("rankRegressions", createRankRegressionData(comparison.getEntries()));
		data.put("rankImprovements", createRankImprovementData(comparison.getEntries()));

		return Collections.unmodifiableMap(data);
	}

	private Map<String, Object> createRetrievalSummaryData(RagEvaluationComparison.Summary summary) {

		Map<String, Object> data = new LinkedHashMap<>();

		if (summary == null) return Collections.unmodifiableMap(data);

		data.put("totalCases", summary.getTotalCases());
		data.put("applicableCases", summary.getApplicableCases());
		data.put("missToHitCount", summary.getMissToHitCount());
		data.put("hitToMissCount", summary.getHitToMissCount());
		data.put("stableHitCount", summary.getStableHitCount());
		data.put("stableMissCount", summary.getStableMissCount());
		data.put("rankImprovedCount", summary.getRankImprovedCount());
		data.put("rankRegressedCount", summary.getRankRegressedCount());
		data.put("notApplicableCount", summary.getNotApplicableCount());

		return Collections.unmodifiableMap(data);
	}

	private Map<String, Object> createAnswerSummaryData(RagEvaluationComparison.AnswerSummary summary) {

		Map<String, Object> data = new LinkedHashMap<>();

		if (summary == null) return Collections.unmodifiableMap(data);

		data.put("baselineAverageScore", summary.getBaselineAverageScore());
		data.put("candidateAverageScore", summary.getCandidateAverageScore());
		data.put("averageScoreDelta", summary.getAverageScoreDelta());

		return Collections.unmodifiableMap(data);
	}

	private List<Map<String, Object>> createEntryData(List<RagEvaluationComparison.Entry> entries) {

		if (entries == null || entries.isEmpty()) return List.of();

		List<Map<String, Object>> result = new ArrayList<>();

		for (RagEvaluationComparison.Entry entry : entries) {
			if (entry != null) result.add(createEntryData(entry));
		}

		return List.copyOf(result);
	}

	private List<Map<String, Object>> createRankImprovementData(List<RagEvaluationComparison.Entry> entries) {

		if (entries == null || entries.isEmpty()) return List.of();

		List<Map<String, Object>> result = new ArrayList<>();

		for (RagEvaluationComparison.Entry entry : entries) {
			if (entry != null && entry.isRankImproved()) result.add(createEntryData(entry));
		}

		return List.copyOf(result);
	}

	private List<Map<String, Object>> createRankRegressionData(List<RagEvaluationComparison.Entry> entries) {

		if (entries == null || entries.isEmpty()) return List.of();

		List<Map<String, Object>> result = new ArrayList<>();

		for (RagEvaluationComparison.Entry entry : entries) {
			if (entry != null && entry.isRankRegressed()) result.add(createEntryData(entry));
		}

		return List.copyOf(result);
	}

	private Map<String, Object> createEntryData(RagEvaluationComparison.Entry entry) {

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("caseId", entry.getCaseId());
		data.put("question", entry.getQuestion());
		data.put("type", entry.getType().name());

		data.put("baselineApplicable", entry.isBaselineApplicable());
		data.put("candidateApplicable", entry.isCandidateApplicable());

		data.put("baselineHitAtK", entry.isBaselineHitAtK());
		data.put("candidateHitAtK", entry.isCandidateHitAtK());

		data.put("baselineRecallAtK", entry.getBaselineRecallAtK());
		data.put("candidateRecallAtK", entry.getCandidateRecallAtK());
		data.put("recallDelta", entry.getRecallDelta());

		data.put("baselineMrr", entry.getBaselineMrr());
		data.put("candidateMrr", entry.getCandidateMrr());
		data.put("mrrDelta", entry.getMrrDelta());

		/*
		 * 정상적으로 null이 될 수 있다.
		 *
		 * 예:
		 * HIT_TO_MISS
		 * candidateFirstRelevantRank = null
		 *
		 * 따라서 Map.copyOf()를 사용하지 않는다.
		 */
		data.put("baselineFirstRelevantRank", entry.getBaselineFirstRelevantRank());
		data.put("candidateFirstRelevantRank", entry.getCandidateFirstRelevantRank());

		data.put("rankImproved", entry.isRankImproved());
		data.put("rankRegressed", entry.isRankRegressed());

		data.put("baselineOverallScore", entry.getBaselineOverallScore());
		data.put("candidateOverallScore", entry.getCandidateOverallScore());
		data.put("overallScoreDelta", entry.getOverallScoreDelta());

		return Collections.unmodifiableMap(data);
	}

	private String createResultMessage(RagEvaluationComparison comparison) {

		RagEvaluationComparison.Summary retrieval = comparison.getSummary();
		RagEvaluationComparison.AnswerSummary answer = comparison.getAnswerSummary();

		if (retrieval == null || answer == null) return "RAG evaluation report comparison completed.";

		return "RAG evaluation report comparison completed. "
				+ "Retrieval: HIT_TO_MISS=" + retrieval.getHitToMissCount()
				+ ", MISS_TO_HIT=" + retrieval.getMissToHitCount()
				+ ", Rank Improved=" + retrieval.getRankImprovedCount()
				+ ", Rank Regressed=" + retrieval.getRankRegressedCount()
				+ ". Answer Score Delta=" + formatScore(answer.getAverageScoreDelta())
				+ ".";
	}

	private void validateOptionalString(Map<String, Object> arguments, String key, List<ToolIssue> issues) {

		if (!arguments.containsKey(key)) return;

		Object value = arguments.get(key);

		if (value == null) return;

		if (!(value instanceof String)) {
			issues.add(error(key, key + " must be a string value."));
			return;
		}

		if (((String) value).isBlank()) issues.add(error(key, key + " must not be blank when provided."));
	}

	private Map<String, Object> property(String type, String description) {

		Map<String, Object> property = new LinkedHashMap<>();

		property.put("type", type);
		property.put("description", description);

		return property;
	}

	private Map<String, Object> safeArguments(ToolRequest request) {

		if (request == null || request.getArguments() == null) return Collections.emptyMap();

		return request.getArguments();
	}

	private String readString(Map<String, Object> arguments, String key) {

		Object value = arguments.get(key);

		if (value == null) return null;
		if (value instanceof String text) return text;

		return String.valueOf(value);
	}

	private String defaultText(String value, String defaultValue) {

		if (value == null || value.isBlank()) return requireText(defaultValue, "defaultValue");

		return value.trim();
	}

	private String requireText(String value, String fieldName) {

		if (value == null) throw new IllegalArgumentException(fieldName + " must not be null");

		String normalized = value.trim();

		if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");

		return normalized;
	}

	private ToolIssue error(String field, String message) {

		return ToolIssue.builder()
				.severity(ToolIssueSeverity.ERROR)
				.field(field)
				.message(message)
				.build();
	}

	private String formatScore(double value) {
		return String.format(java.util.Locale.ROOT, "%.4f", value);
	}

	private String safeMessage(Throwable throwable) {

		if (throwable == null) return "Unknown RAG evaluation comparison error.";

		String message = throwable.getMessage();

		if (message == null || message.isBlank()) return throwable.getClass().getSimpleName();

		return message.trim();
	}
}