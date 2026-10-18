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

import kr.co.goms.ai.agent.event.AgentEvent;
import kr.co.goms.ai.agent.event.AgentEventPublisher;
import kr.co.goms.ai.project.CurrentProjectProvider;
import kr.co.goms.ai.project.EpubProjectContext;
import kr.co.goms.ai.rag.RagException;
import kr.co.goms.ai.rag.RagService;
import kr.co.goms.ai.rag.context.RagContext;
import kr.co.goms.ai.rag.context.RagSource;
import kr.co.goms.ai.rag.index.ProjectIndexException;
import kr.co.goms.ai.rag.index.ProjectIndexOptions;
import kr.co.goms.ai.rag.index.ProjectIndexProgress;
import kr.co.goms.ai.rag.index.ProjectIndexProgressListener;
import kr.co.goms.ai.rag.index.ProjectIndexResult;
import kr.co.goms.ai.rag.index.ProjectRagIndexer;
import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.ai.tool.AgentTool;
import kr.co.goms.ai.tool.ToolContext;
import kr.co.goms.ai.tool.ToolIssue;
import kr.co.goms.ai.tool.ToolIssueSeverity;
import kr.co.goms.ai.tool.ToolRequest;
import kr.co.goms.ai.tool.ToolResult;
import kr.co.goms.ai.tool.ToolStatus;
import kr.co.goms.ai.tool.ToolValidationResult;

/**
 * 현재 GomsBook EPUB 프로젝트의 RAG 인덱스에서
 * 사용자 질의와 관련된 문서를 검색하는 Tool입니다.
 *
 * <p>
 * 검색 전에 현재 프로젝트의 RAG 인덱스를 자동 동기화하고,
 * 실제 검색 및 Context 생성은 {@link RagService}에 위임합니다.
 * </p>
 *
 * <p>
 * 자동 RAG 인덱스 동기화 진행 상태는
 * {@link ProjectIndexProgressListener}를 통해 수신하고,
 * {@link AgentEventPublisher}를 이용하여 Agent Event로 전달합니다.
 * </p>
 */
public final class SearchProjectDocumentsTool implements AgentTool {

    public static final String TOOL_NAME = "search_project_documents";

    private static final String DESCRIPTION =
            "Searches relevant XHTML document chunks in the current EPUB project. "
                    + "The project RAG index is automatically synchronized before retrieval. "
                    + "No separate indexing operation is required before using this tool. "
                    + "Use this tool when project document content is required to answer the user's request.";

    private static final int DEFAULT_TOP_K = 5;
    private static final int MIN_TOP_K = 1;
    private static final int MAX_TOP_K = 50;

    private final RagService ragService;
    private final CurrentProjectProvider projectProvider;
    private final ProjectRagIndexer projectRagIndexer;
    private final AgentEventPublisher eventPublisher;

    public SearchProjectDocumentsTool(
            RagService ragService,
            CurrentProjectProvider projectProvider,
            ProjectRagIndexer projectRagIndexer,
            AgentEventPublisher eventPublisher) {

        this.ragService = Objects.requireNonNull(ragService, "ragService must not be null");
        this.projectProvider = Objects.requireNonNull(projectProvider, "projectProvider must not be null");
        this.projectRagIndexer = Objects.requireNonNull(projectRagIndexer, "projectRagIndexer must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
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

        Map<String, Object> query = property(
                "string",
                "Search query used to find relevant document chunks in the current EPUB project."
        );

        Map<String, Object> topK = property(
                "integer",
                "Maximum number of relevant document chunks to retrieve."
        );

        topK.put("minimum", MIN_TOP_K);
        topK.put("maximum", MAX_TOP_K);
        topK.put("default", DEFAULT_TOP_K);

        Map<String, Object> sourcePath = property(
                "string",
                "Optional XHTML source path used to restrict the search. Canonical format is OEBPS/Text/chapter.xhtml."
        );

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("query", query);
        properties.put("topK", topK);
        properties.put("sourcePath", sourcePath);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("query"));
        schema.put("additionalProperties", false);

        return Collections.unmodifiableMap(schema);
    }

    @Override
    public ToolValidationResult validate(
            ToolRequest request,
            ToolContext context) {

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

        String query = readString(arguments, "query");

        if (query == null || query.isBlank()) {
            issues.add(error("query", "query must not be blank."));
        }

        validateTopK(arguments, issues);
        validateOptionalString(arguments, "sourcePath", issues);

        if (!issues.isEmpty()) return ToolValidationResult.invalid(issues);

        return ToolValidationResult.valid();
    }

    @Override
    public ToolResult execute(
            ToolRequest request,
            ToolContext context) {

        ToolValidationResult validation = validate(request, context);

        if (!validation.isValid()) {

            return ToolResult.builder()
                    .toolName(TOOL_NAME)
                    .status(ToolStatus.FAILED)
                    .message("Invalid project document search request.")
                    .issues(validation.getIssues())
                    .build();
        }

        Map<String, Object> arguments = safeArguments(request);

        String query = readString(arguments, "query").trim();
        int topK = defaultInteger(readInteger(arguments, "topK"), DEFAULT_TOP_K);
        String sourcePath = normalizeSourcePath(readString(arguments, "sourcePath"));

        String runId = resolveRunId(request, context);

        ProjectIndexProgressListener progressListener = progress -> publishProgress(runId, progress);

        try {

            EpubProjectContext project = projectProvider.getCurrentProject();

            if (project == null) {

                return ToolResult.builder()
                        .toolName(TOOL_NAME)
                        .status(ToolStatus.FAILED)
                        .message("Current EPUB project is not available.")
                        .data("error", "CURRENT_PROJECT_NOT_AVAILABLE")
                        .build();
            }

            /*
             * 검색 전에 현재 프로젝트의 RAG 인덱스를 자동 동기화합니다.
             *
             * IndexProjectDocumentsTool과 동일한 Default Index Policy를 사용합니다.
             * 따라서 quiz.xhtml 등의 기본 제외 정책도 동일하게 적용됩니다.
             *
             * NEW / CHANGED / DELETED 문서만 실제 VectorStore에 반영되고
             * 변경되지 않은 문서는 UNCHANGED / SKIP 처리됩니다.
             */
            ProjectIndexResult indexResult = projectRagIndexer.synchronize(
                    project,
                    ProjectIndexOptions.defaults(),
                    progressListener
            );

            if (indexResult == null) {

                return ToolResult.builder()
                        .toolName(TOOL_NAME)
                        .status(ToolStatus.FAILED)
                        .message("Project RAG index synchronization returned no result.")
                        .data("error", "PROJECT_INDEX_RESULT_NOT_AVAILABLE")
                        .build();
            }

            String projectId = indexResult.getProjectId();

            if (projectId == null || projectId.isBlank()) {

                return ToolResult.builder()
                        .toolName(TOOL_NAME)
                        .status(ToolStatus.FAILED)
                        .message("Project RAG index synchronization returned no projectId.")
                        .data("error", "PROJECT_ID_NOT_AVAILABLE")
                        .build();
            }

            RetrievalRequest retrievalRequest = createRetrievalRequest(
                    projectId,
                    query,
                    topK,
                    sourcePath
            );

            /*
             * Retriever를 직접 호출하지 않고 RagService를 사용합니다.
             *
             * RagService가 Retrieval, Ranking,
             * Context 생성을 조율합니다.
             */
            RagContext ragContext = ragService.buildContext(retrievalRequest);

            if (ragContext == null) {

                return ToolResult.builder()
                        .toolName(TOOL_NAME)
                        .status(ToolStatus.FAILED)
                        .message("RAG service returned no context.")
                        .data("error", "RAG_CONTEXT_NOT_AVAILABLE")
                        .data("query", query)
                        .build();
            }

            Map<String, Object> output = createOutput(
                    projectId,
                    query,
                    topK,
                    sourcePath,
                    indexResult,
                    ragContext
            );

            return ToolResult.builder()
                    .toolName(TOOL_NAME)
                    .status(ToolStatus.SUCCESS)
                    .message(createResultMessage(ragContext))
                    .data(output)
                    .build();

        } catch (ProjectIndexException exception) {

            return ToolResult.builder()
                    .toolName(TOOL_NAME)
                    .status(ToolStatus.FAILED)
                    .message("Automatic RAG index synchronization failed: " + safeMessage(exception))
                    .cause(exception)
                    .build();

        } catch (RagException exception) {

            return ToolResult.builder()
                    .toolName(TOOL_NAME)
                    .status(ToolStatus.FAILED)
                    .message("RAG document search failed: " + safeMessage(exception))
                    .data(createErrorOutput(query, sourcePath, exception))
                    .cause(exception)
                    .build();

        } catch (IllegalArgumentException exception) {

            return ToolResult.builder()
                    .toolName(TOOL_NAME)
                    .status(ToolStatus.FAILED)
                    .message("Invalid RAG search request: " + safeMessage(exception))
                    .cause(exception)
                    .build();

        } catch (RuntimeException exception) {

            return ToolResult.builder()
                    .toolName(TOOL_NAME)
                    .status(ToolStatus.FAILED)
                    .message("Unexpected project document search failure: " + safeMessage(exception))
                    .cause(exception)
                    .build();
        }
    }

    private void publishProgress(
            String runId,
            ProjectIndexProgress progress) {

        if (runId == null || runId.isBlank()) return;
        if (progress == null) return;

        try {

            eventPublisher.publish(
                    AgentEvent.ragProgress(
                            runId,
                            progress.getMessage(),
                            progress
                    )
            );

        } catch (RuntimeException exception) {

            System.err.println(
                    "[RAG][PROGRESS][WARN] Failed to publish progress event: "
                            + safeMessage(exception)
            );
        }
    }

    private String resolveRunId(
            ToolRequest request,
            ToolContext context) {

        if (context != null && context.getRunId() != null && !context.getRunId().isBlank()) {
            return context.getRunId().trim();
        }

        if (request != null && request.getRequestId() != null && !request.getRequestId().isBlank()) {
            return request.getRequestId().trim();
        }

        return null;
    }

    private RetrievalRequest createRetrievalRequest(
            String projectId,
            String query,
            int topK,
            String sourcePath) {

        RetrievalRequest.Builder builder = RetrievalRequest.builder()
                .projectId(projectId)
                .query(query)
                .topK(topK);

        if (sourcePath != null) builder.sourcePath(sourcePath);

        return builder.build();
    }

    private Map<String, Object> createOutput(
            String projectId,
            String query,
            int topK,
            String sourcePath,
            ProjectIndexResult indexResult,
            RagContext ragContext) {

        Map<String, Object> output = new LinkedHashMap<>();

        output.put("projectId", projectId);
        output.put("query", query);
        output.put("topK", topK);

        if (sourcePath != null) output.put("sourcePath", sourcePath);

        output.put("indexSync", createIndexSyncOutput(indexResult));
        output.put("context", ragContext.getContextText());
        output.put("empty", ragContext.isEmpty());
        output.put("sourceCount", ragContext.size());
        output.put("sources", ragContext.getSources());
        output.put("topResults", createTopResults(ragContext));
        output.put("embeddingModel", ragContext.getEmbeddingModel());
        output.put("retrievalDurationMillis", ragContext.getRetrievalDurationMillis());
        output.put("truncated", ragContext.isTruncated());
        output.put("characterCount", ragContext.getCharacterCount());
        output.put("originalCharacterCount", ragContext.getOriginalCharacterCount());
        output.put("omittedCharacterCount", ragContext.getOmittedCharacterCount());
        output.put("highestScore", ragContext.getHighestScore());

        return Collections.unmodifiableMap(output);
    }

    private Map<String, Object> createIndexSyncOutput(
            ProjectIndexResult result) {

        Map<String, Object> output = new LinkedHashMap<>();

        output.put("changed", result.isChanged());
        output.put("newFiles", result.getNewFiles());
        output.put("reindexedFiles", result.getReindexedFiles());
        output.put("skippedFiles", result.getSkippedFiles());
        output.put("deletedFiles", result.getDeletedFiles());
        output.put("createdEmbeddings", result.getCreatedEmbeddings());
        output.put("storedVectors", result.getStoredVectors());
        output.put("deletedVectors", result.getDeletedVectors());
        output.put("vectorStoreSize", result.getVectorStoreSize());

        return Collections.unmodifiableMap(output);
    }

    private List<Map<String, Object>> createTopResults(
            RagContext ragContext) {

        if (ragContext.getSources() == null || ragContext.getSources().isEmpty()) return List.of();

        List<Map<String, Object>> results = new ArrayList<>();

        int rank = 1;

        for (RagSource source : ragContext.getSources()) {

            if (source == null) continue;

            DocumentChunk chunk = source.getChunk();

            if (chunk == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();

            item.put("rank", rank);
            item.put("score", source.getScore());
            item.put("sourcePath", chunk.getSourcePath());
            item.put("chunkId", chunk.getId());
            item.put("heading", chunk.getTitle());
            item.put("type", chunk.getType() == null ? null : chunk.getType().name());
            item.put("text", chunk.getContent());

            results.add(Collections.unmodifiableMap(item));

            rank++;
        }

        return List.copyOf(results);
    }

    private String createResultMessage(
            RagContext ragContext) {

        if (ragContext.isEmpty()) return "No relevant project documents were found.";

        return "Project document search completed with "
                + ragContext.size()
                + " relevant source(s).";
    }

    private Map<String, Object> createErrorOutput(
            String query,
            String sourcePath,
            RagException exception) {

        Map<String, Object> output = new LinkedHashMap<>();

        output.put("query", query);

        if (sourcePath != null) output.put("sourcePath", sourcePath);

        output.put("errorType", exception.getClass().getSimpleName());
        output.put("errorMessage", safeMessage(exception));

        return Collections.unmodifiableMap(output);
    }

    private void validateTopK(
            Map<String, Object> arguments,
            List<ToolIssue> issues) {

        if (!arguments.containsKey("topK")) return;

        Integer topK = readInteger(arguments, "topK");

        if (topK == null) {

            issues.add(error("topK", "topK must be an integer."));

            return;
        }

        if (topK < MIN_TOP_K || topK > MAX_TOP_K) {
            issues.add(error("topK", "topK must be between " + MIN_TOP_K + " and " + MAX_TOP_K + "."));
        }
    }

    private void validateOptionalString(
            Map<String, Object> arguments,
            String key,
            List<ToolIssue> issues) {

        if (!arguments.containsKey(key)) return;

        Object value = arguments.get(key);

        if (value == null) return;

        if (!(value instanceof String)) {
            issues.add(error(key, key + " must be a string value."));
        }
    }

    private Map<String, Object> property(
            String type,
            String description) {

        Map<String, Object> property = new LinkedHashMap<>();

        property.put("type", type);
        property.put("description", description);

        return property;
    }

    private Map<String, Object> safeArguments(
            ToolRequest request) {

        if (request == null || request.getArguments() == null) return Collections.emptyMap();

        return request.getArguments();
    }

    private String readString(
            Map<String, Object> arguments,
            String key) {

        Object value = arguments.get(key);

        if (value == null) return null;

        if (value instanceof String text) return text;

        return String.valueOf(value);
    }

    private Integer readInteger(
            Map<String, Object> arguments,
            String key) {

        Object value = arguments.get(key);

        if (value == null) return null;

        if (value instanceof Number number) return number.intValue();

        try {

            return Integer.valueOf(String.valueOf(value).trim());

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    private int defaultInteger(
            Integer value,
            int defaultValue) {

        return value == null ? defaultValue : value;
    }

    private ToolIssue error(
            String field,
            String message) {

        return ToolIssue.builder()
                .severity(ToolIssueSeverity.ERROR)
                .field(field)
                .message(message)
                .build();
    }

    private String normalizeSourcePath(
            String value) {

        if (value == null) return null;

        String normalized = value.trim().replace('\\', '/');

        if (normalized.isEmpty()) return null;

        while (normalized.startsWith("/")) normalized = normalized.substring(1);

        if (normalized.startsWith("OEBPS/")) return normalized;

        if (normalized.startsWith("TEXT/")) {
            return "OEBPS/Text/" + normalized.substring("TEXT/".length());
        }

        if (normalized.startsWith("Text/")) {
            return "OEBPS/Text/" + normalized.substring("Text/".length());
        }

        if (!normalized.contains("/")) return "OEBPS/Text/" + normalized;

        return normalized;
    }

    private String safeMessage(
            Throwable throwable) {

        if (throwable == null) return "Unknown error";
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) return throwable.getClass().getSimpleName();

        return throwable.getMessage();
    }
}