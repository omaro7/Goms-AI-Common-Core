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
 * EPUB 프로젝트 파일 상태와 기존 RAG 인덱스의 동기화 결과입니다.
 *
 * <p>현재 프로젝트에서 발견된 문서, 기존에 인덱싱된 문서,
 * 삭제된 문서와 문서별 재인덱싱 결과를 함께 보관합니다.</p>
 *
 * <pre>
 * 현재 프로젝트 문서
 *      +
 * 기존 VectorStore 인덱스
 *      ↓
 * 비교 및 동기화
 *      ├─ 신규 문서 인덱싱
 *      ├─ 변경 문서 재인덱싱
 *      ├─ 기존 Chunk 재사용
 *      └─ 삭제 문서 인덱스 제거
 *      ↓
 * RagProjectSyncResult
 * </pre>
 */
public final class RagProjectSyncResult {

    /**
     * 동기화한 EPUB 프로젝트 루트 경로입니다.
     */
    private final String projectRoot;

    /**
     * 동기화에 사용한 임베딩 모델명입니다.
     */
    private final String model;

    /**
     * 현재 프로젝트에서 발견된 지원 문서 수입니다.
     */
    private final int discoveredDocumentCount;

    /**
     * 동기화 이전 VectorStore에 존재하던 문서 수입니다.
     */
    private final int previouslyIndexedDocumentCount;

    /**
     * 기존 인덱스에는 있지만 현재 프로젝트에는 없는 문서 수입니다.
     *
     * <p>삭제 설정이 비활성화된 경우 실제 제거 문서 수보다 클 수 있습니다.</p>
     */
    private final int missingDocumentCount;

    /**
     * 실제로 인덱스를 제거한 문서 수입니다.
     */
    private final int removedDocumentCount;

    /**
     * 삭제된 전체 VectorRecord 수입니다.
     */
    private final int deletedRecordCount;

    /**
     * 현재 프로젝트 문서의 인덱싱 결과입니다.
     */
    private final List<RagIndexResult> indexResults;

    /**
     * 인덱스가 제거된 문서의 프로젝트 상대 경로입니다.
     */
    private final List<String> removedSourcePaths;

    /**
     * 새로 임베딩하고 저장한 Chunk 수입니다.
     */
    private final int indexedChunkCount;

    /**
     * 기존 VectorRecord를 재사용한 Chunk 수입니다.
     */
    private final int reusedChunkCount;

    /**
     * 처리에서 제외된 Chunk 수입니다.
     */
    private final int skippedChunkCount;

    /**
     * 처리에 실패한 Chunk 수입니다.
     */
    private final int failedChunkCount;

    /**
     * 성공적으로 처리된 문서 수입니다.
     */
    private final int successfulDocumentCount;

    /**
     * 처리에 실패한 문서 수입니다.
     */
    private final int failedDocumentCount;

    /**
     * 프로젝트 동기화 전체 처리 시간입니다.
     *
     * <p>나노초 단위입니다.</p>
     */
    private final long durationNanos;

    /**
     * 동기화 성공 여부입니다.
     */
    private final boolean success;

    /**
     * 결과 생성 시각입니다.
     *
     * <p>Epoch milliseconds 단위입니다.</p>
     */
    private final long createdAt;

    private RagProjectSyncResult(
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

        this.previouslyIndexedDocumentCount =
            validateNonNegative(
                builder.previouslyIndexedDocumentCount,
                "previouslyIndexedDocumentCount"
            );

        this.missingDocumentCount =
            validateNonNegative(
                builder.missingDocumentCount,
                "missingDocumentCount"
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

        this.indexResults =
            immutableResults(
                builder.indexResults
            );

        this.removedSourcePaths =
            immutablePaths(
                builder.removedSourcePaths
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
     * 빈 프로젝트 동기화 결과를 생성합니다.
     */
    public static RagProjectSyncResult empty(
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

    public int getPreviouslyIndexedDocumentCount() {
        return previouslyIndexedDocumentCount;
    }

    public int getMissingDocumentCount() {
        return missingDocumentCount;
    }

    public int getRemovedDocumentCount() {
        return removedDocumentCount;
    }

    public int getDeletedRecordCount() {
        return deletedRecordCount;
    }

    public List<RagIndexResult> getIndexResults() {
        return indexResults;
    }

    public List<String> getRemovedSourcePaths() {
        return removedSourcePaths;
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

    public int getSuccessfulDocumentCount() {
        return successfulDocumentCount;
    }

    public int getFailedDocumentCount() {
        return failedDocumentCount;
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

    /**
     * 현재 프로젝트 문서 중 실제 처리한 문서 수를 반환합니다.
     */
    public int getProcessedDocumentCount() {
        return indexResults.size();
    }

    /**
     * 동기화에서 처리한 전체 Chunk 수를 반환합니다.
     */
    public int getProcessedChunkCount() {
        return indexedChunkCount
            + reusedChunkCount
            + skippedChunkCount
            + failedChunkCount;
    }

    /**
     * 성공적으로 처리한 Chunk 수를 반환합니다.
     */
    public int getSuccessfulChunkCount() {
        return indexedChunkCount
            + reusedChunkCount
            + skippedChunkCount;
    }

    /**
     * 새로 발견된 문서의 추정 수를 반환합니다.
     *
     * <p>현재 문서 수에서 기존 인덱싱 문서 중 여전히 존재하는 문서 수를
     * 제외하여 계산합니다.</p>
     */
    public int getEstimatedNewDocumentCount() {
        int existingDocumentsStillPresent =
            Math.max(
                0,
                previouslyIndexedDocumentCount
                    - missingDocumentCount
            );

        return Math.max(
            0,
            discoveredDocumentCount
                - existingDocumentsStillPresent
        );
    }

    public boolean hasMissingDocuments() {
        return missingDocumentCount > 0;
    }

    public boolean hasRemovedDocuments() {
        return removedDocumentCount > 0;
    }

    public boolean hasFailures() {
        return failedDocumentCount > 0
            || failedChunkCount > 0;
    }

    public boolean hasChanges() {
        return indexedChunkCount > 0
            || removedDocumentCount > 0
            || deletedRecordCount > 0;
    }

    public boolean isUnchanged() {
        return !hasChanges()
            && failedChunkCount == 0
            && failedDocumentCount == 0;
    }

    public boolean isEmpty() {
        return discoveredDocumentCount == 0
            && previouslyIndexedDocumentCount == 0
            && indexResults.isEmpty()
            && removedSourcePaths.isEmpty();
    }

    /**
     * 문서 처리 성공률을 반환합니다.
     *
     * <p>처리된 문서가 없으면 1.0을 반환합니다.</p>
     */
    public double getDocumentSuccessRate() {
        int processedDocumentCount =
            getProcessedDocumentCount();

        if (processedDocumentCount == 0) {
            return 1.0;
        }

        return (double) successfulDocumentCount
            / processedDocumentCount;
    }

    /**
     * Chunk 처리 성공률을 반환합니다.
     *
     * <p>처리된 Chunk가 없으면 1.0을 반환합니다.</p>
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
     * 지정한 원본 문서의 인덱싱 결과를 반환합니다.
     */
    public RagIndexResult findIndexResult(
        String sourcePath
    ) {
        String normalizedPath =
            normalizePath(sourcePath);

        if (normalizedPath.isBlank()) {
            return null;
        }

        for (RagIndexResult result
            : indexResults) {

            if (normalizePath(
                result.getSourcePath()
            ).equals(normalizedPath)) {

                return result;
            }
        }

        return null;
    }

    /**
     * 특정 문서의 인덱스가 제거되었는지 확인합니다.
     */
    public boolean wasRemoved(
        String sourcePath
    ) {
        String normalizedPath =
            normalizePath(sourcePath);

        if (normalizedPath.isBlank()) {
            return false;
        }

        return removedSourcePaths.contains(
            normalizedPath
        );
    }

    /**
     * 문서별 결과에 포함된 전체 이슈를 반환합니다.
     */
    public List<RagIndexIssue> getIssues() {
        if (indexResults.isEmpty()) {
            return List.of();
        }

        List<RagIndexIssue> issues =
            new ArrayList<>();

        for (RagIndexResult result
            : indexResults) {

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
            : indexResults) {

            if (result != null
                && result.hasIssues()) {

                return true;
            }
        }

        return false;
    }

    private void validateCounts() {
        if (removedDocumentCount
            > missingDocumentCount) {

            throw new IllegalArgumentException(
                "removedDocumentCount must not exceed "
                    + "missingDocumentCount"
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

        if (successfulDocumentCount
                + failedDocumentCount
            > indexResults.size()) {

            throw new IllegalArgumentException(
                "successfulDocumentCount + failedDocumentCount "
                    + "must not exceed indexResults size"
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
        return "RagProjectSyncResult{" +
            "projectRoot='" + projectRoot + '\'' +
            ", model='" + model + '\'' +
            ", discoveredDocumentCount="
                + discoveredDocumentCount +
            ", previouslyIndexedDocumentCount="
                + previouslyIndexedDocumentCount +
            ", missingDocumentCount="
                + missingDocumentCount +
            ", removedDocumentCount="
                + removedDocumentCount +
            ", deletedRecordCount="
                + deletedRecordCount +
            ", indexedChunkCount="
                + indexedChunkCount +
            ", reusedChunkCount="
                + reusedChunkCount +
            ", skippedChunkCount="
                + skippedChunkCount +
            ", failedChunkCount="
                + failedChunkCount +
            ", successfulDocumentCount="
                + successfulDocumentCount +
            ", failedDocumentCount="
                + failedDocumentCount +
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
        private int previouslyIndexedDocumentCount;
        private int missingDocumentCount;
        private int removedDocumentCount;
        private int deletedRecordCount;

        private final List<RagIndexResult> indexResults =
            new ArrayList<>();

        private final List<String> removedSourcePaths =
            new ArrayList<>();

        private int indexedChunkCount;
        private int reusedChunkCount;
        private int skippedChunkCount;
        private int failedChunkCount;

        private int successfulDocumentCount;
        private int failedDocumentCount;

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

        public Builder previouslyIndexedDocumentCount(
            int previouslyIndexedDocumentCount
        ) {
            this.previouslyIndexedDocumentCount =
                previouslyIndexedDocumentCount;

            return this;
        }

        public Builder missingDocumentCount(
            int missingDocumentCount
        ) {
            this.missingDocumentCount =
                missingDocumentCount;

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

        public Builder indexResult(
            RagIndexResult indexResult
        ) {
            if (indexResult != null) {
                indexResults.add(
                    indexResult
                );
            }

            return this;
        }

        public Builder indexResults(
            List<RagIndexResult> indexResults
        ) {
            this.indexResults.clear();

            if (indexResults != null) {
                for (RagIndexResult result
                    : indexResults) {

                    indexResult(result);
                }
            }

            return this;
        }

        public Builder removedSourcePath(
            String sourcePath
        ) {
            String normalized =
                normalizePath(sourcePath);

            if (!normalized.isBlank()) {
                removedSourcePaths.add(
                    normalized
                );
            }

            return this;
        }

        public Builder removedSourcePaths(
            List<String> sourcePaths
        ) {
            this.removedSourcePaths.clear();

            if (sourcePaths != null) {
                for (String sourcePath
                    : sourcePaths) {

                    removedSourcePath(
                        sourcePath
                    );
                }
            }

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
         * 문서별 인덱싱 결과를 기반으로 집계값을 자동 계산합니다.
         */
        public Builder summarizeIndexResults() {
            int successfulDocuments = 0;
            int failedDocuments = 0;

            int indexedChunks = 0;
            int reusedChunks = 0;
            int skippedChunks = 0;
            int failedChunks = 0;

            for (RagIndexResult result
                : indexResults) {

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

        public RagProjectSyncResult build() {
            return new RagProjectSyncResult(
                this
            );
        }
    }
}