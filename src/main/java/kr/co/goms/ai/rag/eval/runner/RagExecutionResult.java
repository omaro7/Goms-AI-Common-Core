/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.rag.eval.model.RagRetrievalResult;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;

/**
 * RAG 실행 결과.
 *
 * Retriever가 검색한 Context와 LLM이 생성한 최종 답변을 보관한다.
 */
public final class RagExecutionResult {

	private final List<String> retrievedContexts;
	private final String answer;
	private final RagRetrievalResult retrievalResult;
	private final RetrievalResult rawRetrievalResult;

	public RagExecutionResult(List<String> retrievedContexts, String answer) {
		this(retrievedContexts, answer, null, null);
	}

	public RagExecutionResult(List<String> retrievedContexts, String answer, RagRetrievalResult retrievalResult) {
		this(retrievedContexts, answer, retrievalResult, null);
	}

	public RagExecutionResult(List<String> retrievedContexts, String answer, RagRetrievalResult retrievalResult, RetrievalResult rawRetrievalResult) {
		this.retrievedContexts = normalizeContexts(retrievedContexts);
		this.answer = requireText(answer, "answer");
		this.retrievalResult = retrievalResult;
		this.rawRetrievalResult = rawRetrievalResult;
	}

	public List<String> getRetrievedContexts() {
		return retrievedContexts;
	}

	public String getAnswer() {
		return answer;
	}

	public RagRetrievalResult getRetrievalResult() {
		return retrievalResult;
	}

	public boolean hasRetrievalResult() {
		return retrievalResult != null;
	}

	public RetrievalResult getRawRetrievalResult() {
		return rawRetrievalResult;
	}

	public boolean hasRawRetrievalResult() {
		return rawRetrievalResult != null;
	}

	private static List<String> normalizeContexts(List<String> contexts) {

		if (contexts == null || contexts.isEmpty()) return Collections.emptyList();

		List<String> normalized = new java.util.ArrayList<>();

		for (String context : contexts) {

			if (context == null) continue;

			String value = context.trim();

			if (!value.isEmpty()) normalized.add(value);
		}

		return Collections.unmodifiableList(normalized);
	}

	private static String requireText(String value, String fieldName) {

		Objects.requireNonNull(value, fieldName + " must not be null");

		String normalized = value.trim();

		if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");

		return normalized;
	}
}