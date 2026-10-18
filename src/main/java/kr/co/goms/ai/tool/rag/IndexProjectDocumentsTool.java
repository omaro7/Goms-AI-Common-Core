/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.tool.rag;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.agent.event.AgentEvent;
import kr.co.goms.ai.agent.event.AgentEventPublisher;
import kr.co.goms.ai.project.CurrentProjectProvider;
import kr.co.goms.ai.project.EpubProjectContext;
import kr.co.goms.ai.rag.index.ProjectIndexException;
import kr.co.goms.ai.rag.index.ProjectIndexOptions;
import kr.co.goms.ai.rag.index.ProjectIndexProgress;
import kr.co.goms.ai.rag.index.ProjectIndexProgressListener;
import kr.co.goms.ai.rag.index.ProjectIndexResult;
import kr.co.goms.ai.rag.index.ProjectRagIndexer;
import kr.co.goms.ai.tool.AgentTool;
import kr.co.goms.ai.tool.ToolContext;
import kr.co.goms.ai.tool.ToolIssue;
import kr.co.goms.ai.tool.ToolIssueSeverity;
import kr.co.goms.ai.tool.ToolRequest;
import kr.co.goms.ai.tool.ToolResult;
import kr.co.goms.ai.tool.ToolStatus;
import kr.co.goms.ai.tool.ToolValidationResult;

/**
 * 현재 GomsBook EPUB 프로젝트의 RAG 인덱스를
 * 명시적으로 동기화하는 Tool입니다.
 *
 * <p>
 * 실제 NEW / CHANGED / UNCHANGED / DELETED 판정,
 * Embedding 및 VectorStore 갱신은 {@link ProjectRagIndexer}에
 * 위임합니다.
 * </p>
 *
 * <p>
 * {@code excludeFiles}를 통해 특정 XHTML 파일을
 * 현재 동기화에서 추가로 제외할 수 있습니다.
 * </p>
 *
 * <p>
 * quiz.xhtml과 같은 기본 제외 파일은
 * {@link ProjectRagIndexer} 구현체의 기본 정책에서 처리합니다.
 * </p>
 *
 * <p>
 * RAG 인덱싱 진행 상태는 {@link ProjectIndexProgressListener}를 통해 수신하고,
 * {@link AgentEventPublisher}를 이용하여 Agent Event로 전달합니다.
 * </p>
 */
public final class IndexProjectDocumentsTool implements AgentTool {

    public static final String TOOL_NAME = "index_project_documents";

    private static final String EXCLUDE_FILES_ARGUMENT = "excludeFiles";

    private static final String DESCRIPTION =
            "Synchronizes the RAG index for XHTML documents in the TEXT directory "
                    + "of the current EPUB project. "
                    + "Use this tool only when the user explicitly requests indexing, "
                    + "reindexing, synchronization, or updating the existing RAG index. "
                    + "Optional excludeFiles can be used only to skip additional XHTML files "
                    + "during the current synchronization operation. "
                    + "excludeFiles MUST NOT be used to delete, clear, remove, or reset "
                    + "the existing RAG index. "
                    + "Files excluded by the indexer's default policy, such as quiz.xhtml, "
                    + "are excluded automatically. "
                    + "Files that no longer exist in the project may be removed from the index "
                    + "as part of synchronization according to the indexer's deletion policy. "
                    + "If the user explicitly requests deletion, clearing, removal, or reset "
                    + "of the current project's RAG index, use delete_rag_project_index instead.";

    private final CurrentProjectProvider projectProvider;
    private final ProjectRagIndexer projectRagIndexer;
    private final AgentEventPublisher eventPublisher;

    public IndexProjectDocumentsTool(
            CurrentProjectProvider projectProvider,
            ProjectRagIndexer projectRagIndexer,
            AgentEventPublisher eventPublisher) {

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

        Map<String, Object> excludeFiles = new LinkedHashMap<>();
        excludeFiles.put("type", "array");
        excludeFiles.put("description", "Optional XHTML file names or project-relative XHTML paths to exclude from RAG indexing. Examples: author.xhtml, OEBPS/Text/lot.xhtml.");
        excludeFiles.put("items", Map.of("type", "string"));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(EXCLUDE_FILES_ARGUMENT, excludeFiles);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of());
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

        validateExcludeFiles(arguments, issues);

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
                    .message("Invalid project RAG indexing request.")
                    .issues(validation.getIssues())
                    .build();
        }

        Map<String, Object> arguments = safeArguments(request);

        List<String> excludeFiles = readStringList(arguments, EXCLUDE_FILES_ARGUMENT);

        ProjectIndexOptions options = ProjectIndexOptions.builder()
                .excludeFiles(excludeFiles)
                .build();

        String runId = resolveRunId(request, context);

        ProjectIndexProgressListener progressListener = progress -> publishProgress(runId, progress);

        try {

            EpubProjectContext project = projectProvider.getCurrentProject();

            if (project == null) {

                return ToolResult.builder()
                        .toolName(TOOL_NAME)
                        .status(ToolStatus.FAILED)
                        .message("Current EPUB project is not available.")
                        .data("excludeFiles", excludeFiles)
                        .data("error", "CURRENT_PROJECT_NOT_AVAILABLE")
                        .build();
            }

            ProjectIndexResult result = projectRagIndexer.synchronize(project, options, progressListener);

            if (result == null) {

                return ToolResult.builder()
                        .toolName(TOOL_NAME)
                        .status(ToolStatus.FAILED)
                        .message("Project RAG index synchronization returned no result.")
                        .data("excludeFiles", excludeFiles)
                        .data("error", "PROJECT_INDEX_RESULT_NOT_AVAILABLE")
                        .build();
            }

            return ToolResult.success(TOOL_NAME)
                    .message("Project RAG index synchronized successfully.")
                    .data("projectId", result.getProjectId())
                    .data("projectName", result.getProjectName())
                    .data("textDirectory", result.getTextDirectory())
                    .data("embeddingModel", result.getEmbeddingModel())
                    .data("excludeFiles", excludeFiles)
                    .data("discoveredFiles", result.getDiscoveredFiles())
                    .data("processedFiles", result.getProcessedFiles())
                    .data("newFiles", result.getNewFiles())
                    .data("reindexedFiles", result.getReindexedFiles())
                    .data("skippedFiles", result.getSkippedFiles())
                    .data("deletedFiles", result.getDeletedFiles())
                    .data("createdChunks", result.getCreatedChunks())
                    .data("createdEmbeddings", result.getCreatedEmbeddings())
                    .data("storedVectors", result.getStoredVectors())
                    .data("deletedVectors", result.getDeletedVectors())
                    .data("vectorStoreSize", result.getVectorStoreSize())
                    .data("indexedFiles", result.getIndexedFiles())
                    .data("skippedFilePaths", result.getSkippedFilePaths())
                    .data("deletedFilePaths", result.getDeletedFilePaths())
                    .build();

        } catch (ProjectIndexException exception) {

            return ToolResult.failure(
                    TOOL_NAME,
                    "Failed to synchronize project RAG index: " + safeMessage(exception),
                    exception
            ).build();

        } catch (RuntimeException exception) {

            return ToolResult.failure(
                    TOOL_NAME,
                    "Unexpected project RAG indexing failure: " + safeMessage(exception),
                    exception
            ).build();
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

    private void validateExcludeFiles(
            Map<String, Object> arguments,
            List<ToolIssue> issues) {

        if (!arguments.containsKey(EXCLUDE_FILES_ARGUMENT)) return;

        Object value = arguments.get(EXCLUDE_FILES_ARGUMENT);

        if (value == null) return;

        if (!(value instanceof Collection<?>) && !value.getClass().isArray()) {

            issues.add(
                    error(
                            EXCLUDE_FILES_ARGUMENT,
                            "excludeFiles must be an array of strings."
                    )
            );

            return;
        }

        List<?> values = toList(value);

        for (int index = 0; index < values.size(); index++) {

            Object item = values.get(index);

            if (!(item instanceof String text)) {

                issues.add(
                        error(
                                EXCLUDE_FILES_ARGUMENT,
                                "excludeFiles[" + index + "] must be a string."
                        )
                );

                continue;
            }

            if (text.isBlank()) {

                issues.add(
                        error(
                                EXCLUDE_FILES_ARGUMENT,
                                "excludeFiles[" + index + "] must not be blank."
                        )
                );
            }
        }
    }

    private List<String> readStringList(
            Map<String, Object> arguments,
            String key) {

        Object value = arguments.get(key);

        if (value == null) return List.of();

        List<?> values = toList(value);

        if (values.isEmpty()) return List.of();

        List<String> result = new ArrayList<>();

        for (Object item : values) {

            if (!(item instanceof String text)) continue;

            String normalized = normalizeExcludedFile(text);

            if (normalized == null) continue;

            if (!result.contains(normalized)) result.add(normalized);
        }

        return List.copyOf(result);
    }

    private List<?> toList(Object value) {

        if (value == null) return List.of();

        if (value instanceof List<?> list) return list;

        if (value instanceof Collection<?> collection) return new ArrayList<>(collection);

        if (!value.getClass().isArray()) return List.of();

        int length = Array.getLength(value);

        List<Object> result = new ArrayList<>(length);

        for (int index = 0; index < length; index++) result.add(Array.get(value, index));

        return result;
    }

    private String normalizeExcludedFile(String value) {

        if (value == null) return null;

        String normalized = value.trim().replace('\\', '/');

        if (normalized.isEmpty()) return null;

        while (normalized.startsWith("/")) normalized = normalized.substring(1);

        return normalized;
    }

    private Map<String, Object> safeArguments(ToolRequest request) {

        if (request == null || request.getArguments() == null) return Collections.emptyMap();

        return request.getArguments();
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

    private String safeMessage(Throwable throwable) {

        if (throwable == null) return "Unknown error";
        if (throwable.getMessage() == null || throwable.getMessage().isBlank()) return throwable.getClass().getSimpleName();

        return throwable.getMessage();
    }
}