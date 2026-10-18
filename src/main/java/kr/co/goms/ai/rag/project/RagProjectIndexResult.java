/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kr.co.goms.ai.rag.index.RagIndexIssue;
import kr.co.goms.ai.rag.index.RagIndexResult;

/**
 * EPUB 프로젝트 전체 RAG 인덱싱 결과입니다.
 *
 * <p>프로젝트에서 발견한 문서 수, 문서별 인덱싱 결과,
 * Chunk 처리 건수와 삭제된 문서 인덱스 정보를 집계합니다.</p>
 */
public final class RagProjectIndexResult {

    /**
     * 인덱싱한 EPUB 프로젝트 루트 경로입니다.
     */
    private final String projectRoot;

    /**
     * 인덱싱에 사용한 임베딩 모델명입니다.
     */
    private final String model;

    /**
     * 프로젝트에서 발견한 지원 문서 수입니다.
     */
    private final int discoveredDocumentCount;

    /**
     * 실제 처리한 문서 수입니다.
     */
    private final int processedDocumentCount;

    /**
     * 성공한 문서 수입니다.
     */
    private final int successfulDocumentCount;

    /**
     * 실패한 문서 수입니다.
     */
    private final int failedDocumentCount;

    /**
     * 새로 임베딩하고 저장한 Chunk 수입니다.
     */
    private final int indexedChunkCount;

    /**
     * 기존 벡터를 재사용한 Chunk 수입니다.
     */
    private final int reusedChunkCount;

    /**
     * 처리에서 제외한 Chunk 수입니다.
     */
    private final int skippedChunkCount;

    /**
     * 처리에 실패한 Chunk 수입니다.
     */
    private final int failedChunkCount;

    /**
     * 프로젝트에서 사라져 인덱스를 제거한 문서 수입니다.
     */
    private final int removedDocumentCount;

    /**
     * 삭제한 전체 VectorRecord 수입니다.
     */
    private final int deletedRecordCount;

    /**
     * 문서별 인덱싱 결과입니다.
     */
    private final List<RagIndexResult> documentResults;

    /**
     * 프로젝트에서 제거된 문서의 상대 경로 목록입니다.
     */
    private final List<String> removedSourcePaths;

    /**
     * 프로젝트 전체 인덱싱 처리 시간입니다.
     *
     * <p>나노초 단위입니다.</p>
     */
    private final long durationNanos;

    /**
     * 전체 프로젝트 인덱싱 성공 여부입니다.
     */
    private final boolean success;

    /**
     * 결과 생성 시각입니다.
     *
     * <p>Epoch milliseconds 단위입니다.</p>
     */
    private final long createdAt;

    private RagProjectIndexResult(
        Builder builder
    ) {
        this.projectRoot =
            requireText(
                builder.projectRoot,
                "projectRoot"
            );

        this.model =
            normalize(builder.model);

        this.discoveredDocumentCount =
            validateNonNegative(
                builder.discoveredDocumentCount,
                "discoveredDocumentCount"
            );

        this.processedDocumentCount =
            validateNonNegative(
                builder.processedDocumentCount,
                "processedDocumentCount"
            );

        this.successfulDocumentCount =
            validateNonNegative(
                builder.successfulDocumentCount,
                "successfulDocumentCount"
            );

        this.failedDocumentCount =
            validateNonNegative(
                builder.failedDocumentCount,
                "failedDocumentCount"
            );

        this.indexedChunkCount =
            validateNonNegative(
                builder.indexedChunkCount,
                "indexedChunkCount"
            );

        this.reusedChunkCount =
            validateNonNegative(
                builder.reusedChunkCount,
                "reusedChunkCount"
            );

        this.skippedChunkCount =
            validateNonNegative(
                builder.skippedChunkCount,
                "skippedChunkCount"
            );

        this.failedChunkCount =
            validateNonNegative(
                builder.failedChunkCount,
                "failedChunkCount"
            );

        this.removedDocumentCount =
            validateNonNegative(
                builder.removedDocumentCount,
                "removedDocumentCount"
            );

        this.deletedRecordCount =
            validateNonNegative(
                builder.deletedRecordCount,
                "deletedRecordCount"
            );

        this.documentResults =
            immutableResults(
                builder.documentResults
            );

        this.removedSourcePaths =
            immutablePaths(
                builder.removedSourcePaths
            );

        this.durationNanos =
            validateNonNegative(
                builder.durationNanos,
                "durationNanos"
            );

        this.success =
            builder.success;

        this.createdAt =
            builder.createdAt <= 0L
                ? System.currentTimeMillis()
                : builder.createdAt;

        validateCounts();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 문서가 없는 빈 프로젝트 인덱싱 결과를 생성합니다.
     */
    public static RagProjectIndexResult empty(
        String projectRoot,
        String model
    ) {
        return builder()
            .projectRoot(projectRoot)
            .model(model)
            .success(true)
            .build();
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public String getModel() {
        return model;
    }

    public int getDiscoveredDocumentCount() {
        return discoveredDocumentCount;
    }

    public int getProcessedDocumentCount() {
        return processedDocumentCount;
    }

    public int getSuccessfulDocumentCount() {
        return successfulDocumentCount;
    }

    public int getFailedDocumentCount() {
        return failedDocumentCount;
    }

    public int getIndexedChunkCount() {
        return indexedChunkCount;
    }

    public int getReusedChunkCount() {
        return reusedChunkCount;
    }

    public int getSkippedChunkCount() {
        return skippedChunkCount;
    }

    public int getFailedChunkCount() {
        return failedChunkCount;
    }

    public int getRemovedDocumentCount() {
        return removedDocumentCount;
    }

    public int getDeletedRecordCount() {
        return deletedRecordCount;
    }

    public List<RagIndexResult> getDocumentResults() {
        return documentResults;
    }

    public List<String> getRemovedSourcePaths() {
        return removedSourcePaths;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public double getDurationMillis() {
        return durationNanos
            / 1_000_000.0;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean hasFailures() {
        return failedDocumentCount > 0
            || failedChunkCount > 0;
    }

    public boolean hasRemovedDocuments() {
        return removedDocumentCount > 0;
    }

    public boolean hasIndexedChunks() {
        return indexedChunkCount > 0;
    }

    public boolean hasReusedChunks() {
        return reusedChunkCount > 0;
    }

    public boolean isEmpty() {
        return discoveredDocumentCount == 0
            && processedDocumentCount == 0
            && documentResults.isEmpty();
    }

    /**
     * 전체 Chunk 처리 건수를 반환합니다.
     */
    public int getProcessedChunkCount() {
        return indexedChunkCount
            + reusedChunkCount
            + skippedChunkCount
            + failedChunkCount;
    }

    /**
     * 성공적으로 처리된 Chunk 수를 반환합니다.
     */
    public int getSuccessfulChunkCount() {
        return indexedChunkCount
            + reusedChunkCount
            + skippedChunkCount;
    }

    /**
     * 문서별 결과에 포함된 모든 이슈를 반환합니다.
     */
    public List<RagIndexIssue> getIssues() {
        if (documentResults.isEmpty()) {
            return List.of();
        }

        List<RagIndexIssue> issues =
            new ArrayList<>();

        for (RagIndexResult result
            : documentResults) {

            if (result == null
                || result.getIssues().isEmpty()) {

                continue;
            }

            issues.addAll(
                result.getIssues()
            );
        }

        return List.copyOf(issues);
    }

    public boolean hasIssues() {
        for (RagIndexResult result
            : documentResults) {

            if (result != null
                && result.hasIssues()) {

                return true;
            }
        }

        return false;
    }

    /**
     * 지정된 상대 경로의 문서 인덱싱 결과를 반환합니다.
     */
    public RagIndexResult findDocumentResult(
        String sourcePath
    ) {
        String normalizedPath =
            normalizePath(sourcePath);

        if (normalizedPath.isBlank()) {
            return null;
        }

        for (RagIndexResult result
            : documentResults) {

            if (normalizePath(
                result.getSourcePath()
            ).equals(normalizedPath)) {

                return result;
            }
        }

        return null;
    }

    /**
     * 문서 처리 성공률을 반환합니다.
     *
     * <p>처리한 문서가 없으면 1.0을 반환합니다.</p>
     */
    public double getDocumentSuccessRate() {
        if (processedDocumentCount == 0) {
            return 1.0;
        }

        return (double) successfulDocumentCount
            / processedDocumentCount;
    }

    /**
     * Chunk 처리 성공률을 반환합니다.
     *
     * <p>처리한 Chunk가 없으면 1.0을 반환합니다.</p>
     */
    public double getChunkSuccessRate() {
        int processedChunkCount =
            getProcessedChunkCount();

        if (processedChunkCount == 0) {
            return 1.0;
        }

        return (double) getSuccessfulChunkCount()
            / processedChunkCount;
    }

    /**
     * 집계값과 문서 결과 수의 정합성을 검증합니다.
     */
    private void validateCounts() {
        if (successfulDocumentCount
                + failedDocumentCount
            > processedDocumentCount) {

            throw new IllegalArgumentException(
                "successfulDocumentCount + failedDocumentCount "
                    + "must not exceed processedDocumentCount"
            );
        }

        if (processedDocumentCount
            > discoveredDocumentCount) {

            throw new IllegalArgumentException(
                "processedDocumentCount must not exceed "
                    + "discoveredDocumentCount"
            );
        }

        if (!documentResults.isEmpty()
            && documentResults.size()
                != processedDocumentCount) {

            throw new IllegalArgumentException(
                "documentResults size must match "
                    + "processedDocumentCount"
            );
        }

        if (!removedSourcePaths.isEmpty()
            && removedSourcePaths.size()
                != removedDocumentCount) {

            throw new IllegalArgumentException(
                "removedSourcePaths size must match "
                    + "removedDocumentCount"
            );
        }
    }

    private static List<RagIndexResult>
        immutableResults(
            List<RagIndexResult> values
        ) {

        if (values == null
            || values.isEmpty()) {

            return Collections.emptyList();
        }

        List<RagIndexResult> copy =
            new ArrayList<>(values.size());

        for (RagIndexResult value : values) {
            if (value != null) {
                copy.add(value);
            }
        }

        return Collections.unmodifiableList(
            copy
        );
    }

    private static List<String> immutablePaths(
        List<String> values
    ) {
        if (values == null
            || values.isEmpty()) {

            return Collections.emptyList();
        }

        List<String> copy =
            new ArrayList<>(values.size());

        for (String value : values) {
            String normalized =
                normalizePath(value);

            if (!normalized.isBlank()) {
                copy.add(normalized);
            }
        }

        return Collections.unmodifiableList(
            copy
        );
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
            normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName
                    + " must not be blank"
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

    @Override
    public String toString() {
        return "RagProjectIndexResult{" +
            "projectRoot='" + projectRoot + '\'' +
            ", model='" + model + '\'' +
            ", discoveredDocumentCount="
                + discoveredDocumentCount +
            ", processedDocumentCount="
                + processedDocumentCount +
            ", successfulDocumentCount="
                + successfulDocumentCount +
            ", failedDocumentCount="
                + failedDocumentCount +
            ", indexedChunkCount="
                + indexedChunkCount +
            ", reusedChunkCount="
                + reusedChunkCount +
            ", skippedChunkCount="
                + skippedChunkCount +
            ", failedChunkCount="
                + failedChunkCount +
            ", removedDocumentCount="
                + removedDocumentCount +
            ", deletedRecordCount="
                + deletedRecordCount +
            ", durationNanos="
                + durationNanos +
            ", success="
                + success +
            '}';
    }

    public static final class Builder {

        private String projectRoot;
        private String model;

        private int discoveredDocumentCount;
        private int processedDocumentCount;
        private int successfulDocumentCount;
        private int failedDocumentCount;

        private int indexedChunkCount;
        private int reusedChunkCount;
        private int skippedChunkCount;
        private int failedChunkCount;

        private int removedDocumentCount;
        private int deletedRecordCount;

        private final List<RagIndexResult> documentResults =
            new ArrayList<>();

        private final List<String> removedSourcePaths =
            new ArrayList<>();

        private long durationNanos;
        private boolean success = true;
        private long createdAt;

        private Builder() {
        }

        public Builder projectRoot(
            String projectRoot
        ) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Builder model(
            String model
        ) {
            this.model = model;
            return this;
        }

        public Builder discoveredDocumentCount(
            int discoveredDocumentCount
        ) {
            this.discoveredDocumentCount =
                discoveredDocumentCount;

            return this;
        }

        public Builder processedDocumentCount(
            int processedDocumentCount
        ) {
            this.processedDocumentCount =
                processedDocumentCount;

            return this;
        }

        public Builder successfulDocumentCount(
            int successfulDocumentCount
        ) {
            this.successfulDocumentCount =
                successfulDocumentCount;

            return this;
        }

        public Builder failedDocumentCount(
            int failedDocumentCount
        ) {
            this.failedDocumentCount =
                failedDocumentCount;

            return this;
        }

        public Builder indexedChunkCount(
            int indexedChunkCount
        ) {
            this.indexedChunkCount =
                indexedChunkCount;

            return this;
        }

        public Builder reusedChunkCount(
            int reusedChunkCount
        ) {
            this.reusedChunkCount =
                reusedChunkCount;

            return this;
        }

        public Builder skippedChunkCount(
            int skippedChunkCount
        ) {
            this.skippedChunkCount =
                skippedChunkCount;

            return this;
        }

        public Builder failedChunkCount(
            int failedChunkCount
        ) {
            this.failedChunkCount =
                failedChunkCount;

            return this;
        }

        public Builder removedDocumentCount(
            int removedDocumentCount
        ) {
            this.removedDocumentCount =
                removedDocumentCount;

            return this;
        }

        public Builder deletedRecordCount(
            int deletedRecordCount
        ) {
            this.deletedRecordCount =
                deletedRecordCount;

            return this;
        }

        public Builder documentResult(
            RagIndexResult documentResult
        ) {
            if (documentResult != null) {
                documentResults.add(
                    documentResult
                );
            }

            return this;
        }

        public Builder documentResults(
            List<RagIndexResult> documentResults
        ) {
            this.documentResults.clear();

            if (documentResults != null) {
                for (RagIndexResult result
                    : documentResults) {

                    documentResult(result);
                }
            }

            return this;
        }

        public Builder removedSourcePath(
            String removedSourcePath
        ) {
            String normalized =
                normalizePath(
                    removedSourcePath
                );

            if (!normalized.isBlank()) {
                removedSourcePaths.add(
                    normalized
                );
            }

            return this;
        }

        public Builder removedSourcePaths(
            List<String> removedSourcePaths
        ) {
            this.removedSourcePaths.clear();

            if (removedSourcePaths != null) {
                for (String sourcePath
                    : removedSourcePaths) {

                    removedSourcePath(
                        sourcePath
                    );
                }
            }

            return this;
        }

        public Builder durationNanos(
            long durationNanos
        ) {
            this.durationNanos =
                durationNanos;

            return this;
        }

        public Builder success(
            boolean success
        ) {
            this.success = success;
            return this;
        }

        public Builder createdAt(
            long createdAt
        ) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * 문서별 결과를 기준으로 집계값을 자동 계산합니다.
         *
         * <p>직접 설정한 Chunk 집계값도 문서 결과 기준 값으로
         * 덮어씁니다.</p>
         */
        public Builder summarizeDocumentResults() {
            int successfulDocuments = 0;
            int failedDocuments = 0;

            int indexedChunks = 0;
            int reusedChunks = 0;
            int skippedChunks = 0;
            int failedChunks = 0;

            for (RagIndexResult result
                : documentResults) {

                if (result.isSuccess()) {
                    successfulDocuments++;
                } else {
                    failedDocuments++;
                }

                indexedChunks +=
                    result.getIndexedCount();

                reusedChunks +=
                    result.getReusedCount();

                skippedChunks +=
                    result.getSkippedCount();

                failedChunks +=
                    result.getFailedCount();
            }

            this.processedDocumentCount =
                documentResults.size();

            this.successfulDocumentCount =
                successfulDocuments;

            this.failedDocumentCount =
                failedDocuments;

            this.indexedChunkCount =
                indexedChunks;

            this.reusedChunkCount =
                reusedChunks;

            this.skippedChunkCount =
                skippedChunks;

            this.failedChunkCount =
                failedChunks;

            this.success =
                failedDocuments == 0
                    && failedChunks == 0;

            return this;
        }

        public RagProjectIndexResult build() {
            return new RagProjectIndexResult(
                this
            );
        }
    }
}