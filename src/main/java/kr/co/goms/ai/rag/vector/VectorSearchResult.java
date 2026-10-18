/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.model.DocumentChunkType;

/**
 * VectorStore 검색 결과입니다.
 *
 * <p>검색된 {@link VectorRecord}, 유사도 점수, 검색 순위와
 * 부가 정보를 함께 표현합니다.</p>
 *
 * <pre>
 * query vector
 *      ↓
 * VectorStore.search(...)
 *      ↓
 * List&lt;VectorSearchResult&gt;
 * </pre>
 */
public final class VectorSearchResult implements Comparable<VectorSearchResult> {

    /**
     * 검색된 벡터 레코드입니다.
     */
    private final VectorRecord record;

    /**
     * 유사도 점수입니다.
     *
     * <p>코사인 유사도를 사용할 경우 일반적으로 -1.0~1.0 범위입니다.
     * 정규화된 임베딩을 사용하는 일반적인 텍스트 검색에서는 대체로
     * 0.0~1.0 범위의 값이 반환됩니다.</p>
     */
    private final double score;

    /**
     * 검색 결과 순위입니다.
     *
     * <p>1부터 시작하며, 순위가 아직 부여되지 않은 경우 0입니다.</p>
     */
    private final int rank;

    /**
     * 검색에 사용한 점수 계산 방식입니다.
     */
    private final VectorSimilarityType similarityType;

    /**
     * 검색 결과가 최소 점수 기준을 통과했는지 여부입니다.
     */
    private final boolean accepted;

    /**
     * 추가 검색 메타데이터입니다.
     *
     * <p>필터 적용 정보, 원본 점수, 재정렬 점수 등을 저장할 수 있습니다.</p>
     */
    private final Map<String, String> metadata;

    private VectorSearchResult(Builder builder) {
        this.record = Objects.requireNonNull(builder.record, "record must not be null");
        this.score = validateScore(builder.score);
        this.rank = validateRank(builder.rank);
        this.similarityType = Objects.requireNonNullElse(builder.similarityType, VectorSimilarityType.COSINE);
        this.accepted = builder.accepted;
        this.metadata = immutableMetadata(builder.metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기본 검색 결과를 생성합니다.
     *
     * @param record 검색된 레코드
     * @param score 유사도 점수
     * @return 검색 결과
     */
    public static VectorSearchResult of(VectorRecord record, double score) {
        return builder()
            .record(record)
            .score(score)
            .accepted(true)
            .build();
    }

    /**
     * 순위가 포함된 검색 결과를 생성합니다.
     *
     * @param record 검색된 레코드
     * @param score 유사도 점수
     * @param rank 결과 순위
     * @return 검색 결과
     */
    public static VectorSearchResult of(VectorRecord record, double score, int rank) {
        return builder()
            .record(record)
            .score(score)
            .rank(rank)
            .accepted(true)
            .build();
    }

    public VectorRecord getRecord() {
        return record;
    }

    /**
     * 검색된 문서 Chunk를 반환합니다.
     */
    public DocumentChunk getChunk() {
        return record.getChunk();
    }

    public String getId() {
        return record.getId();
    }

    public double getScore() {
        return score;
    }

    public int getRank() {
        return rank;
    }

    public boolean hasRank() {
        return rank > 0;
    }

    public VectorSimilarityType getSimilarityType() {
        return similarityType;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getMetadata(String key) {
        if (key == null) {
            return null;
        }

        return metadata.get(key);
    }

    public boolean hasMetadata(String key) {
        return key != null && metadata.containsKey(key);
    }

    /**
     * 점수가 지정한 최소 점수 이상인지 확인합니다.
     */
    public boolean meetsMinimumScore(double minimumScore) {
        validateFinite(minimumScore, "minimumScore");
        return score >= minimumScore;
    }

    /**
     * 지정된 모델의 검색 결과인지 확인합니다.
     */
    public boolean isModel(String model) {
        return record.isModel(model);
    }

    /**
     * 지정된 원본 파일에서 검색된 결과인지 확인합니다.
     */
    public boolean isSourcePath(String sourcePath) {
        if (sourcePath == null) {
            return false;
        }

        return record.getChunk()
            .getSourcePath()
            .equals(sourcePath.trim());
    }

    /**
     * 지정된 Chunk 유형인지 확인합니다.
     */
    public boolean isChunkType(DocumentChunkType type) {
        return type != null && record.getChunk().getType() == type;
    }

    /**
     * 현재 검색 결과에 새로운 순위를 적용한 복사본을 반환합니다.
     */
    public VectorSearchResult withRank(int newRank) {
        return builder()
            .record(record)
            .score(score)
            .rank(newRank)
            .similarityType(similarityType)
            .accepted(accepted)
            .metadata(metadata)
            .build();
    }

    /**
     * 현재 검색 결과에 승인 여부를 적용한 복사본을 반환합니다.
     */
    public VectorSearchResult withAccepted(boolean newAccepted) {
        return builder()
            .record(record)
            .score(score)
            .rank(rank)
            .similarityType(similarityType)
            .accepted(newAccepted)
            .metadata(metadata)
            .build();
    }

    /**
     * 점수가 높은 결과가 먼저 오도록 비교합니다.
     *
     * <p>점수가 같으면 rank, ID 순서로 비교하여 정렬 결과를
     * 안정적으로 유지합니다.</p>
     */
    @Override
    public int compareTo(VectorSearchResult other) {
        Objects.requireNonNull(other, "other must not be null");

        int scoreComparison = Double.compare(other.score, this.score);

        if (scoreComparison != 0) {
            return scoreComparison;
        }

        int thisRank = this.rank == 0 ? Integer.MAX_VALUE : this.rank;
        int otherRank = other.rank == 0 ? Integer.MAX_VALUE : other.rank;

        int rankComparison = Integer.compare(thisRank, otherRank);

        if (rankComparison != 0) {
            return rankComparison;
        }

        return this.getId().compareTo(other.getId());
    }

    private static double validateScore(double score) {
        validateFinite(score, "score");
        return score;
    }

    private static int validateRank(int rank) {
        if (rank < 0) {
            throw new IllegalArgumentException("rank must be greater than or equal to zero");
        }

        return rank;
    }

    private static void validateFinite(double value, String fieldName) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
    }

    private static Map<String, String> immutableMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> copy = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = normalize(entry.getKey());
            String value = normalize(entry.getValue());

            if (!key.isBlank()) {
                copy.put(key, value);
            }
        }

        return Collections.unmodifiableMap(copy);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof VectorSearchResult)) {
            return false;
        }

        VectorSearchResult other = (VectorSearchResult) object;

        return record.equals(other.record)
            && Double.compare(score, other.score) == 0
            && rank == other.rank
            && similarityType == other.similarityType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(record, score, rank, similarityType);
    }

    @Override
    public String toString() {
        return "VectorSearchResult{" +
            "recordId='" + record.getId() + '\'' +
            ", score=" + score +
            ", rank=" + rank +
            ", similarityType=" + similarityType +
            ", accepted=" + accepted +
            ", sourcePath='" +
            record.getChunk().getSourcePath() + '\'' +
            ", chunkType=" +
            record.getChunk().getType() +
            '}';
    }

    public static final class Builder {

        private VectorRecord record;
        private double score;
        private int rank;
        private VectorSimilarityType similarityType = VectorSimilarityType.COSINE;
        private boolean accepted = true;
        private final Map<String, String> metadata = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder record(VectorRecord record) {
            this.record = record;
            return this;
        }

        public Builder score(double score) {
            this.score = score;
            return this;
        }

        public Builder rank(int rank) {
            this.rank = rank;
            return this;
        }

        public Builder similarityType(VectorSimilarityType similarityType) {
            this.similarityType = similarityType;
            return this;
        }

        public Builder accepted(boolean accepted) {
            this.accepted = accepted;
            return this;
        }

        public Builder metadata(String key, String value) {
            String normalizedKey = normalize(key);

            if (!normalizedKey.isBlank()) {
                metadata.put(normalizedKey, normalize(value));
            }

            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            if (metadata == null) {
                return this;
            }

            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                metadata(entry.getKey(), entry.getValue());
            }

            return this;
        }

        public VectorSearchResult build() {
            return new VectorSearchResult(this);
        }
    }
}