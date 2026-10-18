/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag;

import java.util.Objects;

import kr.co.goms.ai.rag.context.RagContext;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;

/**
 * RagService 처리 결과입니다.
 *
 * <p>원본 사용자 프롬프트, 증강된 최종 프롬프트,
 * 검색 결과와 RAG 컨텍스트를 함께 보관합니다.</p>
 */
public final class RagResponse {

    /**
     * 사용자 원본 요청입니다.
     */
    private final String userPrompt;

    /**
     * Retriever에 전달된 실제 검색 질의입니다.
     */
    private final String retrievalQuery;

    /**
     * LLM에 전달할 최종 증강 프롬프트입니다.
     */
    private final String augmentedPrompt;

    /**
     * 원본 검색 결과입니다.
     */
    private final RetrievalResult retrievalResult;

    /**
     * 프롬프트에 사용된 RAG 컨텍스트입니다.
     */
    private final RagContext ragContext;

    /**
     * 전체 RAG 처리 시간입니다.
     *
     * 나노초 단위입니다.
     */
    private final long durationNanos;

    /**
     * RAG 컨텍스트가 실제로 사용되었는지 여부입니다.
     */
    private final boolean contextApplied;

    /**
     * 결과 생성 시각입니다.
     *
     * Epoch milliseconds 단위입니다.
     */
    private final long createdAt;

    private RagResponse(Builder builder) {
        this.userPrompt = requireText(
            builder.userPrompt,
            "userPrompt"
        );

        this.retrievalQuery = requireText(
            builder.retrievalQuery,
            "retrievalQuery"
        );

        this.augmentedPrompt = requireText(
            builder.augmentedPrompt,
            "augmentedPrompt"
        );

        this.retrievalResult =
            Objects.requireNonNull(
                builder.retrievalResult,
                "retrievalResult must not be null"
            );

        this.ragContext =
            Objects.requireNonNull(
                builder.ragContext,
                "ragContext must not be null"
            );

        this.durationNanos =
            validateNonNegative(
                builder.durationNanos,
                "durationNanos"
            );

        this.contextApplied =
            builder.contextApplied;

        this.createdAt =
            builder.createdAt <= 0
                ? System.currentTimeMillis()
                : builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public String getRetrievalQuery() {
        return retrievalQuery;
    }

    public String getAugmentedPrompt() {
        return augmentedPrompt;
    }

    public RetrievalResult getRetrievalResult() {
        return retrievalResult;
    }

    public RagContext getRagContext() {
        return ragContext;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public double getDurationMillis() {
        return durationNanos / 1_000_000.0;
    }

    public boolean isContextApplied() {
        return contextApplied;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean hasSources() {
        return !ragContext.getSources().isEmpty();
    }

    public int getSourceCount() {
        return ragContext.size();
    }

    public boolean isContextTruncated() {
        return ragContext.isTruncated();
    }

    public boolean isEmptyRetrieval() {
        return retrievalResult.isEmpty();
    }

    private static long validateNonNegative(
        long value,
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

    private static String requireText(
        String value,
        String fieldName
    ) {
        String normalized =
            value == null
                ? ""
                : value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "RagResponse{" +
            "retrievalQuery='" + retrievalQuery + '\'' +
            ", sourceCount=" + ragContext.size() +
            ", durationNanos=" + durationNanos +
            ", contextApplied=" + contextApplied +
            ", contextTruncated=" + ragContext.isTruncated() +
            ", augmentedPromptLength="
                + augmentedPrompt.length() +
            '}';
    }

    public static final class Builder {

        private String userPrompt;
        private String retrievalQuery;
        private String augmentedPrompt;
        private RetrievalResult retrievalResult;
        private RagContext ragContext;
        private long durationNanos;
        private boolean contextApplied;
        private long createdAt;

        private Builder() {
        }

        public Builder userPrompt(
            String userPrompt
        ) {
            this.userPrompt = userPrompt;
            return this;
        }

        public Builder retrievalQuery(
            String retrievalQuery
        ) {
            this.retrievalQuery =
                retrievalQuery;

            return this;
        }

        public Builder augmentedPrompt(
            String augmentedPrompt
        ) {
            this.augmentedPrompt =
                augmentedPrompt;

            return this;
        }

        public Builder retrievalResult(
            RetrievalResult retrievalResult
        ) {
            this.retrievalResult =
                retrievalResult;

            return this;
        }

        public Builder ragContext(
            RagContext ragContext
        ) {
            this.ragContext = ragContext;
            return this;
        }

        public Builder durationNanos(
            long durationNanos
        ) {
            this.durationNanos =
                durationNanos;

            return this;
        }

        public Builder contextApplied(
            boolean contextApplied
        ) {
            this.contextApplied =
                contextApplied;

            return this;
        }

        public Builder createdAt(
            long createdAt
        ) {
            this.createdAt = createdAt;
            return this;
        }

        public RagResponse build() {
            return new RagResponse(this);
        }
    }
}