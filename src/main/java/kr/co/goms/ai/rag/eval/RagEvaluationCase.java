/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.rag.util.RagUtil;

/**
 * Golden Dataset의 개별 RAG 평가 Case.
 */
public final class RagEvaluationCase {

	private final String id;
	private final RagEvaluationCaseType type;
	private final String question;
	private final String referenceAnswer;
	private final List<String> expectedDocuments;

	public RagEvaluationCase(String id, RagEvaluationCaseType type, String question, String referenceAnswer) {
		this(id, type, question, referenceAnswer, Collections.emptyList());
	}

	public RagEvaluationCase(String id, RagEvaluationCaseType type, String question, String referenceAnswer, List<String> expectedDocuments) {
		this.id = RagUtil.requireText(id, "id");
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.question = RagUtil.requireText(question, "question");
		this.referenceAnswer = RagUtil.normalizeOptional(referenceAnswer);
		this.expectedDocuments = RagUtil.normalizeExpectedDocuments(expectedDocuments);
	}

	public String getId() {
		return id;
	}

	public RagEvaluationCaseType getType() {
		return type;
	}

	public String getQuestion() {
		return question;
	}

	public String getReferenceAnswer() {
		return referenceAnswer;
	}

	public List<String> getExpectedDocuments() {
		return expectedDocuments;
	}

	public boolean hasExpectedDocuments() {
		return !expectedDocuments.isEmpty();
	}

	@Override
	public String toString() {
		return "RagEvaluationCase{" + "id='" + id + '\'' + ", type='" + type + '\'' + ", question='" + question + '\'' + ", expectedDocuments=" + expectedDocuments + '}';
	}
}