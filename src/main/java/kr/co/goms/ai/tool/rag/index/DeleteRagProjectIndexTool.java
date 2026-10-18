/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.tool.rag.index;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;

import kr.co.goms.ai.agent.approval.AgentApproval;
import kr.co.goms.ai.agent.approval.AgentApprovalAction;
import kr.co.goms.ai.agent.approval.AgentApprovalService;
import kr.co.goms.ai.agent.approval.payload.DeleteRagProjectIndexApprovalPayload;
import kr.co.goms.ai.rag.util.RagUtil;
import kr.co.goms.ai.tool.AgentTool;
import kr.co.goms.ai.tool.ToolContext;
import kr.co.goms.ai.tool.ToolRequest;
import kr.co.goms.ai.tool.ToolResult;
import kr.co.goms.ai.tool.ToolStatus;

/**
 * 현재 프로젝트의 현재 Embedding Model에 해당하는
 * RAG Vector Index 삭제를 요청하는 Tool.
 *
 * 실제 삭제는 사용자 승인 후
 * DeleteRagProjectIndexApprovalHandler에서 수행합니다.
 */
public final class DeleteRagProjectIndexTool implements AgentTool {

	public static final String TOOL_NAME = "delete_rag_project_index";
	private static final String DESCRIPTION =
	        "Deletes the existing RAG vector index for the current GomsBook project "
	                + "and the current embedding model. "
	                + "Use this tool when the user explicitly requests deletion, clearing, "
	                + "removal, reset, or complete removal of the current project's RAG index. "
	                + "This operation requires user approval before actual deletion. "
	                + "Only the current project's vectors are deleted; vectors belonging to "
	                + "other projects in the shared Qdrant collection are preserved. "
	                + "Do NOT use index_project_documents with excludeFiles to perform index deletion.";
	
	private static final String APPROVAL_TITLE = "RAG 인덱스 삭제";
	private static final String PREVIEW_TITLE = "내용";

	private final AgentApprovalService approvalService;
	private final Gson gson;

	public DeleteRagProjectIndexTool(AgentApprovalService approvalService, Gson gson) {
		this.approvalService = Objects.requireNonNull(approvalService, "approvalService must not be null");
		this.gson = Objects.requireNonNull(gson, "gson must not be null");
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

		try {

			if (request == null) throw new IllegalArgumentException("ToolRequest must not be null.");
			if (context == null) throw new IllegalArgumentException("ToolContext must not be null.");

			String runId = requireText(context.getRunId(), "context.runId");
			String projectId = RagUtil.requireProjectId(context.getProjectId());

			DeleteRagProjectIndexApprovalPayload payload = new DeleteRagProjectIndexApprovalPayload(projectId);

			String content = gson.toJson(payload);
			String fileName = projectId + " RAG index";
			String message = projectId + " 프로젝트의 현재 Embedding Model RAG 인덱스를 삭제하시겠습니까? 삭제 후 다시 사용하려면 재인덱싱이 필요합니다.";

			AgentApproval approval = approvalService.create(
					runId,
					projectId,
					AgentApprovalAction.PREFIX + TOOL_NAME,
					APPROVAL_TITLE,
					message,
					fileName,
					content);

			Map<String, Object> data = new LinkedHashMap<>();

			data.put("approvalRequired", true);
			data.put("approvalId", approval.getApprovalId());
			data.put("action", approval.getAction());
			data.put("title", approval.getTitle());
			data.put("message", approval.getMessage());
			data.put("fileName", approval.getFileName());
			data.put("content", approval.getContent());
			data.put("previewTitle", PREVIEW_TITLE);
			data.put("preview", createPreview(projectId));
			data.put("projectId", projectId);
			data.put("scope", "PROJECT_CURRENT_MODEL");
			data.put("displayInstruction", "미리보기 섹션 제목은 반드시 '내용'으로 표시하고 콜론(:)을 붙이지 마세요.");

			return ToolResult.builder()
					.toolName(TOOL_NAME)
					.requestId(request.getRequestId())
					.toolCallId(request.getToolCallId())
					.status(ToolStatus.SUCCESS)
					.message("RAG project index deletion approval is required.")
					.data(data)
					.build();

		} catch (RuntimeException exception) {

			return ToolResult.builder()
					.toolName(TOOL_NAME)
					.requestId(request != null ? request.getRequestId() : null)
					.toolCallId(request != null ? request.getToolCallId() : null)
					.status(ToolStatus.FAILED)
					.message("Failed to prepare RAG project index deletion: " + safeMessage(exception))
					.cause(exception)
					.build();
		}
	}

	private String createPreview(String projectId) {
		return "프로젝트: " + projectId + "\n삭제 범위: 현재 Embedding Model의 RAG Vector Index\n다른 프로젝트 Vector Index: 유지";
	}

	private String requireText(String value, String name) {
		if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank.");
		return value.trim();
	}

	private String safeMessage(Throwable throwable) {
		if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) return throwable == null ? "Unknown error." : throwable.getClass().getSimpleName();
		return throwable.getMessage();
	}
}