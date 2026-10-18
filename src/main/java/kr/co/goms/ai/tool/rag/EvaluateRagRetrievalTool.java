/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.tool.rag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import kr.co.goms.ai.rag.eval.profile.RagEvaluationProfile;
import kr.co.goms.ai.rag.eval.report.RagRetrievalEvaluationReport;
import kr.co.goms.ai.rag.eval.service.RagRetrievalEvaluationService;
import kr.co.goms.ai.tool.AgentTool;
import kr.co.goms.ai.tool.ToolContext;
import kr.co.goms.ai.tool.ToolRequest;
import kr.co.goms.ai.tool.ToolResult;
import kr.co.goms.ai.tool.ToolStatus;

public final class EvaluateRagRetrievalTool implements AgentTool {

	public static final String TOOL_NAME = "evaluate_rag_retrieval";

	private static final String DESCRIPTION = "Evaluates Golden Dataset retrieval performance without generating LLM answers. Calculates Hit Rate@K, Average Recall@K, and MRR for the current RAG retrieval profile.";

	private final RagRetrievalEvaluationService evaluationService;
	private final RagEvaluationProfile evaluationProfile;

	public EvaluateRagRetrievalTool(RagRetrievalEvaluationService evaluationService, RagEvaluationProfile evaluationProfile) {
		this.evaluationService = Objects.requireNonNull(evaluationService, "evaluationService must not be null");
		this.evaluationProfile = Objects.requireNonNull(evaluationProfile, "evaluationProfile must not be null");
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

		properties.put("caseIds", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Optional Golden Dataset case IDs. When omitted, all retrieval-evaluable cases are evaluated."));

		Map<String, Object> schema = new LinkedHashMap<>();

		schema.put("type", "object");
		schema.put("properties", properties);
		schema.put("required", List.of());
		schema.put("additionalProperties", false);

		return Map.copyOf(schema);
	}

	@Override
	public ToolResult execute(ToolRequest request, ToolContext context) {

		try {

			List<String> caseIds = readCaseIds(request);
			RagRetrievalEvaluationReport report = caseIds.isEmpty() ? evaluationService.evaluate() : evaluationService.evaluate(caseIds);
			Map<String, Object> data = new LinkedHashMap<>();

			data.put("experimentId", evaluationProfile.getExperimentId());
			data.put("retrievalType", evaluationProfile.getRetrievalMode().name());
			data.put("datasetName", report.getDatasetName());
			data.put("totalCases", report.getTotalCases());
			data.put("evaluatedCases", report.getEvaluatedCases());
			data.put("hitRateAtK", report.getHitRateAtK());
			data.put("averageRecallAtK", report.getAverageRecallAtK());
			data.put("mrr", report.getMrr());
			data.put("filteredRun", !caseIds.isEmpty());
			data.put("caseIds", caseIds);
			data.put("llmUsed", false);

			return ToolResult.builder().toolName(TOOL_NAME).status(ToolStatus.SUCCESS).message("RAG retrieval evaluation completed. Evaluated cases=" + report.getEvaluatedCases() + ", Hit Rate@K=" + format(report.getHitRateAtK()) + ", Average Recall@K=" + format(report.getAverageRecallAtK()) + ", MRR=" + format(report.getMrr()) + ".").data(data).build();

		} catch (Exception exception) {

			return ToolResult.builder().toolName(TOOL_NAME).status(ToolStatus.FAILED).message("RAG retrieval evaluation failed: " + safeMessage(exception)).cause(exception).build();
		}
	}

	private List<String> readCaseIds(ToolRequest request) {

		if (request == null) return List.of();

		Object value = request.getArgument("caseIds");

		if (value == null) return List.of();
		if (!(value instanceof Iterable<?> iterable)) throw new IllegalArgumentException("caseIds must be an array");

		Set<String> values = new LinkedHashSet<>();

		for (Object item : iterable) {
			if (!(item instanceof String caseId)) throw new IllegalArgumentException("caseIds must contain only strings");
			if (!caseId.isBlank()) values.add(caseId.trim());
		}

		return List.copyOf(values);
	}

	private String format(double value) {
		return String.format(java.util.Locale.ROOT, "%.4f", value);
	}

	private String safeMessage(Throwable throwable) {

		if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) return throwable == null ? "Unknown error" : throwable.getClass().getSimpleName();

		return throwable.getMessage();
	}
}