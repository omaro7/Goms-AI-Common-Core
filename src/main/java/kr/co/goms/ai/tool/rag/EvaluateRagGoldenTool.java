/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.tool.rag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.rag.eval.model.RagRetrievalEvaluationResult;
import kr.co.goms.ai.rag.eval.profile.RagEvaluationProfile;
import kr.co.goms.ai.rag.eval.report.RagEvaluationReport;
import kr.co.goms.ai.rag.eval.service.RagEvaluationService;
import kr.co.goms.ai.tool.AgentTool;
import kr.co.goms.ai.tool.ToolContext;
import kr.co.goms.ai.tool.ToolRequest;
import kr.co.goms.ai.tool.ToolResult;
import kr.co.goms.ai.tool.ToolStatus;

/**
 * 현재 프로젝트의 RAG Golden Dataset 평가를 실행하는 Tool.
 */
public final class EvaluateRagGoldenTool implements AgentTool {

    public static final String TOOL_NAME = "evaluate_rag_golden";

    private static final String DESCRIPTION = "Evaluates the current project's RAG pipeline using its Golden Dataset and returns retrieval evaluation metrics including Hit Rate@K, Average Recall@K, and MRR. Use this tool when the user explicitly requests Golden Dataset evaluation, RAG baseline evaluation, or retrieval quality measurement.";

    private final RagEvaluationService evaluationService;
    private final RagEvaluationProfile evaluationProfile;

    public EvaluateRagGoldenTool(RagEvaluationService evaluationService, RagEvaluationProfile evaluationProfile) {
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

        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("type", "object");
        schema.put("properties", Map.of());
        schema.put("required", List.of());
        schema.put("additionalProperties", false);

        return Map.copyOf(schema);
    }

    @Override
    public ToolResult execute(ToolRequest request, ToolContext context) {

        String requestId = request == null ? null : request.getRequestId();

        try {

            RagEvaluationReport report = evaluationService.evaluateGolden();

            if (report == null) {
                return ToolResult.builder().requestId(requestId).toolName(TOOL_NAME).status(ToolStatus.FAILED).message("RAG Golden Dataset evaluation returned no report.").build();
            }

            RetrievalSummary summary = createRetrievalSummary(report);

            Map<String, Object> data = new LinkedHashMap<>();

            data.put("experimentId", evaluationProfile.getExperimentId());
            data.put("retrievalType", evaluationProfile.getRetrievalMode().name());
            data.put("datasetName", report.getDatasetName());
            data.put("totalCases", report.size());
            data.put("evaluatedCases", summary.evaluatedCases());
            data.put("hitRateAtK", summary.hitRateAtK());
            data.put("averageRecallAtK", summary.averageRecallAtK());
            data.put("mrr", summary.mrr());
            data.put("averageScore", report.getAverageScore());

            return ToolResult.builder().requestId(requestId).toolName(TOOL_NAME).status(ToolStatus.SUCCESS).message(createSuccessMessage(summary)).data(data).build();

        } catch (IllegalArgumentException | IllegalStateException exception) {

            return ToolResult.builder().requestId(requestId).toolName(TOOL_NAME).status(ToolStatus.FAILED).message("RAG Golden Dataset evaluation failed: " + safeMessage(exception)).cause(exception).build();

        } catch (Exception exception) {

            return ToolResult.builder().requestId(requestId).toolName(TOOL_NAME).status(ToolStatus.FAILED).message("Unexpected RAG Golden Dataset evaluation failure: " + safeMessage(exception)).cause(exception).build();
        }
    }

    private RetrievalSummary createRetrievalSummary(RagEvaluationReport report) {

        int evaluatedCases = 0;
        int hitCases = 0;
        double recallTotal = 0.0;
        double reciprocalRankTotal = 0.0;

        for (RagEvaluationReport.Entry entry : report.getEntries()) {

            if (entry == null) {
                continue;
            }

            RagRetrievalEvaluationResult retrievalResult = entry.getRetrievalEvaluationResult();

            if (retrievalResult == null || !retrievalResult.isApplicable()) {
                continue;
            }

            evaluatedCases++;

            if (retrievalResult.isHitAtK()) {
                hitCases++;
            }

            recallTotal += retrievalResult.getRecallAtK();
            reciprocalRankTotal += retrievalResult.getMrr();
        }

        if (evaluatedCases == 0) {
            return new RetrievalSummary(0, 0.0, 0.0, 0.0);
        }

        return new RetrievalSummary(evaluatedCases, (double) hitCases / evaluatedCases, recallTotal / evaluatedCases, reciprocalRankTotal / evaluatedCases);
    }

    private String createSuccessMessage(RetrievalSummary summary) {
        return "RAG Golden Dataset evaluation completed. Evaluated cases=" + summary.evaluatedCases() + ", Hit Rate@K=" + format(summary.hitRateAtK()) + ", Average Recall@K=" + format(summary.averageRecallAtK()) + ", MRR=" + format(summary.mrr()) + ".";
    }

    private String format(double value) {
        return String.format("%.4f", value);
    }

    private String safeMessage(Throwable throwable) {

        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "Unknown error";
        }

        return throwable.getMessage();
    }

    private record RetrievalSummary(int evaluatedCases, double hitRateAtK, double averageRecallAtK, double mrr) {
    }
}