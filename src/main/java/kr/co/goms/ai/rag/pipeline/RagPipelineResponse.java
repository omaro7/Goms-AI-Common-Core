/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.pipeline;

import java.util.Objects;

import kr.co.goms.ai.llm.LlmResponse;
import kr.co.goms.ai.rag.RagResponse;
import kr.co.goms.ai.rag.context.RagContext;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;

/**
 * RAG 검색과 LLM 실행이 완료된 최종 파이프라인 응답입니다.
 */
public final class RagPipelineResponse {

    /**
     * 사용자 원본 요청입니다.
     */
    private final String userPrompt;

    /**
     * 문서 검색에 사용한 질의입니다.
     */
    private final String retrievalQuery;

    /**
     * RAG로 증강된 프롬프트입니다.
     */
    private final String augmentedPrompt;

    /**
     * LLM이 생성한 최종 답변입니다.
     */
    private final String answer;

    /**
     * RAG 서비스 처리 결과입니다.
     */
    private final RagResponse ragResponse;

    /**
     * LLM 원본 응답입니다.
     */
    private final LlmResponse llmResponse;

    /**
     * 사용된 Chat/Agent 모델명입니다.
     */
    private final String chatModel;

    /**
     * 전체 파이프라인 처리 시간입니다.
     *
     * 나노초 단위입니다.
     */
    private final long durationNanos;

    /**
     * LLM 호출 처리 시간입니다.
     *
     * 나노초 단위입니다.
     */
    private final long llmDurationNanos;

    /**
     * 결과 생성 시각입니다.
     *
     * Epoch milliseconds 단위입니다.
     */
    private final long createdAt;

    private RagPipelineResponse(
        Builder builder
    ) {
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

        this.answer = requireText(
            builder.answer,
            "answer"
        );

        this.ragResponse =
            Objects.requireNonNull(
                builder.ragResponse,
                "ragResponse must not be null"
            );

        this.llmResponse =
            Objects.requireNonNull(
                builder.llmResponse,
                "llmResponse must not be null"
            );

        this.chatModel = requireText(
            builder.chatModel,
            "chatModel"
        );

        this.durationNanos =
            validateNonNegative(
                builder.durationNanos,
                "durationNanos"
            );

        this.llmDurationNanos =
            validateNonNegative(
                builder.llmDurationNanos,
                "llmDurationNanos"
            );

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

    public String getAnswer() {
        return answer;
    }

    public RagResponse getRagResponse() {
        return ragResponse;
    }

    public LlmResponse getLlmResponse() {
        return llmResponse;
    }

    public String getChatModel() {
        return chatModel;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public double getDurationMillis() {
        return durationNanos
            / 1_000_000.0;
    }

    public long getLlmDurationNanos() {
        return llmDurationNanos;
    }

    public double getLlmDurationMillis() {
        return llmDurationNanos
            / 1_000_000.0;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public RetrievalResult getRetrievalResult() {
        return ragResponse
            .getRetrievalResult();
    }

    public RagContext getRagContext() {
        return ragResponse
            .getRagContext();
    }

    public boolean hasSources() {
        return ragResponse.hasSources();
    }

    public int getSourceCount() {
        return ragResponse.getSourceCount();
    }

    public boolean isContextApplied() {
        return ragResponse.isContextApplied();
    }

    public boolean isContextTruncated() {
        return ragResponse
            .isContextTruncated();
    }

    private static long validateNonNegative(
        long value,
        String fieldName
    ) {
        if (value < 0L) {
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
        return "RagPipelineResponse{" +
            "retrievalQuery='" + retrievalQuery + '\'' +
            ", chatModel='" + chatModel + '\'' +
            ", sourceCount=" + getSourceCount() +
            ", contextApplied=" + isContextApplied() +
            ", contextTruncated=" + isContextTruncated() +
            ", answerLength=" + answer.length() +
            ", durationNanos=" + durationNanos +
            ", llmDurationNanos=" + llmDurationNanos +
            '}';
    }

    public static final class Builder {

        private String userPrompt;
        private String retrievalQuery;
        private String augmentedPrompt;
        private String answer;
        private RagResponse ragResponse;
        private LlmResponse llmResponse;
        private String chatModel;
        private long durationNanos;
        private long llmDurationNanos;
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

        public Builder answer(
            String answer
        ) {
            this.answer = answer;
            return this;
        }

        public Builder ragResponse(
            RagResponse ragResponse
        ) {
            this.ragResponse =
                ragResponse;

            return this;
        }

        public Builder llmResponse(
            LlmResponse llmResponse
        ) {
            this.llmResponse =
                llmResponse;

            return this;
        }

        public Builder chatModel(
            String chatModel
        ) {
            this.chatModel =
                chatModel;

            return this;
        }

        public Builder durationNanos(
            long durationNanos
        ) {
            this.durationNanos =
                durationNanos;

            return this;
        }

        public Builder llmDurationNanos(
            long llmDurationNanos
        ) {
            this.llmDurationNanos =
                llmDurationNanos;

            return this;
        }

        public Builder createdAt(
            long createdAt
        ) {
            this.createdAt = createdAt;
            return this;
        }

        public RagPipelineResponse build() {
            return new RagPipelineResponse(
                this
            );
        }
    }
}