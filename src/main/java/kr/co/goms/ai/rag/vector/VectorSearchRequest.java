/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.vector;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import kr.co.goms.ai.rag.model.DocumentChunkType;

/**
 * VectorStore 검색 요청입니다.
 *
 * <p>검색 질의 벡터, 결과 개수, 최소 점수와 문서 필터 조건을
 * 하나의 객체로 전달합니다.</p>
 *
 * <pre>
 * VectorSearchRequest request =
 *     VectorSearchRequest.builder()
 *         .projectId(projectId)
 *         .queryVector(queryVector)
 *         .model("nomic-embed-text")
 *         .topK(8)
 *         .minimumScore(0.50)
 *         .chunkType(DocumentChunkType.PARAGRAPH)
 *         .build();
 * </pre>
 */
public final class VectorSearchRequest {

    public static final int DEFAULT_TOP_K = 8;

    public static final double DEFAULT_MINIMUM_SCORE = 0.0;

    public static final VectorSimilarityType DEFAULT_SIMILARITY_TYPE = VectorSimilarityType.COSINE;

    /**
     * 검색 질의 임베딩 벡터입니다.
     */
    private final float[] queryVector;

    /**
     * 검색에 사용한 임베딩 모델명입니다.
     *
     * 서로 다른 모델로 생성된 벡터 간 검색을 방지합니다.
     */
    private final String model;

    /**
     * 반환할 최대 결과 개수입니다.
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
     * 검색 대상 Chunk 유형입니다.
     *
     * 비어 있으면 모든 Chunk 유형을 검색합니다.
     */
    private final Set<DocumentChunkType> chunkTypes;

    /**
     * 검색 대상 원본 문서 경로입니다.
     *
     * 비어 있으면 모든 원본 문서를 검색합니다.
     */
    private final Set<String> sourcePaths;

    /**
     * 검색 대상 EPUB 의미 유형입니다.
     *
     * 예:
     * chapter
     * toc
     * titlepage
     */
    private final Set<String> epubTypes;

    /**
     * 검색 대상 언어입니다.
     *
     * 예:
     * ko
     * en
     */
    private final Set<String> languages;

    /**
     * 검색 대상 메타데이터 조건입니다.
     *
     * 모든 조건이 일치해야 검색 대상에 포함됩니다.
     */
    private final Map<String, String> metadataFilters;

    /**
     * 최소 점수 미만 결과도 반환할지 여부입니다.
     *
     * false이면 minimumScore 미만 결과를 제외합니다.
     */
    private final boolean includeRejected;
    
    /**
     * 검색 대상 EPUB 프로젝트 식별자입니다.
     */
    private final String projectId;

    private VectorSearchRequest(Builder builder) {
    	
    	this.projectId =
    	        requireText(
    	                builder.projectId,
    	                "projectId"
    	        );

        this.queryVector = copyAndValidateVector(
            builder.queryVector
        );

        this.model = requireText(
            builder.model,
            "model"
        );

        this.topK = validateTopK(builder.topK);

        this.minimumScore = validateMinimumScore(
            builder.minimumScore,
            builder.similarityType
        );

        this.similarityType = Objects.requireNonNullElse(
            builder.similarityType,
            DEFAULT_SIMILARITY_TYPE
        );

        this.chunkTypes = immutableChunkTypes(
            builder.chunkTypes
        );

        this.sourcePaths = immutableTextSet(
            builder.sourcePaths
        );

        this.epubTypes = immutableTextSet(
            builder.epubTypes
        );

        this.languages = immutableTextSet(
            builder.languages
        );

        this.metadataFilters = immutableMetadataFilters(
            builder.metadataFilters
        );

        this.includeRejected = builder.includeRejected;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기본 벡터 검색 요청을 생성합니다.
     *
     * @param projectId EPUB 프로젝트 식별자
     * @param queryVector 질의 벡터
     * @param model 임베딩 모델명
     * @return 검색 요청
     */
    public static VectorSearchRequest of(
        String projectId,
        float[] queryVector,
        String model
    ) {
        return builder()
            .projectId(projectId)
            .queryVector(queryVector)
            .model(model)
            .build();
    }

    /**
     * 결과 개수가 지정된 검색 요청을 생성합니다.
     *
     * @param projectId EPUB 프로젝트 식별자
     * @param queryVector 질의 벡터
     * @param model 임베딩 모델명
     * @param topK 최대 결과 개수
     * @return 검색 요청
     */
    public static VectorSearchRequest of(
        String projectId,
        float[] queryVector,
        String model,
        int topK
    ) {
        return builder()
            .projectId(projectId)
            .queryVector(queryVector)
            .model(model)
            .topK(topK)
            .build();
    }

    public String getProjectId() {
        return projectId;
    }
    
    public float[] getQueryVector() {
        return queryVector.clone();
    }

    public int getDimensions() {
        return queryVector.length;
    }

    public String getModel() {
        return model;
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

    /**
     * 지정된 레코드가 검색 조건과 일치하는지 확인합니다.
     *
     * @param record 확인할 벡터 레코드
     * @return 검색 대상 포함 여부
     */
    public boolean matches(VectorRecord record) {
        if (record == null) {
            return false;
        }
        /*
         * Project Scope
         */
        if (!record.isProject(
            projectId
        )) {
            return false;
        }

        if (!record.isModel(model)) {
            return false;
        }

        if (!record.hasDimensions(getDimensions())) {
            return false;
        }

        if (hasChunkTypeFilters()
            && !chunkTypes.contains(
                record.getChunk().getType()
            )) {

            return false;
        }

        if (hasSourcePathFilters()
            && !sourcePaths.contains(
                normalizePath(
                    record.getChunk().getSourcePath()
                )
            )) {

            return false;
        }

        if (hasEpubTypeFilters()
            && !epubTypes.contains(
                normalize(
                    record.getChunk().getEpubType()
                )
            )) {

            return false;
        }

        if (hasLanguageFilters()
            && !languages.contains(
                normalizeLanguage(
                    record.getChunk().getLanguage()
                )
            )) {

            return false;
        }

        return matchesMetadata(record);
    }

    /**
     * 계산된 점수가 최소 점수 기준을 충족하는지 확인합니다.
     */
    public boolean accepts(double score) {
        if (!Double.isFinite(score)) {
            return false;
        }

        /*
         * VectorStore의 외부 점수는 모든 방식에서 값이 클수록
         * 관련성이 높도록 변환된다고 가정합니다.
         */
        return score >= minimumScore;
    }

    private boolean matchesMetadata(VectorRecord record) {
        if (!hasMetadataFilters()) {
            return true;
        }

        Map<String, String> chunkMetadata =
            record.getChunk().getMetadata();

        for (Map.Entry<String, String> filter
            : metadataFilters.entrySet()) {

            String actualValue =
                chunkMetadata.get(filter.getKey());

            if (actualValue == null) {
                return false;
            }

            if (!normalize(actualValue).equals(
                filter.getValue()
            )) {
                return false;
            }
        }

        return true;
    }

    private static float[] copyAndValidateVector(
        float[] vector
    ) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException(
                "queryVector must not be null or empty"
            );
        }

        float[] copy = vector.clone();

        for (int index = 0;
             index < copy.length;
             index++) {

            if (!Float.isFinite(copy[index])) {
                throw new IllegalArgumentException(
                    "queryVector contains invalid value at index "
                        + index
                );
            }
        }

        return copy;
    }

    private static int validateTopK(int topK) {
        if (topK < 1) {
            throw new IllegalArgumentException(
                "topK must be greater than zero"
            );
        }

        return topK;
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

        VectorSimilarityType resolvedType =
            Objects.requireNonNullElse(
                similarityType,
                DEFAULT_SIMILARITY_TYPE
            );

        if (resolvedType == VectorSimilarityType.COSINE
            && (minimumScore < -1.0
                || minimumScore > 1.0)) {

            throw new IllegalArgumentException(
                "minimumScore for cosine similarity "
                    + "must be between -1.0 and 1.0"
            );
        }

        return minimumScore;
    }

    private static Set<DocumentChunkType> immutableChunkTypes(
        Set<DocumentChunkType> chunkTypes
    ) {
        if (chunkTypes == null || chunkTypes.isEmpty()) {
            return Collections.emptySet();
        }

        EnumSet<DocumentChunkType> copy =
            EnumSet.noneOf(DocumentChunkType.class);

        for (DocumentChunkType type : chunkTypes) {
            if (type != null) {
                copy.add(type);
            }
        }

        if (copy.isEmpty()) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(copy);
    }

    private static Set<String> immutableTextSet(
        Set<String> values
    ) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> copy = new LinkedHashSet<>();

        for (String value : values) {
            String normalized = normalize(value);

            if (!normalized.isBlank()) {
                copy.add(normalized);
            }
        }

        return Collections.unmodifiableSet(copy);
    }

    private static Map<String, String> immutableMetadataFilters(
        Map<String, String> filters
    ) {
        if (filters == null || filters.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> copy =
            new LinkedHashMap<>();

        for (Map.Entry<String, String> entry
            : filters.entrySet()) {

            String key = normalize(entry.getKey());
            String value = normalize(entry.getValue());

            if (!key.isBlank()) {
                copy.put(key, value);
            }
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

    private static String normalize(String value) {
        return value == null
            ? ""
            : value.trim();
    }

    private static String normalizePath(String value) {
        return normalize(value)
            .replace('\\', '/');
    }

    private static String normalizeLanguage(String value) {
        return normalize(value)
            .replace('_', '-')
            .toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String toString() {
        return "VectorSearchRequest{" +
            "projectId='" + projectId + '\'' +
            ", model='" + model + '\'' +
            ", dimensions=" + queryVector.length +
            ", topK=" + topK +
            ", minimumScore=" + minimumScore +
            ", similarityType=" + similarityType +
            ", chunkTypes=" + chunkTypes +
            ", sourcePaths=" + sourcePaths +
            ", epubTypes=" + epubTypes +
            ", languages=" + languages +
            ", metadataFilters=" + metadataFilters +
            ", includeRejected=" + includeRejected +
            '}';
    }

    public static final class Builder {
    	private String projectId;
        private float[] queryVector;
        private String model;
        private int topK = DEFAULT_TOP_K;
        private double minimumScore =
            DEFAULT_MINIMUM_SCORE;
        private VectorSimilarityType similarityType =
            DEFAULT_SIMILARITY_TYPE;

        private final Set<DocumentChunkType> chunkTypes =
            EnumSet.noneOf(DocumentChunkType.class);

        private final Set<String> sourcePaths =
            new LinkedHashSet<>();

        private final Set<String> epubTypes =
            new LinkedHashSet<>();

        private final Set<String> languages =
            new LinkedHashSet<>();

        private final Map<String, String> metadataFilters =
            new LinkedHashMap<>();

        private boolean includeRejected;

        private Builder() {
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }
        
        public Builder queryVector(float[] queryVector) {
            this.queryVector = queryVector == null
                ? null
                : queryVector.clone();

            return this;
        }

        public Builder model(String model) {
            this.model = model;
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

        public Builder chunkType(
            DocumentChunkType chunkType
        ) {
            if (chunkType != null) {
                chunkTypes.add(chunkType);
            }

            return this;
        }

        public Builder chunkTypes(
            Set<DocumentChunkType> chunkTypes
        ) {
            this.chunkTypes.clear();

            if (chunkTypes != null) {
                for (DocumentChunkType type : chunkTypes) {
                    chunkType(type);
                }
            }

            return this;
        }

        public Builder sourcePath(String sourcePath) {
            String normalized =
                normalizePath(sourcePath);

            if (!normalized.isBlank()) {
                sourcePaths.add(normalized);
            }

            return this;
        }

        public Builder sourcePaths(
            Set<String> sourcePaths
        ) {
            this.sourcePaths.clear();

            if (sourcePaths != null) {
                for (String sourcePath : sourcePaths) {
                    sourcePath(sourcePath);
                }
            }

            return this;
        }

        public Builder epubType(String epubType) {
            String normalized = normalize(epubType);

            if (!normalized.isBlank()) {
                epubTypes.add(normalized);
            }

            return this;
        }

        public Builder epubTypes(
            Set<String> epubTypes
        ) {
            this.epubTypes.clear();

            if (epubTypes != null) {
                for (String epubType : epubTypes) {
                    epubType(epubType);
                }
            }

            return this;
        }

        public Builder language(String language) {
            String normalized =
                normalizeLanguage(language);

            if (!normalized.isBlank()) {
                languages.add(normalized);
            }

            return this;
        }

        public Builder languages(
            Set<String> languages
        ) {
            this.languages.clear();

            if (languages != null) {
                for (String language : languages) {
                    language(language);
                }
            }

            return this;
        }

        public Builder metadataFilter(
            String key,
            String value
        ) {
            String normalizedKey = normalize(key);

            if (!normalizedKey.isBlank()) {
                metadataFilters.put(
                    normalizedKey,
                    normalize(value)
                );
            }

            return this;
        }

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
            this.includeRejected = includeRejected;
            return this;
        }

        public VectorSearchRequest build() {
            return new VectorSearchRequest(this);
        }
    }
}