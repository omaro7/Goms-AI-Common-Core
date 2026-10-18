/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.tool.rag.benchmark;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.rag.eval.benchmark.VectorBenchmarkLatency;
import kr.co.goms.ai.rag.eval.benchmark.VectorBenchmarkResult;
import kr.co.goms.ai.rag.eval.benchmark.VectorStoreBenchmarkExecutionResult;
import kr.co.goms.ai.rag.eval.benchmark.VectorStoreBenchmarkExecutionService;
import kr.co.goms.ai.rag.eval.benchmark.VectorStoreBenchmarkReport;
import kr.co.goms.ai.tool.AgentTool;
import kr.co.goms.ai.tool.ToolContext;
import kr.co.goms.ai.tool.ToolRequest;
import kr.co.goms.ai.tool.ToolResult;
import kr.co.goms.ai.tool.ToolStatus;
import kr.co.goms.ai.util.ToolUtil;

/**
 * 현재 프로젝트의 Golden Dataset을 이용하여
 * InMemoryVectorStore와 QdrantVectorStore의
 * Vector Search Latency를 비교하는 Tool입니다.
 *
 * <pre>
 * Current Project
 *      ↓
 * Latest Golden Dataset
 *      ↓
 * Query Embedding 사전 생성
 *      ↓
 * ┌───────────────────────┬───────────────────────┐
 * │ VECTOR_MEMORY_V1      │ VECTOR_QDRANT_V1      │
 * │ InMemoryVectorStore   │ QdrantVectorStore     │
 * └───────────────────────┴───────────────────────┘
 *      ↓
 * Pure Vector Search Latency
 *      ↓
 * Average / P50 / P95 / P99
 *      ↓
 * JSON Benchmark Report
 * </pre>
 *
 * <p>
 * Query Embedding 시간은 Benchmark 측정 대상에서 제외합니다.
 * 동일한 Query Vector를 사용하여 Vector Store 자체의
 * Search Latency를 비교합니다.
 * </p>
 */
public final class BenchmarkRagVectorStoreTool implements AgentTool {

	public static final String TOOL_NAME = "benchmark_rag_vector_store";

	public static final String DESCRIPTION =
			"Benchmarks pure vector search latency between InMemoryVectorStore and QdrantVectorStore "
			+ "for the current GomsBook project using the latest RAG Golden Dataset. "
			+ "Returns Average, Min, Max, P50, P95 and P99 latency metrics. "
			+ "Use this tool when the user asks to compare Vector Memory and Qdrant search performance, "
			+ "latency, benchmark performance, P50, P95 or P99. "
			+ "Query embedding time is excluded from the measured vector search latency.";

	private final VectorStoreBenchmarkExecutionService executionService;

	public BenchmarkRagVectorStoreTool(VectorStoreBenchmarkExecutionService executionService) {
		this.executionService = Objects.requireNonNull(executionService, "executionService must not be null");
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

		return schema;
	}

	@Override
	public ToolResult execute(ToolRequest request, ToolContext context) {

		String requestId = request == null ? null : request.getRequestId();
		String toolCallId = request == null ? null : request.getToolCallId();

		try {

			VectorStoreBenchmarkExecutionResult executionResult = executionService.execute();
			VectorStoreBenchmarkReport report = executionResult.getReport();

			return success(requestId, toolCallId, executionResult, report);

		} catch (IllegalArgumentException | IllegalStateException exception) {

			return ToolUtil.validationFailed(
					requestId,
					TOOL_NAME,
					ToolUtil.safeMessage(exception));

		} catch (Exception exception) {

			return ToolUtil.failed(
					requestId,
					TOOL_NAME,
					"RAG_VECTOR_STORE_BENCHMARK_FAILED",
					"Failed to benchmark RAG Vector Store: " + ToolUtil.safeMessage(exception),
					exception);
		}
	}

	private ToolResult success(
			String requestId,
			String toolCallId,
			VectorStoreBenchmarkExecutionResult executionResult,
			VectorStoreBenchmarkReport report) {

		VectorBenchmarkResult memory = report.getMemory();
		VectorBenchmarkResult qdrant = report.getQdrant();

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("datasetName", report.getDatasetName());
		data.put("projectId", report.getProjectId());
		data.put("model", report.getModel());
		data.put("topK", report.getTopK());
		data.put("datasetPath", executionResult.getDatasetPath().toString());
		data.put("reportPath", executionResult.getReportPath().toString());
		data.put("memory", toBenchmarkData(memory));
		data.put("qdrant", toBenchmarkData(qdrant));
		data.put("comparison", toComparisonData(memory, qdrant));
		data.put("embeddingLatencyIncluded", false);

		return ToolResult.builder()
				.requestId(requestId)
				.toolCallId(toolCallId)
				.toolName(TOOL_NAME)
				.status(ToolStatus.SUCCESS)
				.message(createMessage(memory, qdrant))
				.data(data)
				.build();
	}

	private Map<String, Object> toBenchmarkData(VectorBenchmarkResult result) {

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("experimentId", result.getExperimentId());
		data.put("backend", result.getBackend());
		data.put("projectId", result.getProjectId());
		data.put("model", result.getModel());
		data.put("vectorCount", result.getVectorCount());
		data.put("queryCount", result.getQueryCount());
		data.put("warmupIterations", result.getWarmupIterations());
		data.put("measurementIterations", result.getMeasurementIterations());
		data.put("latency", toLatencyData(result.getLatency()));

		return data;
	}

	private Map<String, Object> toLatencyData(VectorBenchmarkLatency latency) {

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("sampleCount", latency.getSampleCount());
		data.put("averageMs", latency.getAverageMs());
		data.put("minMs", latency.getMinMs());
		data.put("maxMs", latency.getMaxMs());
		data.put("p50Ms", latency.getP50Ms());
		data.put("p95Ms", latency.getP95Ms());
		data.put("p99Ms", latency.getP99Ms());

		return data;
	}

	private Map<String, Object> toComparisonData(
			VectorBenchmarkResult memory,
			VectorBenchmarkResult qdrant) {

		VectorBenchmarkLatency memoryLatency = memory.getLatency();
		VectorBenchmarkLatency qdrantLatency = qdrant.getLatency();

		Map<String, Object> data = new LinkedHashMap<>();

		data.put("averageDeltaMs", qdrantLatency.getAverageMs() - memoryLatency.getAverageMs());
		data.put("p50DeltaMs", qdrantLatency.getP50Ms() - memoryLatency.getP50Ms());
		data.put("p95DeltaMs", qdrantLatency.getP95Ms() - memoryLatency.getP95Ms());
		data.put("p99DeltaMs", qdrantLatency.getP99Ms() - memoryLatency.getP99Ms());

		data.put("averageRatio", ratio(qdrantLatency.getAverageMs(), memoryLatency.getAverageMs()));
		data.put("p50Ratio", ratio(qdrantLatency.getP50Ms(), memoryLatency.getP50Ms()));
		data.put("p95Ratio", ratio(qdrantLatency.getP95Ms(), memoryLatency.getP95Ms()));
		data.put("p99Ratio", ratio(qdrantLatency.getP99Ms(), memoryLatency.getP99Ms()));

		return data;
	}

	private String createMessage(
			VectorBenchmarkResult memory,
			VectorBenchmarkResult qdrant) {

		VectorBenchmarkLatency memoryLatency = memory.getLatency();
		VectorBenchmarkLatency qdrantLatency = qdrant.getLatency();

		return "RAG Vector Store benchmark completed. "
				+ "VECTOR_MEMORY_V1 Average=" + format(memoryLatency.getAverageMs()) + " ms, "
				+ "P50=" + format(memoryLatency.getP50Ms()) + " ms, "
				+ "P95=" + format(memoryLatency.getP95Ms()) + " ms, "
				+ "P99=" + format(memoryLatency.getP99Ms()) + " ms. "
				+ "VECTOR_QDRANT_V1 Average=" + format(qdrantLatency.getAverageMs()) + " ms, "
				+ "P50=" + format(qdrantLatency.getP50Ms()) + " ms, "
				+ "P95=" + format(qdrantLatency.getP95Ms()) + " ms, "
				+ "P99=" + format(qdrantLatency.getP99Ms()) + " ms.";
	}

	private Double ratio(double numerator, double denominator) {
		if (!Double.isFinite(numerator) || !Double.isFinite(denominator) || denominator <= 0.0) return null;
		return numerator / denominator;
	}

	private String format(double value) {
		return String.format(Locale.US, "%.4f", value);
	}
}