/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

/**
 * RAG 문서 인덱싱 설정입니다.
 */
public final class RagIndexRequest {

    public static final int DEFAULT_BATCH_SIZE = 16;

    /**
     * 임베딩 배치 크기입니다.
     */
    private final int batchSize;

    /**
     * 기존 레코드가 존재하더라도 강제로 다시 임베딩할지 여부입니다.
     */
    private final boolean forceReindex;

    /**
     * 인덱싱 전에 원본 문서의 기존 레코드를 삭제할지 여부입니다.
     */
    private final boolean replaceSource;

    /**
     * 내용 해시가 동일한 레코드는 재사용할지 여부입니다.
     */
    private final boolean reuseUnchanged;

    /**
     * 문서 처리 중 오류가 발생해도 다음 문서를 계속 처리할지 여부입니다.
     */
    private final boolean continueOnError;

    /**
     * 임베딩 벡터를 L2 정규화할지 여부입니다.
     */
    private final boolean normalize;

    /**
     * 입력이 모델 컨텍스트를 초과하면 잘라낼지 여부입니다.
     */
    private final boolean truncate;

    /**
     * 새 인덱스 버전입니다.
     */
    private final long version;

    private RagIndexRequest(Builder builder) {
        this.batchSize =
            validateBatchSize(builder.batchSize);

        this.forceReindex =
            builder.forceReindex;

        this.replaceSource =
            builder.replaceSource;

        this.reuseUnchanged =
            builder.reuseUnchanged;

        this.continueOnError =
            builder.continueOnError;

        this.normalize =
            builder.normalize;

        this.truncate =
            builder.truncate;

        this.version =
            validateVersion(builder.version);

        if (forceReindex && reuseUnchanged) {
            throw new IllegalArgumentException(
                "forceReindex and reuseUnchanged cannot both be true"
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RagIndexRequest defaults() {
        return builder().build();
    }

    /**
     * 전체 재인덱싱 설정을 생성합니다.
     */
    public static RagIndexRequest force() {
        return builder()
            .forceReindex(true)
            .reuseUnchanged(false)
            .replaceSource(true)
            .build();
    }

    public int getBatchSize() {
        return batchSize;
    }

    public boolean isForceReindex() {
        return forceReindex;
    }

    public boolean isReplaceSource() {
        return replaceSource;
    }

    public boolean isReuseUnchanged() {
        return reuseUnchanged;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public boolean isNormalize() {
        return normalize;
    }

    public boolean isTruncate() {
        return truncate;
    }

    public long getVersion() {
        return version;
    }

    private static int validateBatchSize(
        int batchSize
    ) {
        if (batchSize < 1) {
            throw new IllegalArgumentException(
                "batchSize must be greater than zero"
            );
        }

        return batchSize;
    }

    private static long validateVersion(
        long version
    ) {
        if (version < 1L) {
            throw new IllegalArgumentException(
                "version must be greater than zero"
            );
        }

        return version;
    }

    @Override
    public String toString() {
        return "RagIndexRequest{" +
            "batchSize=" + batchSize +
            ", forceReindex=" + forceReindex +
            ", replaceSource=" + replaceSource +
            ", reuseUnchanged=" + reuseUnchanged +
            ", continueOnError=" + continueOnError +
            ", normalize=" + normalize +
            ", truncate=" + truncate +
            ", version=" + version +
            '}';
    }

    public static final class Builder {

        private int batchSize =
            DEFAULT_BATCH_SIZE;

        private boolean forceReindex;
        private boolean replaceSource;
        private boolean reuseUnchanged = true;
        private boolean continueOnError;
        private boolean normalize = true;
        private boolean truncate = true;
        private long version = 1L;

        private Builder() {
        }

        public Builder batchSize(
            int batchSize
        ) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder forceReindex(
            boolean forceReindex
        ) {
            this.forceReindex = forceReindex;

            if (forceReindex) {
                this.reuseUnchanged = false;
            }

            return this;
        }

        public Builder replaceSource(
            boolean replaceSource
        ) {
            this.replaceSource = replaceSource;
            return this;
        }

        public Builder reuseUnchanged(
            boolean reuseUnchanged
        ) {
            this.reuseUnchanged = reuseUnchanged;

            if (reuseUnchanged) {
                this.forceReindex = false;
            }

            return this;
        }

        public Builder continueOnError(
            boolean continueOnError
        ) {
            this.continueOnError =
                continueOnError;

            return this;
        }

        public Builder normalize(
            boolean normalize
        ) {
            this.normalize = normalize;
            return this;
        }

        public Builder truncate(
            boolean truncate
        ) {
            this.truncate = truncate;
            return this;
        }

        public Builder version(
            long version
        ) {
            this.version = version;
            return this;
        }

        public RagIndexRequest build() {
            return new RagIndexRequest(this);
        }
    }
}