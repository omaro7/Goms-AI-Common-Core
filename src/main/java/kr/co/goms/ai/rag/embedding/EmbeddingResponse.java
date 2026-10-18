/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.embedding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 임베딩 모델 실행 결과입니다.
 *
 * <p>단일 입력 및 배치 입력의 임베딩 결과를 모두 지원합니다.</p>
 *
 * <pre>
 * EmbeddingResponse response = embeddingClient.embed(request);
 *
 * float[] vector = response.getEmbedding();
 * int dimensions = response.getDimensions();
 * </pre>
 */
public final class EmbeddingResponse {

    /**
     * 실제 응답에 사용된 모델명.
     */
    private final String model;

    /**
     * 입력별 임베딩 벡터 목록.
     *
     * 요청 입력 순서와 동일한 순서를 유지합니다.
     */
    private final List<float[]> embeddings;

    /**
     * 임베딩 벡터 차원.
     *
     * 응답이 비어 있으면 0입니다.
     */
    private final int dimensions;

    /**
     * 요청 추적용 식별자.
     */
    private final String requestId;

    /**
     * 입력 토큰 수.
     *
     * 서버가 값을 제공하지 않으면 0입니다.
     */
    private final long inputTokenCount;

    /**
     * 전체 처리 시간.
     *
     * 나노초 단위이며 값을 알 수 없으면 0입니다.
     */
    private final long totalDurationNanos;

    /**
     * 모델 로딩 시간.
     *
     * 나노초 단위이며 값을 알 수 없으면 0입니다.
     */
    private final long loadDurationNanos;

    /**
     * 응답 생성 시각.
     *
     * Epoch milliseconds이며 값을 알 수 없으면 0입니다.
     */
    private final long createdAt;

    /**
     * 응답 벡터가 정규화되었는지 여부.
     */
    private final boolean normalized;

    private EmbeddingResponse(Builder builder) {
        this.model = requireText(builder.model, "model");
        this.embeddings = immutableEmbeddings(builder.embeddings);
        this.dimensions = resolveDimensions(this.embeddings);
        this.requestId = normalizeText(builder.requestId);
        this.inputTokenCount = validateNonNegative(
            builder.inputTokenCount,
            "inputTokenCount"
        );
        this.totalDurationNanos = validateNonNegative(
            builder.totalDurationNanos,
            "totalDurationNanos"
        );
        this.loadDurationNanos = validateNonNegative(
            builder.loadDurationNanos,
            "loadDurationNanos"
        );
        this.createdAt = validateNonNegative(
            builder.createdAt,
            "createdAt"
        );
        this.normalized = builder.normalized;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 단일 임베딩 응답을 생성합니다.
     *
     * @param model 모델명
     * @param embedding 임베딩 벡터
     * @return 임베딩 응답
     */
    public static EmbeddingResponse of(
        String model,
        float[] embedding
    ) {
        return builder()
            .model(model)
            .embedding(embedding)
            .build();
    }

    /**
     * 배치 임베딩 응답을 생성합니다.
     *
     * @param model 모델명
     * @param embeddings 임베딩 벡터 목록
     * @return 임베딩 응답
     */
    public static EmbeddingResponse ofBatch(
        String model,
        List<float[]> embeddings
    ) {
        return builder()
            .model(model)
            .embeddings(embeddings)
            .build();
    }

    public String getModel() {
        return model;
    }

    /**
     * 모든 임베딩 벡터를 반환합니다.
     *
     * <p>외부에서 내부 상태를 변경할 수 없도록 각 벡터는 복사되어
     * 반환됩니다.</p>
     */
    public List<float[]> getEmbeddings() {
        List<float[]> copies =
            new ArrayList<>(embeddings.size());

        for (float[] embedding : embeddings) {
            copies.add(embedding.clone());
        }

        return Collections.unmodifiableList(copies);
    }

    /**
     * 첫 번째 임베딩 벡터를 반환합니다.
     *
     * <p>단일 입력 요청에서 사용하는 편의 메서드입니다.</p>
     *
     * @return 첫 번째 임베딩 벡터의 복사본
     * @throws IllegalStateException 응답 벡터가 없는 경우
     */
    public float[] getEmbedding() {
        if (embeddings.isEmpty()) {
            throw new IllegalStateException(
                "Embedding response does not contain an embedding"
            );
        }

        return embeddings.get(0).clone();
    }

    /**
     * 특정 입력 인덱스의 임베딩 벡터를 반환합니다.
     *
     * @param index 입력 순서 인덱스
     * @return 임베딩 벡터의 복사본
     */
    public float[] getEmbedding(int index) {
        return embeddings.get(index).clone();
    }

    public int getDimensions() {
        return dimensions;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean hasRequestId() {
        return !requestId.isBlank();
    }

    public long getInputTokenCount() {
        return inputTokenCount;
    }

    public long getTotalDurationNanos() {
        return totalDurationNanos;
    }

    public long getLoadDurationNanos() {
        return loadDurationNanos;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isNormalized() {
        return normalized;
    }

    /**
     * 응답에 포함된 임베딩 벡터 개수를 반환합니다.
     */
    public int size() {
        return embeddings.size();
    }

    /**
     * 여러 임베딩이 포함된 배치 응답인지 확인합니다.
     */
    public boolean isBatch() {
        return embeddings.size() > 1;
    }

    /**
     * 임베딩 결과가 존재하는지 확인합니다.
     */
    public boolean hasEmbeddings() {
        return !embeddings.isEmpty();
    }

    /**
     * 전체 처리 시간을 밀리초 단위로 반환합니다.
     */
    public double getTotalDurationMillis() {
        return totalDurationNanos / 1_000_000.0;
    }

    /**
     * 모델 로딩 시간을 밀리초 단위로 반환합니다.
     */
    public double getLoadDurationMillis() {
        return loadDurationNanos / 1_000_000.0;
    }

    /**
     * 요청 입력 개수와 응답 벡터 개수가 일치하는지 확인합니다.
     *
     * @param expectedCount 예상 입력 개수
     * @throws IllegalStateException 개수가 일치하지 않는 경우
     */
    public void validateCount(int expectedCount) {
        if (expectedCount < 0) {
            throw new IllegalArgumentException(
                "expectedCount must be greater than or equal to zero"
            );
        }

        if (embeddings.size() != expectedCount) {
            throw new IllegalStateException(
                "Embedding count mismatch. expected="
                    + expectedCount
                    + ", actual="
                    + embeddings.size()
            );
        }
    }

    /**
     * 모든 벡터의 값이 유효한지 확인합니다.
     *
     * NaN 또는 무한대 값이 있으면 예외를 발생시킵니다.
     */
    public void validateValues() {
        for (int embeddingIndex = 0;
             embeddingIndex < embeddings.size();
             embeddingIndex++) {

            float[] embedding = embeddings.get(embeddingIndex);

            for (int dimensionIndex = 0;
                 dimensionIndex < embedding.length;
                 dimensionIndex++) {

                float value = embedding[dimensionIndex];

                if (!Float.isFinite(value)) {
                    throw new IllegalStateException(
                        "Invalid embedding value at embeddingIndex="
                            + embeddingIndex
                            + ", dimensionIndex="
                            + dimensionIndex
                    );
                }
            }
        }
    }

    private static List<float[]> immutableEmbeddings(
        List<float[]> embeddings
    ) {
        if (embeddings == null || embeddings.isEmpty()) {
            throw new IllegalArgumentException(
                "embeddings must not be empty"
            );
        }

        List<float[]> copies =
            new ArrayList<>(embeddings.size());

        int expectedDimensions = -1;

        for (int index = 0; index < embeddings.size(); index++) {
            float[] embedding = embeddings.get(index);

            if (embedding == null || embedding.length == 0) {
                throw new IllegalArgumentException(
                    "embedding must not be null or empty at index "
                        + index
                );
            }

            if (expectedDimensions < 0) {
                expectedDimensions = embedding.length;
            } else if (embedding.length != expectedDimensions) {
                throw new IllegalArgumentException(
                    "All embeddings must have the same dimensions. "
                        + "expected="
                        + expectedDimensions
                        + ", actual="
                        + embedding.length
                        + ", index="
                        + index
                );
            }

            float[] copy = embedding.clone();

            validateEmbeddingValues(copy, index);

            copies.add(copy);
        }

        return Collections.unmodifiableList(copies);
    }

    private static void validateEmbeddingValues(
        float[] embedding,
        int embeddingIndex
    ) {
        for (int dimensionIndex = 0;
             dimensionIndex < embedding.length;
             dimensionIndex++) {

            float value = embedding[dimensionIndex];

            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(
                    "Embedding contains invalid value at embeddingIndex="
                        + embeddingIndex
                        + ", dimensionIndex="
                        + dimensionIndex
                );
            }
        }
    }

    private static int resolveDimensions(
        List<float[]> embeddings
    ) {
        if (embeddings.isEmpty()) {
            return 0;
        }

        return embeddings.get(0).length;
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
        String normalized = normalizeText(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }

        return normalized;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "EmbeddingResponse{" +
            "model='" + model + '\'' +
            ", embeddingCount=" + embeddings.size() +
            ", dimensions=" + dimensions +
            ", requestId='" + requestId + '\'' +
            ", inputTokenCount=" + inputTokenCount +
            ", totalDurationNanos=" + totalDurationNanos +
            ", loadDurationNanos=" + loadDurationNanos +
            ", createdAt=" + createdAt +
            ", normalized=" + normalized +
            '}';
    }

    public static final class Builder {

        private String model;
        private final List<float[]> embeddings =
            new ArrayList<>();
        private String requestId;
        private long inputTokenCount;
        private long totalDurationNanos;
        private long loadDurationNanos;
        private long createdAt;
        private boolean normalized;

        private Builder() {
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * 임베딩 벡터 하나를 추가합니다.
         */
        public Builder embedding(float[] embedding) {
            if (embedding != null) {
                this.embeddings.add(embedding.clone());
            } else {
                this.embeddings.add(null);
            }

            return this;
        }

        /**
         * 기존 임베딩을 제거하고 새 목록으로 교체합니다.
         */
        public Builder embeddings(
            List<float[]> embeddings
        ) {
            this.embeddings.clear();

            if (embeddings != null) {
                for (float[] embedding : embeddings) {
                    embedding(embedding);
                }
            }

            return this;
        }

        /**
         * 기존 목록에 여러 임베딩을 추가합니다.
         */
        public Builder addEmbeddings(
            List<float[]> embeddings
        ) {
            if (embeddings != null) {
                for (float[] embedding : embeddings) {
                    embedding(embedding);
                }
            }

            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder inputTokenCount(
            long inputTokenCount
        ) {
            this.inputTokenCount = inputTokenCount;
            return this;
        }

        public Builder totalDurationNanos(
            long totalDurationNanos
        ) {
            this.totalDurationNanos = totalDurationNanos;
            return this;
        }

        public Builder loadDurationNanos(
            long loadDurationNanos
        ) {
            this.loadDurationNanos = loadDurationNanos;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder normalized(boolean normalized) {
            this.normalized = normalized;
            return this;
        }

        public EmbeddingResponse build() {
            return new EmbeddingResponse(this);
        }
    }
}