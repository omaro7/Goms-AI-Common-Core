/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.prompt;

import java.util.Objects;

import kr.co.goms.ai.rag.context.RagContext;

/**
 * 사용자 프롬프트에 RAG 검색 컨텍스트를 결합하는 인터페이스입니다.
 *
 * <pre>
 * 사용자 요청
 *     +
 * RagContext
 *     ↓
 * PromptAugmentor
 *     ↓
 * 최종 LLM 프롬프트
 * </pre>
 *
 * <p>이 인터페이스는 문자열 결합만 담당합니다. 실제 문서 검색은
 * Retriever가 담당하고, LLM 호출은 AgentExecutor 또는 LlmClient가
 * 담당합니다.</p>
 */
public interface PromptAugmentor {

    /**
     * 사용자 프롬프트와 RAG 컨텍스트를 결합합니다.
     *
     * @param userPrompt 사용자 요청
     * @param ragContext 검색된 RAG 컨텍스트
     * @return LLM에 전달할 최종 프롬프트
     * @throws PromptAugmentationException 프롬프트 생성 실패 시
     */
    String augment(
        String userPrompt,
        RagContext ragContext
    ) throws PromptAugmentationException;

    /**
     * RAG 컨텍스트가 없는 사용자 프롬프트를 처리합니다.
     *
     * <p>기본 구현은 사용자 프롬프트만 검증한 뒤 그대로 반환합니다.</p>
     *
     * @param userPrompt 사용자 요청
     * @return 검증된 사용자 프롬프트
     * @throws PromptAugmentationException 프롬프트 생성 실패 시
     */
    default String augment(
        String userPrompt
    ) throws PromptAugmentationException {

        return validateUserPrompt(userPrompt);
    }

    /**
     * 검색 결과가 없을 때 RAG 없이 원본 프롬프트를 사용할지 결정합니다.
     *
     * <p>기본값은 true입니다. 검색 결과가 없다는 이유로 Agent 실행을
     * 중단하지 않고 일반 LLM 요청으로 계속 진행합니다.</p>
     *
     * @return 빈 컨텍스트 허용 여부
     */
    default boolean allowEmptyContext() {
        return true;
    }

    /**
     * 사용자 프롬프트를 검증하고 정규화합니다.
     *
     * @param userPrompt 사용자 요청
     * @return 정규화된 사용자 요청
     * @throws PromptAugmentationException 프롬프트가 비어 있는 경우
     */
    default String validateUserPrompt(
        String userPrompt
    ) throws PromptAugmentationException {

        String normalized =
            normalizeMultiline(userPrompt);

        if (normalized.isBlank()) {
            throw new PromptAugmentationException(
                "User prompt must not be blank",
                PromptAugmentationOperation.VALIDATE
            );
        }

        return normalized;
    }

    /**
     * RAG 컨텍스트를 검증합니다.
     *
     * @param ragContext 검증할 컨텍스트
     * @throws PromptAugmentationException 사용할 수 없는 컨텍스트인 경우
     */
    default void validateContext(
        RagContext ragContext
    ) throws PromptAugmentationException {

        if (ragContext == null) {
            if (allowEmptyContext()) {
                return;
            }

            throw new PromptAugmentationException(
                "RAG context must not be null",
                PromptAugmentationOperation.VALIDATE
            );
        }

        if (ragContext.isEmpty()
            && !allowEmptyContext()) {

            throw new PromptAugmentationException(
                "RAG context must not be empty",
                PromptAugmentationOperation.VALIDATE
            );
        }
    }

    /**
     * 사용자 프롬프트와 RagContext의 질의가 동일한지 확인합니다.
     *
     * <p>검색 질의와 실제 사용자 요청이 다를 수 있으므로 불일치 자체를
     * 오류로 보지는 않습니다. 진단이나 로깅에 사용할 수 있습니다.</p>
     *
     * @param userPrompt 사용자 요청
     * @param ragContext RAG 컨텍스트
     * @return 질의 일치 여부
     */
    default boolean isQueryMatched(
        String userPrompt,
        RagContext ragContext
    ) {
        if (ragContext == null) {
            return false;
        }

        String normalizedPrompt =
            normalizeForComparison(userPrompt);

        String normalizedQuery =
            normalizeForComparison(
                ragContext.getQuery()
            );

        return !normalizedPrompt.isBlank()
            && normalizedPrompt.equals(
                normalizedQuery
            );
    }

    /**
     * 프롬프트 최대 길이를 반환합니다.
     *
     * <p>0이면 별도의 문자 수 제한을 적용하지 않습니다.</p>
     *
     * @return 최대 프롬프트 문자 수
     */
    default int getMaximumPromptCharacters() {
        return 0;
    }

    /**
     * 생성된 프롬프트 길이를 검증합니다.
     *
     * @param prompt 생성된 최종 프롬프트
     * @throws PromptAugmentationException 최대 길이를 초과한 경우
     */
    default void validatePromptLength(
        String prompt
    ) throws PromptAugmentationException {

        Objects.requireNonNull(
            prompt,
            "prompt must not be null"
        );

        int maximumCharacters =
            getMaximumPromptCharacters();

        if (maximumCharacters > 0
            && prompt.length() > maximumCharacters) {

            throw new PromptAugmentationException(
                "Augmented prompt exceeds maximum length. "
                    + "maximum="
                    + maximumCharacters
                    + ", actual="
                    + prompt.length(),
                PromptAugmentationOperation.LENGTH_CHECK
            );
        }
    }

    /**
     * 여러 줄 텍스트의 개행과 불필요한 공백을 정리합니다.
     */
    private static String normalizeMultiline(
        String value
    ) {
        if (value == null) {
            return "";
        }

        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replaceAll("[\\t ]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    /**
     * 비교를 위해 공백과 대소문자를 정규화합니다.
     */
    private static String normalizeForComparison(
        String value
    ) {
        if (value == null) {
            return "";
        }

        return value
            .replaceAll("\\s+", " ")
            .trim()
            .toLowerCase(
                java.util.Locale.ROOT
            );
    }
}