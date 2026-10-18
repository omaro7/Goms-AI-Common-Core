/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.agent.approval.payload;

import java.util.Objects;

/**
 * 현재 프로젝트의 RAG Vector Index 삭제 승인 Payload.
 */
public final class DeleteRagProjectIndexApprovalPayload {

	private String projectId;

	public DeleteRagProjectIndexApprovalPayload() {
	}

	public DeleteRagProjectIndexApprovalPayload(String projectId) {
		this.projectId = requireText(projectId, "projectId");
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	private static String requireText(String value, String name) {

		Objects.requireNonNull(value, name);

		String normalized = value.trim();

		if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be empty.");

		return normalized;
	}
}