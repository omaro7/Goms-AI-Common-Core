/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.prompt;

import java.util.Objects;

import kr.co.goms.ai.rag.context.RagContext;

/**
 * 사용자 요청과 RAG 컨텍스트를 결합하는 기본
 * {@link PromptAugmentor} 구현체입니다.
 *
 * <p>최종 프롬프트는 다음 영역으로 구성됩니다.</p>
 *
 * <ol>
 *     <li>RAG 응답 지침</li>
 *     <li>검색된 프로젝트 참고 문서</li>
 *     <li>사용자 요청</li>
 *     <li>최종 응답 규칙</li>
 * </ol>
 */
public final class DefaultPromptAugmentor
    implements PromptAugmentor {

    public static final int DEFAULT_MAXIMUM_PROMPT_CHARACTERS =
        24_000;

    public static final int DEFAULT_MAXIMUM_CONTEXT_CHARACTERS =
        18_000;

    private static final String DEFAULT_SYSTEM_INSTRUCTION = """
        당신은 GomsBook Editor에 통합된 EPUB 제작 AI Agent입니다.

        아래 참고 문서는 현재 EPUB 프로젝트에서 검색된 실제 자료입니다.
        참고 문서에 포함된 내용을 우선하여 사용하십시오.

        참고 문서에 없는 내용을 사실처럼 만들어 내지 마십시오.
        문서 내용만으로 판단할 수 없는 경우에는 확인할 수 없다고 명확히 답하십시오.
        XHTML, EPUB, 접근성 또는 메타데이터를 다룰 때는 원본 구조와 식별자를 보존하십시오.
        """;

    private static final String DEFAULT_RESPONSE_INSTRUCTION = """
        응답 규칙:
        1. 사용자 요청에 직접 답하십시오.
        2. 참고 문서에 근거한 내용과 일반적인 제안을 구분하십시오.
        3. 문서 위치를 언급할 때는 파일 경로와 요소 ID를 사용하십시오.
        4. 원문 수정이 필요한 경우 변경 대상과 변경 이유를 명확히 제시하십시오.
        5. 참고 문서가 부족하면 임의로 추측하지 마십시오.
        """;

    /**
     * 최종 프롬프트의 최대 문자 수입니다.
     *
     * 0이면 제한하지 않습니다.
     */
    private final int maximumPromptCharacters;

    /**
     * RAG 컨텍스트에 사용할 최대 문자 수입니다.
     *
     * 0이면 컨텍스트를 자르지 않습니다.
     */
    private final int maximumContextCharacters;

    /**
     * 검색 결과가 없는 경우에도 사용자 요청을 실행할지 여부입니다.
     */
    private final boolean allowEmptyContext;

    /**
     * 검색 점수와 출처 정보를 프롬프트에 포함할지 여부입니다.
     *
     * 현재 RagContext가 이미 출처 정보를 포함하므로 기본값은 true입니다.
     */
    private final boolean includeSources;

    /**
     * 프롬프트 앞부분에 삽입할 기본 지침입니다.
     */
    private final String systemInstruction;

    /**
     * 프롬프트 마지막에 삽입할 응답 규칙입니다.
     */
    private final String responseInstruction;

    public DefaultPromptAugmentor() {
        this(
            DEFAULT_MAXIMUM_PROMPT_CHARACTERS,
            DEFAULT_MAXIMUM_CONTEXT_CHARACTERS,
            true,
            true,
            DEFAULT_SYSTEM_INSTRUCTION,
            DEFAULT_RESPONSE_INSTRUCTION
        );
    }

    public DefaultPromptAugmentor(
        int maximumPromptCharacters,
        int maximumContextCharacters
    ) {
        this(
            maximumPromptCharacters,
            maximumContextCharacters,
            true,
            true,
            DEFAULT_SYSTEM_INSTRUCTION,
            DEFAULT_RESPONSE_INSTRUCTION
        );
    }

    public DefaultPromptAugmentor(
        int maximumPromptCharacters,
        int maximumContextCharacters,
        boolean allowEmptyContext,
        boolean includeSources,
        String systemInstruction,
        String responseInstruction
    ) {
        this.maximumPromptCharacters =
            validateMaximumCharacters(
                maximumPromptCharacters,
                "maximumPromptCharacters"
            );

        this.maximumContextCharacters =
            validateMaximumCharacters(
                maximumContextCharacters,
                "maximumContextCharacters"
            );

        if (maximumPromptCharacters > 0
            && maximumContextCharacters > maximumPromptCharacters) {

            throw new IllegalArgumentException(
                "maximumContextCharacters must not exceed "
                    + "maximumPromptCharacters"
            );
        }

        this.allowEmptyContext = allowEmptyContext;
        this.includeSources = includeSources;

        this.systemInstruction =
            normalizeMultiline(systemInstruction);

        this.responseInstruction =
            normalizeMultiline(responseInstruction);
    }

    @Override
    public String augment(
        String userPrompt,
        RagContext ragContext
    ) throws PromptAugmentationException {

        String normalizedUserPrompt =
            validateUserPrompt(userPrompt);

        validateContext(ragContext);

        try {
            RagContext preparedContext =
                prepareContext(ragContext);

            String contextBlock =
                createContextBlock(preparedContext);

            String augmentedPrompt =
                buildPrompt(
                    normalizedUserPrompt,
                    contextBlock,
                    preparedContext
                );

            augmentedPrompt =
                enforceMaximumPromptLength(
                    normalizedUserPrompt,
                    preparedContext,
                    augmentedPrompt
                );

            validatePromptLength(augmentedPrompt);

            return augmentedPrompt;

        } catch (PromptAugmentationException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new PromptAugmentationException(
                "Failed to augment prompt",
                PromptAugmentationOperation.AUGMENT,
                ragContext == null
                    ? ""
                    : ragContext.getQuery(),
                exception
            );
        }
    }

    /**
     * 검색 결과가 없을 때는 일반 사용자 프롬프트 형식으로 구성합니다.
     */
    @Override
    public String augment(
        String userPrompt
    ) throws PromptAugmentationException {

        String normalizedUserPrompt =
            validateUserPrompt(userPrompt);

        String prompt = buildPrompt(
            normalizedUserPrompt,
            createEmptyContextBlock(),
            null
        );

        validatePromptLength(prompt);

        return prompt;
    }

    @Override
    public boolean allowEmptyContext() {
        return allowEmptyContext;
    }

    @Override
    public int getMaximumPromptCharacters() {
        return maximumPromptCharacters;
    }

    /**
     * 컨텍스트 최대 길이를 적용합니다.
     */
    private RagContext prepareContext(
    	    RagContext ragContext
    	) throws PromptAugmentationException {

	    if (ragContext == null
	        || ragContext.isEmpty()) {

	        return ragContext;
	    }

	    /*
	     * 최대 컨텍스트 길이는 RagContextBuilder에서
	     * 출처 단위로 이미 적용합니다.
	     */
	    if (maximumContextCharacters > 0
	        && ragContext.getCharacterCount()
	            > maximumContextCharacters) {

	        throw new PromptAugmentationException(
	            "RAG context exceeds the configured maximum length. "
	                + "Build the context through RagContextBuilder. "
	                + "maximum="
	                + maximumContextCharacters
	                + ", actual="
	                + ragContext.getCharacterCount(),
	            PromptAugmentationOperation.LENGTH_CHECK,
	            ragContext.getQuery(),
	            null
	        );
	    }

	    return ragContext;
	}
    /**
     * 참고 문서 영역을 생성합니다.
     */
    private String createContextBlock(
        RagContext ragContext
    ) throws PromptAugmentationException {

        if (ragContext == null || ragContext.isEmpty()) {
            if (!allowEmptyContext) {
                throw new PromptAugmentationException(
                    "RAG context is empty",
                    PromptAugmentationOperation.FORMAT_CONTEXT
                );
            }

            return createEmptyContextBlock();
        }

        StringBuilder builder =
            new StringBuilder();

        builder.append("[프로젝트 참고 문서]\n");

        if (includeSources) {
            builder.append(
                ragContext.getContextText()
            );
        } else {
            appendContentsOnly(
                builder,
                ragContext
            );
        }

        if (ragContext.isTruncated()) {
            builder.append("\n\n")
                .append(
                    "[안내] 참고 문서가 최대 길이에 맞게 일부 생략되었습니다."
                );
        }

        return builder.toString().trim();
    }

    /**
     * 파일명, 점수 등의 출처 표시 없이 Chunk 본문만 구성합니다.
     */
    private void appendContentsOnly(
        StringBuilder builder,
        RagContext ragContext
    ) {
        for (int index = 0;
             index < ragContext.getSources().size();
             index++) {

            if (index > 0) {
                builder.append("\n\n");
            }

            builder.append("[문서 ")
                .append(index + 1)
                .append("]\n")
                .append(
                    ragContext.getSources()
                        .get(index)
                        .getContent()
                );
        }
    }

    private String createEmptyContextBlock() {
        return """
            [프로젝트 참고 문서]
            현재 질의와 관련된 프로젝트 문서를 찾지 못했습니다.
            프로젝트에 존재하는 사실이나 내용을 추측하지 마십시오.
            일반적인 설명이 가능한 경우에는 일반 지식 또는 제안임을 명시하십시오.
            """.trim();
    }

    /**
     * 최종 프롬프트를 조립합니다.
     */
    private String buildPrompt(
        String userPrompt,
        String contextBlock,
        RagContext ragContext
    ) {
        StringBuilder builder =
            new StringBuilder();

        if (!systemInstruction.isBlank()) {
            builder.append("[역할 및 지침]\n")
                .append(systemInstruction)
                .append("\n\n");
        }

        builder.append(contextBlock)
            .append("\n\n");

        builder.append("[사용자 요청]\n")
            .append(userPrompt);

        if (!responseInstruction.isBlank()) {
            builder.append("\n\n")
                .append(responseInstruction);
        }

        if (ragContext != null
            && ragContext.hasContext()) {

            builder.append("\n\n")
                .append(
                    "답변을 작성할 때 필요한 경우 "
                )
                .append(
                    "[출처 N] 형식으로 근거를 표시하십시오."
                );
        }

        return normalizeMultiline(
            builder.toString()
        );
    }

    /**
     * 최종 프롬프트가 최대 길이를 초과하면 컨텍스트를 추가로 줄입니다.
     *
     * 사용자 요청과 필수 지침은 유지하고 참고 문서를 우선 축소합니다.
     */
    private String enforceMaximumPromptLength(
    	    String userPrompt,
    	    RagContext ragContext,
    	    String prompt
    	) throws PromptAugmentationException {

	    if (maximumPromptCharacters <= 0
	        || prompt.length()
	            <= maximumPromptCharacters) {

	        return prompt;
	    }

	    throw new PromptAugmentationException(
	        "Augmented prompt exceeds maximum length. "
	            + "Reduce RagContextBuilder.maximumContextCharacters. "
	            + "maximum="
	            + maximumPromptCharacters
	            + ", actual="
	            + prompt.length(),
	        PromptAugmentationOperation.LENGTH_CHECK,
	        ragContext == null
	            ? ""
	            : ragContext.getQuery(),
	        null
	    );
	}

    private PromptAugmentationException
        createLengthException(
            int actualLength
        ) {

        return new PromptAugmentationException(
            "Augmented prompt exceeds maximum length. "
                + "maximum="
                + maximumPromptCharacters
                + ", actual="
                + actualLength,
            PromptAugmentationOperation.LENGTH_CHECK
        );
    }

    private static int validateMaximumCharacters(
        int value,
        String fieldName
    ) {
        if (value < 0) {
            throw new IllegalArgumentException(
                fieldName
                    + " must be greater than or equal to zero"
            );
        }

        return value;
    }

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
            .replaceAll("\\n[\\t ]+", "\n")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    public int getMaximumContextCharacters() {
        return maximumContextCharacters;
    }

    public boolean isIncludeSources() {
        return includeSources;
    }

    public String getSystemInstruction() {
        return systemInstruction;
    }

    public String getResponseInstruction() {
        return responseInstruction;
    }

    @Override
    public String toString() {
        return "DefaultPromptAugmentor{" +
            "maximumPromptCharacters="
                + maximumPromptCharacters +
            ", maximumContextCharacters="
                + maximumContextCharacters +
            ", allowEmptyContext="
                + allowEmptyContext +
            ", includeSources="
                + includeSources +
            '}';
    }
}