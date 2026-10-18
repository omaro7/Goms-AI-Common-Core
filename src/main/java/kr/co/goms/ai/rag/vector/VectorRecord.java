/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.vector;

import java.util.Objects;

import kr.co.goms.ai.rag.model.DocumentChunk;

/**
 * VectorStore에 저장되는 하나의 문서 벡터 레코드입니다.
 *
 * <p>문서 Chunk와 해당 Chunk에서 생성한 임베딩 벡터,
 * 임베딩 모델 및 인덱싱 정보를 함께 관리합니다.</p>
 *
 * <pre>
 * DocumentChunk
 *      +
 * Embedding Vector
 *      ↓
 * VectorRecord
 *      ↓
 * VectorStore
 * </pre>
 */
public final class VectorRecord {

    /**
     * 벡터 레코드 고유 식별자입니다.
     *
     * 일반적으로 DocumentChunk ID와 동일한 값을 사용합니다.
     */
    private final String id;

    /**
     * 임베딩 대상 문서 Chunk입니다.
     */
    private final DocumentChunk chunk;

    /**
     * 임베딩 벡터입니다.
     */
    private final float[] vector;

    /**
     * 임베딩 생성에 사용된 모델명입니다.
     *
     * 예:
     * nomic-embed-text
     * bge-m3
     */
    private final String model;

    /**
     * 임베딩 벡터 차원입니다.
     */
    private final int dimensions;

    /**
     * 임베딩 대상 텍스트의 해시입니다.
     *
     * Chunk 내용이 변경되었는지 확인하여 증분 인덱싱에 사용합니다.
     */
    private final String contentHash;

    /**
     * 원본 DocumentSource 전체 내용의 해시입니다.
     *
     * 문서 파일 단위 변경 여부를 확인하는 데 사용할 수 있습니다.
     */
    private final String sourceHash;

    /**
     * 벡터가 L2 정규화되었는지 여부입니다.
     */
    private final boolean normalized;

    /**
     * 인덱싱 완료 시각입니다.
     *
     * Epoch milliseconds 단위입니다.
     */
    private final long indexedAt;

    /**
     * 레코드 생성 또는 갱신 버전입니다.
     *
     * VectorStore 스키마나 인덱싱 정책 변경 시 사용할 수 있습니다.
     */
    private final long version;
    
    /**
     * 이 VectorRecord가 속한 EPUB 프로젝트 식별자입니다.
     */
    private final String projectId;

    private VectorRecord(Builder builder) {
    	
        this.projectId =
                requireText(
                        builder.projectId,
                        "projectId"
                );
        
        this.id = requireText(builder.id, "id");

        this.chunk = Objects.requireNonNull(
            builder.chunk,
            "chunk must not be null"
        );

        this.vector = copyAndValidateVector(builder.vector);
        this.dimensions = vector.length;

        this.model = requireText(
            builder.model,
            "model"
        );

        this.contentHash = normalize(builder.contentHash);
        this.sourceHash = normalize(builder.sourceHash);
        this.normalized = builder.normalized;

        this.indexedAt = resolveIndexedAt(
            builder.indexedAt
        );

        this.version = validateVersion(
            builder.version
        );

        validateIdConsistency();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기본 VectorRecord를 생성합니다.
     *
     * @param projectId EPUB 프로젝트 식별자
     * @param chunk 문서 Chunk
     * @param vector 임베딩 벡터
     * @param model 임베딩 모델명
     * @return 벡터 레코드
     */
    public static VectorRecord of(
        String projectId,
        DocumentChunk chunk,
        float[] vector,
        String model
    ) {
        Objects.requireNonNull(
            chunk,
            "chunk must not be null"
        );

        return builder()
    		.projectId(projectId)
            .id(chunk.getId())
            .chunk(chunk)
            .vector(vector)
            .model(model)
            .indexedAt(System.currentTimeMillis())
            .build();
    }
    
    public String getProjectId() {
        return projectId;
    }

    public String getId() {
        return id;
    }

    public DocumentChunk getChunk() {
        return chunk;
    }

    /**
     * 임베딩 벡터의 복사본을 반환합니다.
     */
    public float[] getVector() {
        return vector.clone();
    }

    public String getModel() {
        return model;
    }

    public int getDimensions() {
        return dimensions;
    }

    public String getContentHash() {
        return contentHash;
    }

    public boolean hasContentHash() {
        return !contentHash.isBlank();
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public boolean hasSourceHash() {
        return !sourceHash.isBlank();
    }

    public boolean isNormalized() {
        return normalized;
    }

    public long getIndexedAt() {
        return indexedAt;
    }

    public long getVersion() {
        return version;
    }

    public boolean isProject(
            String projectId) {

        if (projectId == null) {
            return false;
        }

        return this.projectId.equals(
                projectId.trim()
        );
    }
    
    /**
     * 지정한 모델로 생성된 레코드인지 확인합니다.
     */
    public boolean isModel(String model) {
        if (model == null) {
            return false;
        }

        return this.model.equals(model.trim());
    }

    /**
     * 지정된 벡터 차원과 동일한지 확인합니다.
     */
    public boolean hasDimensions(int dimensions) {
        return this.dimensions == dimensions;
    }

    /**
     * Chunk 내용이 변경되었는지 확인합니다.
     *
     * @param newContentHash 현재 Chunk 내용 해시
     * @return 내용 변경 여부
     */
    public boolean isContentChanged(
        String newContentHash
    ) {
        String normalizedHash =
            normalize(newContentHash);

        if (contentHash.isBlank()) {
            return true;
        }

        return !contentHash.equals(normalizedHash);
    }

    /**
     * 원본 문서가 변경되었는지 확인합니다.
     *
     * @param newSourceHash 현재 원본 문서 해시
     * @return 원본 문서 변경 여부
     */
    public boolean isSourceChanged(
        String newSourceHash
    ) {
        String normalizedHash =
            normalize(newSourceHash);

        if (sourceHash.isBlank()) {
            return true;
        }

        return !sourceHash.equals(normalizedHash);
    }

    /**
     * 특정 모델 및 차원과 호환되는지 확인합니다.
     *
     * 임베딩 모델 변경 시 기존 인덱스를 재사용할 수 있는지 판단할 때
     * 사용합니다.
     */
    public boolean isCompatible(
        String model,
        int dimensions
    ) {
        return isModel(model)
            && hasDimensions(dimensions);
    }

    /**
     * 벡터의 L2 크기를 계산합니다.
     */
    public double calculateMagnitude() {
        double squaredSum = 0.0;

        for (float value : vector) {
            squaredSum += (double) value * value;
        }

        return Math.sqrt(squaredSum);
    }

    /**
     * 정규화 여부를 실제 벡터 크기로 검증합니다.
     *
     * @param tolerance 허용 오차
     * @return 단위 벡터 여부
     */
    public boolean isUnitVector(double tolerance) {
        if (tolerance < 0.0
            || !Double.isFinite(tolerance)) {

            throw new IllegalArgumentException(
                "tolerance must be a finite non-negative value"
            );
        }

        double magnitude = calculateMagnitude();

        return Math.abs(magnitude - 1.0)
            <= tolerance;
    }

    private void validateIdConsistency() {
        if (!id.equals(chunk.getId())) {
            throw new IllegalArgumentException(
                "VectorRecord id must match DocumentChunk id. "
                    + "recordId="
                    + id
                    + ", chunkId="
                    + chunk.getId()
            );
        }
    }

    private static float[] copyAndValidateVector(
        float[] vector
    ) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException(
                "vector must not be null or empty"
            );
        }

        float[] copy = vector.clone();

        for (int index = 0;
             index < copy.length;
             index++) {

            float value = copy[index];

            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(
                    "vector contains invalid value at index "
                        + index
                );
            }
        }

        return copy;
    }

    private static long resolveIndexedAt(
        long indexedAt
    ) {
        if (indexedAt < 0) {
            throw new IllegalArgumentException(
                "indexedAt must be greater than or equal to zero"
            );
        }

        return indexedAt == 0
            ? System.currentTimeMillis()
            : indexedAt;
    }

    private static long validateVersion(
        long version
    ) {
        if (version < 1) {
            throw new IllegalArgumentException(
                "version must be greater than zero"
            );
        }

        return version;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof VectorRecord)) {
            return false;
        }

        VectorRecord other =
            (VectorRecord) object;

        return projectId.equals(
                other.projectId
            )
            && id.equals(
                    other.id
            )
            && model.equals(
                    other.model
            );
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                projectId,
                id,
                model
        );
    }

    @Override
    public String toString() {
        return "VectorRecord{" +
        	"projectId='" + projectId + '\'' +
            ", id='" + id + '\'' +
            ", chunkType=" + chunk.getType() +
            ", sourcePath='" + chunk.getSourcePath() + '\'' +
            ", model='" + model + '\'' +
            ", dimensions=" + dimensions +
            ", contentHash='" + contentHash + '\'' +
            ", sourceHash='" + sourceHash + '\'' +
            ", normalized=" + normalized +
            ", indexedAt=" + indexedAt +
            ", version=" + version +
            '}';
    }

    public static final class Builder {
    	private String projectId;
        private String id;
        private DocumentChunk chunk;
        private float[] vector;
        private String model;
        private String contentHash;
        private String sourceHash;
        private boolean normalized;
        private long indexedAt;
        private long version = 1L;

        private Builder() {
        }

        public Builder projectId(String projectId) {
            this.projectId =
                    projectId;

            return this;
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder chunk(DocumentChunk chunk) {
            this.chunk = chunk;

            /*
             * ID가 별도로 지정되지 않았으면 Chunk ID를 기본값으로 사용합니다.
             */
            if (chunk != null
                && (id == null || id.isBlank())) {

                this.id = chunk.getId();
            }

            return this;
        }

        public Builder vector(float[] vector) {
            this.vector = vector == null
                ? null
                : vector.clone();

            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder contentHash(
            String contentHash
        ) {
            this.contentHash = contentHash;
            return this;
        }

        public Builder sourceHash(
            String sourceHash
        ) {
            this.sourceHash = sourceHash;
            return this;
        }

        public Builder normalized(
            boolean normalized
        ) {
            this.normalized = normalized;
            return this;
        }

        public Builder indexedAt(long indexedAt) {
            this.indexedAt = indexedAt;
            return this;
        }

        public Builder version(long version) {
            this.version = version;
            return this;
        }

        public VectorRecord build() {
            return new VectorRecord(this);
        }
    }
}