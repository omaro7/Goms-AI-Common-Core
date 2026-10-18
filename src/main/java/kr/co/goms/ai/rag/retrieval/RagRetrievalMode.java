/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.retrieval;

public enum RagRetrievalMode {

	VECTOR_ONLY,

	VECTOR_GRAPH,

	HYBRID;

	public static RagRetrievalMode from(String value) {

		if (value == null || value.isBlank()) return VECTOR_ONLY;

		try {
			return valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Unsupported RAG retrieval mode: " + value, exception);
		}
	}
}