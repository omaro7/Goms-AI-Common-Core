/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.retrieval;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import kr.co.goms.ai.rag.model.DocumentChunkType;
import kr.co.goms.ai.rag.vector.VectorSimilarityType;

/**
 * Retriever 검색 요청입니다.
 *
 * <p>사용자 질의와 VectorStore 검색 조건을 정의합니다.</p>
 */
public final class RetrievalRequest {

    public static final int DEFAULT_TOP_K = 8;

    public static final double DEFAULT_MINIMUM_SCORE = 0.45;

    public static final VectorSimilarityType
        DEFAULT_SIMILARITY_TYPE =
            VectorSimilarityType.COSINE;

    /**
     * 사용자 검색 질의입니다.
     */
    private final String query;

    /**
     * 검색할 최대 결과 개수입니다.
     */
    private final int topK;

    /**
     * 결과에 포함할 최소 유사도 점수입니다.
     */
    private final double minimumScore;

    /**
     * 벡터 유사도 계산 방식입니다.
     */
    private final VectorSimilarityType similarityType;

    /**
     * 검색할 Chunk 유형입니다.
     *
     * 비어 있으면 모든 Chunk 유형을 검색합니다.
     */
    private final Set<DocumentChunkType> chunkTypes;

    /**
     * 검색할 원본 문서 경로입니다.
     *
     * 비어 있으면 모든 원본 문서를 검색합니다.
     */
    private final Set<String> sourcePaths;

    /**
     * 검색할 EPUB 의미 유형입니다.
     *
     * 예:
     * chapter
     * toc
     * titlepage
     */
    private final Set<String> epubTypes;

    /**
     * 검색할 문서 언어입니다.
     *
     * 예:
     * ko
     * en
     * ko-kr
     */
    private final Set<String> languages;

    /**
     * Chunk 메타데이터 필터입니다.
     *
     * 모든 조건이 일치해야 검색 대상에 포함됩니다.
     */
    private final Map<String, String> metadataFilters;

    /**
     * 최소 점수 미달 결과도 포함할지 여부입니다.
     */
    private final boolean includeRejected;

    /**
     * 검색 결과를 원본 문서 순서로 재정렬할지 여부입니다.
     *
     * false이면 유사도 점수 순서를 유지합니다.
     */
    private final boolean preserveDocumentOrder;
    
    /**
     * 검색 대상 EPUB 프로젝트 식별자입니다.
     */
    private final String projectId;

    private RetrievalRequest(Builder builder) {
    	
    	this.projectId =
    	        requireText(
    	                builder.projectId,
    	                "projectId"
    	        );

    	
        this.query = requireText(
            builder.query,
            "query"
        );

        this.topK = validateTopK(
            builder.topK
        );

        this.similarityType =
            builder.similarityType == null
                ? DEFAULT_SIMILARITY_TYPE
                : builder.similarityType;

        this.minimumScore =
            validateMinimumScore(
                builder.minimumScore,
                this.similarityType
            );

        this.chunkTypes =
            immutableChunkTypes(
                builder.chunkTypes
            );

        this.sourcePaths =
            immutableTextSet(
                builder.sourcePaths,
                true
            );

        this.epubTypes =
            immutableTextSet(
                builder.epubTypes,
                false
            );

        this.languages =
            immutableLanguages(
                builder.languages
            );

        this.metadataFilters =
            immutableMetadata(
                builder.metadataFilters
            );

        this.includeRejected =
            builder.includeRejected;

        this.preserveDocumentOrder =
            builder.preserveDocumentOrder;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기본 검색 조건으로 요청을 생성합니다.
     *
     * @param query 사용자 질의
     * @return 검색 요청
     */
    public static RetrievalRequest of(
        String query
    ) {
        return builder()
            .query(query)
            .build();
    }

    /**
     * 결과 개수를 지정하여 요청을 생성합니다.
     *
     * @param query 사용자 질의
     * @param topK 최대 검색 결과 개수
     * @return 검색 요청
     */
    public static RetrievalRequest of(
        String query,
        int topK
    ) {
        return builder()
            .query(query)
            .topK(topK)
            .build();
    }

    public String getProjectId() {
        return projectId;
    }
    
    public String getQuery() {
        return query;
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

    public Set<DocumentChunkType> getChunkTypes() {
        return chunkTypes;
    }

    public Set<String> getSourcePaths() {
        return sourcePaths;
    }

    public Set<String> getEpubTypes() {
        return epubTypes;
    }

    public Set<String> getLanguages() {
        return languages;
    }

    public Map<String, String> getMetadataFilters() {
        return metadataFilters;
    }

    public boolean isIncludeRejected() {
        return includeRejected;
    }

    public boolean isPreserveDocumentOrder() {
        return preserveDocumentOrder;
    }

    public boolean hasChunkTypeFilters() {
        return !chunkTypes.isEmpty();
    }

    public boolean hasSourcePathFilters() {
        return !sourcePaths.isEmpty();
    }

    public boolean hasEpubTypeFilters() {
        return !epubTypes.isEmpty();
    }

    public boolean hasLanguageFilters() {
        return !languages.isEmpty();
    }

    public boolean hasMetadataFilters() {
        return !metadataFilters.isEmpty();
    }

    /**
     * 하나 이상의 검색 필터가 존재하는지 확인합니다.
     */
    public boolean hasFilters() {
        return hasChunkTypeFilters()
            || hasSourcePathFilters()
            || hasEpubTypeFilters()
            || hasLanguageFilters()
            || hasMetadataFilters();
    }

    private static int validateTopK(int topK) {
        if (topK < 1) {
            throw new IllegalArgumentException(
                "topK must be greater than zero"
            );
        }

        return topK;
    }

    /**
     * 유사도 방식에 따라 최소 점수를 검증합니다.
     */
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
                    "minimumScore for cosine similarity "
                        + "must be between -1.0 and 1.0"
                );
            }
        }

        if (similarityType
            == VectorSimilarityType.EUCLIDEAN) {

            /*
             * VectorSimilarity.euclideanScore()는
             * 1 / (1 + distance) 값을 반환하므로
             * 검색 점수 범위는 0 초과 1 이하입니다.
             */
            if (minimumScore < 0.0
                || minimumScore > 1.0) {

                throw new IllegalArgumentException(
                    "minimumScore for euclidean similarity "
                        + "must be between 0.0 and 1.0"
                );
            }
        }

        /*
         * DOT_PRODUCT는 정규화 여부에 따라 범위가 달라지므로
         * 유한한 값인지 여부만 검사합니다.
         */
        return minimumScore;
    }

    private static Set<DocumentChunkType>
        immutableChunkTypes(
            Set<DocumentChunkType> values
        ) {

        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        EnumSet<DocumentChunkType> copy =
            EnumSet.noneOf(
                DocumentChunkType.class
            );

        for (DocumentChunkType value : values) {
            if (value != null) {
                copy.add(value);
            }
        }

        if (copy.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(copy);
    }

    private static Set<String> immutableTextSet(
        Set<String> values,
        boolean normalizePath
    ) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> copy =
            new LinkedHashSet<>();

        for (String value : values) {
            String normalized = normalize(value);

            if (normalizePath) {
                normalized =
                    normalized.replace('\\', '/');
            }

            if (!normalized.isBlank()) {
                copy.add(normalized);
            }
        }

        if (copy.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(copy);
    }

    private static Set<String> immutableLanguages(
        Set<String> values
    ) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> copy =
            new LinkedHashSet<>();

        for (String value : values) {
            String normalized =
                normalizeLanguage(value);

            if (!normalized.isBlank()) {
                copy.add(normalized);
            }
        }

        if (copy.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(copy);
    }

    private static Map<String, String>
        immutableMetadata(
            Map<String, String> values
        ) {

        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> copy =
            new LinkedHashMap<>();

        for (Map.Entry<String, String> entry
            : values.entrySet()) {

            String key =
                normalize(entry.getKey());

            String value =
                normalize(entry.getValue());

            if (!key.isBlank()) {
                copy.put(key, value);
            }
        }

        if (copy.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(copy);
    }

    private static String requireText(
        String value,
        String fieldName
    ) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }

        return normalized;
    }

    private static String normalize(
        String value
    ) {
        return value == null
            ? ""
            : value.trim();
    }

    private static String normalizePath(
        String value
    ) {
        return normalize(value)
            .replace('\\', '/');
    }

    private static String normalizeLanguage(
        String value
    ) {
        return normalize(value)
            .replace('_', '-')
            .toLowerCase(
                java.util.Locale.ROOT
            );
    }

    @Override
    public String toString() {
        return "RetrievalRequest{" +
            "query='" + query + '\'' +
            ", projectId='" + projectId + '\'' + 
            ", topK=" + topK +
            ", minimumScore=" + minimumScore +
            ", similarityType=" + similarityType +
            ", chunkTypes=" + chunkTypes +
            ", sourcePaths=" + sourcePaths +
            ", epubTypes=" + epubTypes +
            ", languages=" + languages +
            ", metadataFilters=" + metadataFilters +
            ", includeRejected=" + includeRejected +
            ", preserveDocumentOrder="
                + preserveDocumentOrder +
            '}';
    }

    public static final class Builder {

    	private String projectId;
    	
        private String query;

        private int topK =
            DEFAULT_TOP_K;

        private double minimumScore =
            DEFAULT_MINIMUM_SCORE;

        private VectorSimilarityType similarityType =
            DEFAULT_SIMILARITY_TYPE;

        private final Set<DocumentChunkType> chunkTypes =
            EnumSet.noneOf(
                DocumentChunkType.class
            );

        private final Set<String> sourcePaths =
            new LinkedHashSet<>();

        private final Set<String> epubTypes =
            new LinkedHashSet<>();

        private final Set<String> languages =
            new LinkedHashSet<>();

        private final Map<String, String> metadataFilters =
            new LinkedHashMap<>();

        private boolean includeRejected;

        private boolean preserveDocumentOrder;

        private Builder() {
        }

        public Builder projectId(
                String projectId) {

            this.projectId =
                    projectId;

            return this;
        }
        
        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder minimumScore(
            double minimumScore
        ) {
            this.minimumScore = minimumScore;
            return this;
        }

        public Builder similarityType(
            VectorSimilarityType similarityType
        ) {
            this.similarityType = similarityType;
            return this;
        }

        /**
         * Chunk 유형 하나를 추가합니다.
         */
        public Builder chunkType(
            DocumentChunkType chunkType
        ) {
            if (chunkType != null) {
                chunkTypes.add(chunkType);
            }

            return this;
        }

        /**
         * 기존 Chunk 유형 조건을 제거하고
         * 전달된 유형 집합으로 교체합니다.
         */
        public Builder chunkTypes(
            Set<DocumentChunkType> chunkTypes
        ) {
            this.chunkTypes.clear();

            if (chunkTypes != null) {
                for (DocumentChunkType chunkType
                    : chunkTypes) {

                    chunkType(chunkType);
                }
            }

            return this;
        }

        /**
         * 원본 문서 경로 하나를 추가합니다.
         */
        public Builder sourcePath(
            String sourcePath
        ) {
            String normalized =
                normalizePath(sourcePath);

            if (!normalized.isBlank()) {
                sourcePaths.add(normalized);
            }

            return this;
        }

        /**
         * 기존 원본 문서 경로 조건을 제거하고
         * 전달된 경로 집합으로 교체합니다.
         */
        public Builder sourcePaths(
            Set<String> sourcePaths
        ) {
            this.sourcePaths.clear();

            if (sourcePaths != null) {
                for (String sourcePath
                    : sourcePaths) {

                    sourcePath(sourcePath);
                }
            }

            return this;
        }

        /**
         * EPUB 의미 유형 하나를 추가합니다.
         */
        public Builder epubType(
            String epubType
        ) {
            String normalized =
                normalize(epubType);

            if (!normalized.isBlank()) {
                epubTypes.add(normalized);
            }

            return this;
        }

        /**
         * 기존 EPUB 의미 유형 조건을 제거하고
         * 전달된 유형 집합으로 교체합니다.
         */
        public Builder epubTypes(
            Set<String> epubTypes
        ) {
            this.epubTypes.clear();

            if (epubTypes != null) {
                for (String epubType
                    : epubTypes) {

                    epubType(epubType);
                }
            }

            return this;
        }

        /**
         * 언어 조건 하나를 추가합니다.
         */
        public Builder language(
            String language
        ) {
            String normalized =
                normalizeLanguage(language);

            if (!normalized.isBlank()) {
                languages.add(normalized);
            }

            return this;
        }

        /**
         * 기존 언어 조건을 제거하고
         * 전달된 언어 집합으로 교체합니다.
         */
        public Builder languages(
            Set<String> languages
        ) {
            this.languages.clear();

            if (languages != null) {
                for (String language
                    : languages) {

                    language(language);
                }
            }

            return this;
        }

        /**
         * 메타데이터 조건 하나를 추가합니다.
         */
        public Builder metadataFilter(
            String key,
            String value
        ) {
            String normalizedKey =
                normalize(key);

            if (!normalizedKey.isBlank()) {
                metadataFilters.put(
                    normalizedKey,
                    normalize(value)
                );
            }

            return this;
        }

        /**
         * 기존 메타데이터 조건을 제거하고
         * 전달된 조건으로 교체합니다.
         */
        public Builder metadataFilters(
            Map<String, String> metadataFilters
        ) {
            this.metadataFilters.clear();

            if (metadataFilters != null) {
                for (Map.Entry<String, String> entry
                    : metadataFilters.entrySet()) {

                    metadataFilter(
                        entry.getKey(),
                        entry.getValue()
                    );
                }
            }

            return this;
        }

        public Builder includeRejected(
            boolean includeRejected
        ) {
            this.includeRejected =
                includeRejected;

            return this;
        }

        public Builder preserveDocumentOrder(
            boolean preserveDocumentOrder
        ) {
            this.preserveDocumentOrder =
                preserveDocumentOrder;

            return this;
        }

        public RetrievalRequest build() {
            return new RetrievalRequest(this);
        }
    }
}