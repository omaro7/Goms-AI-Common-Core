/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.profile;

/**
 * RAG Evaluation 실험 버전.
 */
public enum RagEvaluationVersion {

	V1("v1"),
	V2("v2"),
	V3("v3"),
	V4("v4"),
	V5("v5"),
	V6("v6");
	
	private final String value;

	RagEvaluationVersion(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static RagEvaluationVersion from(String value) {

		if (value == null || value.isBlank()) return V1;

		for (RagEvaluationVersion version : values()) {
			if (version.name().equalsIgnoreCase(value) || version.value.equalsIgnoreCase(value)) return version;
		}

		throw new IllegalArgumentException("Unsupported RAG evaluation version: " + value);
	}
}