/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.config;

import java.time.Duration;
import java.util.Objects;

import kr.co.goms.ai.rag.vector.VectorSimilarityType;

/**
 * GomsBook RAG 전체 설정입니다.
 *
 * <p>다음 구성요소가 이 설정을 공유할 수 있습니다.</p>
 *
 * <ul>
 *     <li>EmbeddingModelProvider</li>
 *     <li>ChatModelProvider</li>
 *     <li>DefaultRetriever</li>
 *     <li>RagContextBuilder</li>
 *     <li>DefaultPromptAugmentor</li>
 *     <li>DefaultRagIndexer</li>
 *     <li>DefaultRagPipeline</li>
 * </ul>
 */
public final class RagConfiguration {

    public static final String DEFAULT_CHAT_MODEL =
        "gemma4:31b-cloud";

    public static final String DEFAULT_EMBEDDING_MODEL =
        "nomic-embed-text";

    public static final int DEFAULT_TOP_K = 8;

    public static final double DEFAULT_MINIMUM_SCORE = 0.45;

    public static final int DEFAULT_INDEX_BATCH_SIZE = 16;

    public static final int DEFAULT_MAXIMUM_CONTEXT_CHARACTERS =
        18_000;

    public static final int DEFAULT_MAXIMUM_PROMPT_CHARACTERS =
        24_000;

    public static final int DEFAULT_MAXIMUM_OUTPUT_TOKENS = 0;

    public static final Duration DEFAULT_EMBEDDING_TIMEOUT =
        Duration.ofMinutes(2);

    public static final Duration DEFAULT_LLM_TIMEOUT =
        Duration.ofMinutes(5);

    /**
     * Agent 및 최종 답변 생성에 사용할 모델입니다.
     */
    private final String chatModel;

    /**
     * 문서와 질의 임베딩에 사용할 모델입니다.
     */
    private final String embeddingModel;

    /**
     * 검색 결과 최대 개수입니다.
     */
    private final int topK;

    /**
     * 기본 최소 검색 점수입니다.
     */
    private final double minimumScore;

    /**
     * 기본 벡터 유사도 방식입니다.
     */
    private final VectorSimilarityType similarityType;

    /**
     * 문서 인덱싱 시 한 번에 임베딩할 Chunk 개수입니다.
     */
    private final int indexBatchSize;

    /**
     * RAG 컨텍스트 최대 문자 수입니다.
     */
    private final int maximumContextCharacters;

    /**
     * 최종 증강 프롬프트 최대 문자 수입니다.
     */
    private final int maximumPromptCharacters;

    /**
     * LLM 최대 출력 토큰 수입니다.
     *
     * 0이면 모델 또는 LlmClient 기본값을 사용합니다.
     */
    private final int maximumOutputTokens;

    /**
     * 문서 및 질의 벡터의 L2 정규화 여부입니다.
     */
    private final boolean normalizeEmbeddings;

    /**
     * 임베딩 입력이 모델 Context를 초과하면 잘라낼지 여부입니다.
     */
    private final boolean truncateEmbeddingInput;

    /**
     * 내용이 변경되지 않은 기존 벡터를 재사용할지 여부입니다.
     */
    private final boolean reuseUnchangedChunks;

    /**
     * 문서 인덱싱 전에 기존 문서 벡터를 삭제할지 여부입니다.
     */
    private final boolean replaceSourceOnIndex;

    /**
     * 인덱싱 중 일부 Chunk 실패 시 다음 Chunk를 계속 처리할지 여부입니다.
     */
    private final boolean continueOnIndexError;

    /**
     * 검색 결과가 없어도 일반 LLM 요청을 계속할지 여부입니다.
     */
    private final boolean allowEmptyContext;

    /**
     * 프롬프트에 파일 경로와 검색 점수 등 출처 정보를 포함할지 여부입니다.
     */
    private final boolean includeSources;

    /**
     * 검색 결과를 원본 문서 순서로 재정렬할지 여부입니다.
     */
    private final boolean preserveDocumentOrder;

    /**
     * 임베딩 요청 제한 시간입니다.
     */
    private final Duration embeddingTimeout;

    /**
     * 최종 LLM 요청 제한 시간입니다.
     */
    private final Duration llmTimeout;

    /**
     * 인덱스 스키마 및 정책 버전입니다.
     */
    private final long indexVersion;

    private RagConfiguration(Builder builder) {
        this.chatModel = requireText(
            builder.chatModel,
            "chatModel"
        );

        this.embeddingModel = requireText(
            builder.embeddingModel,
            "embeddingModel"
        );

        this.topK = validatePositive(
            builder.topK,
            "topK"
        );

        this.similarityType =
            Objects.requireNonNullElse(
                builder.similarityType,
                VectorSimilarityType.COSINE
            );

        this.minimumScore =
            validateMinimumScore(
                builder.minimumScore,
                this.similarityType
            );

        this.indexBatchSize = validatePositive(
            builder.indexBatchSize,
            "indexBatchSize"
        );

        this.maximumContextCharacters =
            validateNonNegative(
                builder.maximumContextCharacters,
                "maximumContextCharacters"
            );

        this.maximumPromptCharacters =
            validateNonNegative(
                builder.maximumPromptCharacters,
                "maximumPromptCharacters"
            );

        if (maximumPromptCharacters > 0
            && maximumContextCharacters
                > maximumPromptCharacters) {

            throw new IllegalArgumentException(
                "maximumContextCharacters must not exceed "
                    + "maximumPromptCharacters"
            );
        }

        this.maximumOutputTokens =
            validateNonNegative(
                builder.maximumOutputTokens,
                "maximumOutputTokens"
            );

        this.normalizeEmbeddings =
            builder.normalizeEmbeddings;

        this.truncateEmbeddingInput =
            builder.truncateEmbeddingInput;

        this.reuseUnchangedChunks =
            builder.reuseUnchangedChunks;

        this.replaceSourceOnIndex =
            builder.replaceSourceOnIndex;

        this.continueOnIndexError =
            builder.continueOnIndexError;

        this.allowEmptyContext =
            builder.allowEmptyContext;

        this.includeSources =
            builder.includeSources;

        this.preserveDocumentOrder =
            builder.preserveDocumentOrder;

        this.embeddingTimeout =
            validateDuration(
                builder.embeddingTimeout,
                DEFAULT_EMBEDDING_TIMEOUT,
                "embeddingTimeout"
            );

        this.llmTimeout =
            validateDuration(
                builder.llmTimeout,
                DEFAULT_LLM_TIMEOUT,
                "llmTimeout"
            );

        this.indexVersion =
            validatePositive(
                builder.indexVersion,
                "indexVersion"
            );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RagConfiguration defaults() {
        return builder().build();
    }

    public String getChatModel() {
        return chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public int getTopK() {
        return topK;
    }

    public double getMinimumScore() {
        return minimumScore;
    }

    public VectorSimilarityType getSimilarityType() {
        return similarityType;
    }

    public int getIndexBatchSize() {
        return indexBatchSize;
    }

    public int getMaximumContextCharacters() {
        return maximumContextCharacters;
    }

    public int getMaximumPromptCharacters() {
        return maximumPromptCharacters;
    }

    public int getMaximumOutputTokens() {
        return maximumOutputTokens;
    }

    public boolean isNormalizeEmbeddings() {
        return normalizeEmbeddings;
    }

    public boolean isTruncateEmbeddingInput() {
        return truncateEmbeddingInput;
    }

    public boolean isReuseUnchangedChunks() {
        return reuseUnchangedChunks;
    }

    public boolean isReplaceSourceOnIndex() {
        return replaceSourceOnIndex;
    }

    public boolean isContinueOnIndexError() {
        return continueOnIndexError;
    }

    public boolean isAllowEmptyContext() {
        return allowEmptyContext;
    }

    public boolean isIncludeSources() {
        return includeSources;
    }

    public boolean isPreserveDocumentOrder() {
        return preserveDocumentOrder;
    }

    public Duration getEmbeddingTimeout() {
        return embeddingTimeout;
    }

    public Duration getLlmTimeout() {
        return llmTimeout;
    }

    public long getIndexVersion() {
        return indexVersion;
    }

    /**
     * 기본 RetrievalRequest 설정을 구성할 때 사용할 값입니다.
     */
    public boolean hasPromptLimit() {
        return maximumPromptCharacters > 0;
    }

    /**
     * RAG 컨텍스트 길이 제한 여부를 반환합니다.
     */
    public boolean hasContextLimit() {
        return maximumContextCharacters > 0;
    }

    /**
     * LLM 출력 토큰 제한 여부를 반환합니다.
     */
    public boolean hasOutputTokenLimit() {
        return maximumOutputTokens > 0;
    }

    /**
     * 임베딩 모델이 변경되었는지 확인합니다.
     *
     * 모델이 변경되면 기존 벡터 인덱스를 재사용하면 안 됩니다.
     */
    public boolean isEmbeddingModelChanged(
        String previousModel
    ) {
        if (previousModel == null
            || previousModel.isBlank()) {

            return true;
        }

        return !embeddingModel.equals(
            previousModel.trim()
        );
    }

    private static double validateMinimumScore(
        double minimumScore,
        VectorSimilarityType similarityType
    ) {
        if (!Double.isFinite(minimumScore)) {
            throw new IllegalArgumentException(
                "minimumScore must be finite"
            );
        }

        if (similarityType
            == VectorSimilarityType.COSINE) {

            if (minimumScore < -1.0
                || minimumScore > 1.0) {

                throw new IllegalArgumentException(
                    "minimumScore for cosine similarity must be "
                        + "between -1.0 and 1.0"
                );
            }
        }

        if (similarityType
            == VectorSimilarityType.EUCLIDEAN) {

            if (minimumScore < 0.0
                || minimumScore > 1.0) {

                throw new IllegalArgumentException(
                    "minimumScore for euclidean score must be "
                        + "between 0.0 and 1.0"
                );
            }
        }

        return minimumScore;
    }

    private static int validatePositive(
        int value,
        String fieldName
    ) {
        if (value < 1) {
            throw new IllegalArgumentException(
                fieldName + " must be greater than zero"
            );
        }

        return value;
    }

    private static long validatePositive(
        long value,
        String fieldName
    ) {
        if (value < 1L) {
            throw new IllegalArgumentException(
                fieldName + " must be greater than zero"
            );
        }

        return value;
    }

    private static int validateNonNegative(
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

    private static Duration validateDuration(
        Duration value,
        Duration defaultValue,
        String fieldName
    ) {
        Duration resolved =
            Objects.requireNonNullElse(
                value,
                defaultValue
            );

        if (resolved.isZero()
            || resolved.isNegative()) {

            throw new IllegalArgumentException(
                fieldName + " must be greater than zero"
            );
        }

        return resolved;
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
        return "RagConfiguration{" +
            "chatModel='" + chatModel + '\'' +
            ", embeddingModel='" + embeddingModel + '\'' +
            ", topK=" + topK +
            ", minimumScore=" + minimumScore +
            ", similarityType=" + similarityType +
            ", indexBatchSize=" + indexBatchSize +
            ", maximumContextCharacters="
                + maximumContextCharacters +
            ", maximumPromptCharacters="
                + maximumPromptCharacters +
            ", maximumOutputTokens="
                + maximumOutputTokens +
            ", normalizeEmbeddings="
                + normalizeEmbeddings +
            ", truncateEmbeddingInput="
                + truncateEmbeddingInput +
            ", reuseUnchangedChunks="
                + reuseUnchangedChunks +
            ", replaceSourceOnIndex="
                + replaceSourceOnIndex +
            ", continueOnIndexError="
                + continueOnIndexError +
            ", allowEmptyContext="
                + allowEmptyContext +
            ", includeSources="
                + includeSources +
            ", preserveDocumentOrder="
                + preserveDocumentOrder +
            ", embeddingTimeout="
                + embeddingTimeout +
            ", llmTimeout="
                + llmTimeout +
            ", indexVersion="
                + indexVersion +
            '}';
    }

    public static final class Builder {

        private String chatModel =
            DEFAULT_CHAT_MODEL;

        private String embeddingModel =
            DEFAULT_EMBEDDING_MODEL;

        private int topK =
            DEFAULT_TOP_K;

        private double minimumScore =
            DEFAULT_MINIMUM_SCORE;

        private VectorSimilarityType similarityType =
            VectorSimilarityType.COSINE;

        private int indexBatchSize =
            DEFAULT_INDEX_BATCH_SIZE;

        private int maximumContextCharacters =
            DEFAULT_MAXIMUM_CONTEXT_CHARACTERS;

        private int maximumPromptCharacters =
            DEFAULT_MAXIMUM_PROMPT_CHARACTERS;

        private int maximumOutputTokens =
            DEFAULT_MAXIMUM_OUTPUT_TOKENS;

        private boolean normalizeEmbeddings = true;

        private boolean truncateEmbeddingInput = true;

        private boolean reuseUnchangedChunks = true;

        private boolean replaceSourceOnIndex;

        private boolean continueOnIndexError;

        private boolean allowEmptyContext = true;

        private boolean includeSources = true;

        private boolean preserveDocumentOrder;

        private Duration embeddingTimeout =
            DEFAULT_EMBEDDING_TIMEOUT;

        private Duration llmTimeout =
            DEFAULT_LLM_TIMEOUT;

        private long indexVersion = 1L;

        private Builder() {
        }

        public Builder chatModel(
            String chatModel
        ) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder embeddingModel(
            String embeddingModel
        ) {
            this.embeddingModel =
                embeddingModel;

            return this;
        }

        public Builder topK(
            int topK
        ) {
            this.topK = topK;
            return this;
        }

        public Builder minimumScore(
            double minimumScore
        ) {
            this.minimumScore =
                minimumScore;

            return this;
        }

        public Builder similarityType(
            VectorSimilarityType similarityType
        ) {
            this.similarityType =
                similarityType;

            return this;
        }

        public Builder indexBatchSize(
            int indexBatchSize
        ) {
            this.indexBatchSize =
                indexBatchSize;

            return this;
        }

        public Builder maximumContextCharacters(
            int maximumContextCharacters
        ) {
            this.maximumContextCharacters =
                maximumContextCharacters;

            return this;
        }

        public Builder maximumPromptCharacters(
            int maximumPromptCharacters
        ) {
            this.maximumPromptCharacters =
                maximumPromptCharacters;

            return this;
        }

        public Builder maximumOutputTokens(
            int maximumOutputTokens
        ) {
            this.maximumOutputTokens =
                maximumOutputTokens;

            return this;
        }

        public Builder normalizeEmbeddings(
            boolean normalizeEmbeddings
        ) {
            this.normalizeEmbeddings =
                normalizeEmbeddings;

            return this;
        }

        public Builder truncateEmbeddingInput(
            boolean truncateEmbeddingInput
        ) {
            this.truncateEmbeddingInput =
                truncateEmbeddingInput;

            return this;
        }

        public Builder reuseUnchangedChunks(
            boolean reuseUnchangedChunks
        ) {
            this.reuseUnchangedChunks =
                reuseUnchangedChunks;

            return this;
        }

        public Builder replaceSourceOnIndex(
            boolean replaceSourceOnIndex
        ) {
            this.replaceSourceOnIndex =
                replaceSourceOnIndex;

            return this;
        }

        public Builder continueOnIndexError(
            boolean continueOnIndexError
        ) {
            this.continueOnIndexError =
                continueOnIndexError;

            return this;
        }

        public Builder allowEmptyContext(
            boolean allowEmptyContext
        ) {
            this.allowEmptyContext =
                allowEmptyContext;

            return this;
        }

        public Builder includeSources(
            boolean includeSources
        ) {
            this.includeSources =
                includeSources;

            return this;
        }

        public Builder preserveDocumentOrder(
            boolean preserveDocumentOrder
        ) {
            this.preserveDocumentOrder =
                preserveDocumentOrder;

            return this;
        }

        public Builder embeddingTimeout(
            Duration embeddingTimeout
        ) {
            this.embeddingTimeout =
                embeddingTimeout;

            return this;
        }

        public Builder llmTimeout(
            Duration llmTimeout
        ) {
            this.llmTimeout =
                llmTimeout;

            return this;
        }

        public Builder indexVersion(
            long indexVersion
        ) {
            this.indexVersion =
                indexVersion;

            return this;
        }

        public RagConfiguration build() {
            return new RagConfiguration(this);
        }
    }
}