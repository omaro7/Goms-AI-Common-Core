/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.retrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * Retriever 검색 결과입니다.
 */
public final class RetrievalResult {

    /**
     * 원본 사용자 질의입니다.
     */
    private final String query;

    /**
     * 사용된 임베딩 모델명입니다.
     */
    private final String model;

    /**
     * 검색 결과 목록입니다.
     */
    private final List<VectorSearchResult> searchResults;

    /**
     * 검색 처리 시간입니다.
     *
     * 나노초 단위입니다.
     */
    private final long durationNanos;

    /**
     * 질의 임베딩 차원입니다.
     */
    private final int dimensions;

    private RetrievalResult(Builder builder) {
        this.query = requireText(
            builder.query,
            "query"
        );

        this.model = requireText(
            builder.model,
            "model"
        );

        this.searchResults =
            immutableResults(builder.searchResults);

        this.durationNanos =
            validateNonNegative(
                builder.durationNanos,
                "durationNanos"
            );

        this.dimensions =
            validateNonNegative(
                builder.dimensions,
                "dimensions"
            );
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getQuery() {
        return query;
    }

    public String getModel() {
        return model;
    }

    public List<VectorSearchResult> getSearchResults() {
        return searchResults;
    }

    public List<DocumentChunk> getChunks() {
        if (searchResults.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> chunks =
            new ArrayList<>(searchResults.size());

        for (VectorSearchResult result : searchResults) {
            chunks.add(result.getChunk());
        }

        return List.copyOf(chunks);
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public double getDurationMillis() {
        return durationNanos / 1_000_000.0;
    }

    public int getDimensions() {
        return dimensions;
    }

    public int size() {
        return searchResults.size();
    }

    public boolean isEmpty() {
        return searchResults.isEmpty();
    }

    public double getHighestScore() {
        return searchResults.isEmpty()
            ? 0.0
            : searchResults.get(0).getScore();
    }

    private static List<VectorSearchResult> immutableResults(
        List<VectorSearchResult> values
    ) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        List<VectorSearchResult> copy =
            new ArrayList<>(values.size());

        for (VectorSearchResult value : values) {
            if (value != null) {
                copy.add(value);
            }
        }

        return Collections.unmodifiableList(copy);
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
            value == null ? "" : value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }

        return normalized;
    }

    public static final class Builder {

        private String query;
        private String model;

        private final List<VectorSearchResult> searchResults =
            new ArrayList<>();

        private long durationNanos;
        private int dimensions;

        private Builder() {
        }

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder searchResult(
            VectorSearchResult searchResult
        ) {
            if (searchResult != null) {
                searchResults.add(searchResult);
            }

            return this;
        }

        public Builder searchResults(
            List<VectorSearchResult> searchResults
        ) {
            this.searchResults.clear();

            if (searchResults != null) {
                for (VectorSearchResult result : searchResults) {
                    searchResult(result);
                }
            }

            return this;
        }

        public Builder durationNanos(
            long durationNanos
        ) {
            this.durationNanos = durationNanos;
            return this;
        }

        public Builder dimensions(int dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public RetrievalResult build() {
            return new RetrievalResult(this);
        }
    }
}