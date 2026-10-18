/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag;

import java.util.Objects;

import kr.co.goms.ai.rag.context.RagContext;
import kr.co.goms.ai.rag.context.RagContextBuilder;
import kr.co.goms.ai.rag.prompt.PromptAugmentationException;
import kr.co.goms.ai.rag.prompt.PromptAugmentor;
import kr.co.goms.ai.rag.retrieval.RetrievalException;
import kr.co.goms.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;
import kr.co.goms.ai.rag.retrieval.Retriever;

/**
 * Retriever, RagContextBuilder, PromptAugmentor를 연결하는
 * 기본 {@link RagService} 구현체입니다.
 *
 * <pre>
 * 사용자 요청
 *      ↓
 * DefaultRagService
 *      ├─ Retriever
 *      ├─ RagContextBuilder
 *      └─ PromptAugmentor
 *      ↓
 * RagResponse
 * </pre>
 */
public final class DefaultRagService implements RagService {

    private final Retriever retriever;
    private final RagContextBuilder ragContextBuilder;
    private final PromptAugmentor promptAugmentor;

    /**
     * 기본 RAG 서비스를 생성합니다.
     *
     * @param retriever 관련 문서 검색기
     * @param ragContextBuilder 검색 결과 컨텍스트 생성기
     * @param promptAugmentor 사용자 프롬프트 증강기
     */
    public DefaultRagService(
        Retriever retriever,
        RagContextBuilder ragContextBuilder,
        PromptAugmentor promptAugmentor
    ) {
        this.retriever = Objects.requireNonNull(
            retriever,
            "retriever must not be null"
        );

        this.ragContextBuilder = Objects.requireNonNull(
            ragContextBuilder,
            "ragContextBuilder must not be null"
        );

        this.promptAugmentor = Objects.requireNonNull(
            promptAugmentor,
            "promptAugmentor must not be null"
        );
    }

    /**
     * 사용자 프롬프트를 검색 질의로도 사용하여 증강합니다.
     */
    @Override
    public RagResponse augment(
        String userPrompt
    ) throws RagException {

        String normalizedPrompt =
            validatePrompt(userPrompt);

        RetrievalRequest retrievalRequest =
            RetrievalRequest.of(
                normalizedPrompt
            );

        return augment(
            normalizedPrompt,
            retrievalRequest
        );
    }

    /**
     * 별도의 검색 조건을 적용하여 사용자 프롬프트를 증강합니다.
     */
    @Override
    public RagResponse augment(
        String userPrompt,
        RetrievalRequest retrievalRequest
    ) throws RagException {

        String normalizedPrompt =
            validatePrompt(userPrompt);

        validateRetrievalRequest(
            retrievalRequest
        );

        String retrievalQuery =
            retrievalRequest.getQuery();

        long startedAt =
            System.nanoTime();

        try {
            RetrievalResult retrievalResult =
                retrieveInternal(
                    retrievalRequest
                );

            RagContext ragContext =
                buildContextInternal(
                    retrievalResult
                );

            String augmentedPrompt =
                augmentPromptInternal(
                    normalizedPrompt,
                    ragContext
                );

            return RagResponse.builder()
                .userPrompt(
                    normalizedPrompt
                )
                .retrievalQuery(
                    retrievalQuery
                )
                .augmentedPrompt(
                    augmentedPrompt
                )
                .retrievalResult(
                    retrievalResult
                )
                .ragContext(
                    ragContext
                )
                .durationNanos(
                    System.nanoTime()
                        - startedAt
                )
                .contextApplied(
                    ragContext.hasContext()
                )
                .createdAt(
                    System.currentTimeMillis()
                )
                .build();

        } catch (RagException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new RagException(
                "Unexpected error while processing RAG request",
                RagOperation.BUILD_RESPONSE,
                retrievalQuery,
                exception
            );
        }
    }

    /**
     * 기본 검색 조건으로 RAG 컨텍스트를 생성합니다.
     */
    @Override
    public RagContext buildContext(
        String query
    ) throws RagException {

        String normalizedQuery =
            validateQuery(query);

        RetrievalRequest request =
            RetrievalRequest.of(
                normalizedQuery
            );

        return buildContext(request);
    }

    /**
     * 지정된 검색 조건으로 RAG 컨텍스트를 생성합니다.
     */
    @Override
    public RagContext buildContext(
        RetrievalRequest retrievalRequest
    ) throws RagException {

        validateRetrievalRequest(
            retrievalRequest
        );

        RetrievalResult retrievalResult =
            retrieveInternal(
                retrievalRequest
            );

        return buildContextInternal(
            retrievalResult
        );
    }

    /**
     * 기본 검색 조건으로 관련 문서를 검색합니다.
     */
    @Override
    public RetrievalResult retrieve(
        String query
    ) throws RagException {

        String normalizedQuery =
            validateQuery(query);

        try {
            return retriever.retrieve(
                normalizedQuery
            );

        } catch (RetrievalException exception) {
            throw new RagException(
                "Failed to retrieve related documents",
                RagOperation.RETRIEVE,
                normalizedQuery,
                exception
            );

        } catch (RuntimeException exception) {
            throw new RagException(
                "Unexpected error while retrieving documents",
                RagOperation.RETRIEVE,
                normalizedQuery,
                exception
            );
        }
    }

    /**
     * 지정된 조건으로 관련 문서를 검색합니다.
     */
    @Override
    public RetrievalResult retrieve(
        RetrievalRequest retrievalRequest
    ) throws RagException {

        validateRetrievalRequest(
            retrievalRequest
        );

        return retrieveInternal(
            retrievalRequest
        );
    }

    /**
     * Retriever와 VectorStore 상태를 확인합니다.
     */
    @Override
    public boolean isAvailable() {
        try {
            return retriever.isAvailable();

        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * RetrievalRequest를 사용해 검색합니다.
     */
    private RetrievalResult retrieveInternal(
        RetrievalRequest retrievalRequest
    ) throws RagException {

        try {
            RetrievalResult result =
                retriever.retrieve(
                    retrievalRequest
                );

            if (result == null) {
                throw new RagException(
                    "Retriever returned null result",
                    RagOperation.RETRIEVE,
                    retrievalRequest.getQuery(),
                    null
                );
            }

            return result;

        } catch (RetrievalException exception) {
            throw new RagException(
                "Failed to retrieve related documents",
                RagOperation.RETRIEVE,
                retrievalRequest.getQuery(),
                exception
            );

        } catch (RagException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new RagException(
                "Unexpected error while retrieving documents",
                RagOperation.RETRIEVE,
                retrievalRequest.getQuery(),
                exception
            );
        }
    }

    /**
     * 검색 결과를 출처 단위 제한이 적용된 RagContext로 변환합니다.
     */
    private RagContext buildContextInternal(
        RetrievalResult retrievalResult
    ) throws RagException {

        try {
            RagContext ragContext =
                ragContextBuilder.build(
                    retrievalResult
                );

            if (ragContext == null) {
                throw new RagException(
                    "RagContextBuilder returned null context",
                    RagOperation.BUILD_CONTEXT,
                    retrievalResult.getQuery(),
                    null
                );
            }

            return ragContext;

        } catch (RagException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new RagException(
                "Failed to build RAG context",
                RagOperation.BUILD_CONTEXT,
                retrievalResult.getQuery(),
                exception
            );
        }
    }

    /**
     * 사용자 요청에 RAG 컨텍스트를 결합합니다.
     */
    private String augmentPromptInternal(
        String userPrompt,
        RagContext ragContext
    ) throws RagException {

        try {
            String augmentedPrompt =
                promptAugmentor.augment(
                    userPrompt,
                    ragContext
                );

            if (augmentedPrompt == null
                || augmentedPrompt.isBlank()) {

                throw new RagException(
                    "PromptAugmentor returned an empty prompt",
                    RagOperation.AUGMENT_PROMPT,
                    ragContext == null
                        ? ""
                        : ragContext.getQuery(),
                    null
                );
            }

            return augmentedPrompt;

        } catch (PromptAugmentationException exception) {
            throw new RagException(
                "Failed to augment user prompt",
                RagOperation.AUGMENT_PROMPT,
                ragContext == null
                    ? ""
                    : ragContext.getQuery(),
                exception
            );

        } catch (RagException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new RagException(
                "Unexpected error while augmenting prompt",
                RagOperation.AUGMENT_PROMPT,
                ragContext == null
                    ? ""
                    : ragContext.getQuery(),
                exception
            );
        }
    }

    private String validatePrompt(
        String userPrompt
    ) throws RagException {

        try {
            return validateUserPrompt(
                userPrompt
            );

        } catch (IllegalArgumentException exception) {
            throw new RagException(
                "User prompt is invalid",
                RagOperation.VALIDATE,
                "",
                exception
            );
        }
    }

    private String validateQuery(
        String query
    ) throws RagException {

        String normalized =
            query == null
                ? ""
                : query
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .trim();

        if (normalized.isBlank()) {
            throw new RagException(
                "Retrieval query must not be blank",
                RagOperation.VALIDATE,
                "",
                null
            );
        }

        return normalized;
    }

    private void validateRetrievalRequest(
        RetrievalRequest retrievalRequest
    ) throws RagException {

        if (retrievalRequest == null) {
            throw new RagException(
                "RetrievalRequest must not be null",
                RagOperation.VALIDATE,
                "",
                null
            );
        }

        validateQuery(
            retrievalRequest.getQuery()
        );

        if (retrievalRequest.getTopK() < 1) {
            throw new RagException(
                "Retrieval topK must be greater than zero",
                RagOperation.VALIDATE,
                retrievalRequest.getQuery(),
                null
            );
        }

        if (!Double.isFinite(
            retrievalRequest.getMinimumScore()
        )) {
            throw new RagException(
                "Retrieval minimumScore must be finite",
                RagOperation.VALIDATE,
                retrievalRequest.getQuery(),
                null
            );
        }

        if (retrievalRequest.getSimilarityType()
            == null) {

            throw new RagException(
                "Retrieval similarityType must not be null",
                RagOperation.VALIDATE,
                retrievalRequest.getQuery(),
                null
            );
        }
    }

    public Retriever getRetriever() {
        return retriever;
    }

    public RagContextBuilder getRagContextBuilder() {
        return ragContextBuilder;
    }

    public PromptAugmentor getPromptAugmentor() {
        return promptAugmentor;
    }

    @Override
    public String toString() {
        return "DefaultRagService{" +
            "retriever="
                + retriever.getClass().getSimpleName() +
            ", ragContextBuilder="
                + ragContextBuilder +
            ", promptAugmentor="
                + promptAugmentor.getClass().getSimpleName() +
            '}';
    }
}