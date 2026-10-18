/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * RAG 평가 실행에 필요한 전체 Context.
 *
 * 질문, 검색된 Context, 생성된 답변,
 * 그리고 기준 답변(reference answer)을 함께 관리한다.
 */
public final class RagEvaluationContext {

	private final RagEvaluationCaseType caseType;
    private final String question;
    private final List<String> retrievedContexts;
    private final String answer;
    private final String referenceAnswer;

    public RagEvaluationContext(
    		RagEvaluationCaseType caseType,
            String question,
            List<String> retrievedContexts,
            String answer,
            String referenceAnswer) {
    	
        if (caseType == null) {
            throw new NullPointerException("caseType must not be null");
        }

        this.caseType = caseType;
        this.question = requireText(question, "question");
        this.retrievedContexts = normalizeContexts(retrievedContexts);
        this.answer = requireText(answer, "answer");
        this.referenceAnswer = requireText(referenceAnswer, "referenceAnswer");
    }

    public RagEvaluationCaseType getCaseType() {
        return caseType;
    }

    public boolean isNoAnswerExpected() {
        return caseType == RagEvaluationCaseType.NO_ANSWER;
    }
    
    public String getQuestion() {
        return question;
    }

    public List<String> getRetrievedContexts() {
        return retrievedContexts;
    }

    public String getAnswer() {
        return answer;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    private static List<String> normalizeContexts(List<String> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return Collections.emptyList();
        }

        return contexts.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "RagEvaluationContext{" +
                "question='" + question + '\'' +
                ", retrievedContexts=" + retrievedContexts.size() +
                ", answer='" + answer + '\'' +
                '}';
    }
}