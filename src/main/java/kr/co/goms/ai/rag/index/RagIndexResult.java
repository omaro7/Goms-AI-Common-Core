/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 문서 하나의 RAG 인덱싱 결과입니다.
 */
public final class RagIndexResult {

    private final String sourcePath;
    private final String model;
    private final int totalChunkCount;
    private final int indexedCount;
    private final int reusedCount;
    private final int skippedCount;
    private final int deletedCount;
    private final int failedCount;
    private final long durationNanos;
    private final boolean success;
    private final List<String> indexedRecordIds;
    private final List<RagIndexIssue> issues;

    private RagIndexResult(Builder builder) {
        this.sourcePath =
            requireText(
                builder.sourcePath,
                "sourcePath"
            );

        this.model =
            normalize(builder.model);

        this.totalChunkCount =
            validateNonNegative(
                builder.totalChunkCount,
                "totalChunkCount"
            );

        this.indexedCount =
            validateNonNegative(
                builder.indexedCount,
                "indexedCount"
            );

        this.reusedCount =
            validateNonNegative(
                builder.reusedCount,
                "reusedCount"
            );

        this.skippedCount =
            validateNonNegative(
                builder.skippedCount,
                "skippedCount"
            );

        this.deletedCount =
            validateNonNegative(
                builder.deletedCount,
                "deletedCount"
            );

        this.failedCount =
            validateNonNegative(
                builder.failedCount,
                "failedCount"
            );

        this.durationNanos =
            validateNonNegative(
                builder.durationNanos,
                "durationNanos"
            );

        this.success = builder.success;

        this.indexedRecordIds =
            immutableTextList(
                builder.indexedRecordIds
            );

        this.issues =
            immutableIssues(builder.issues);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RagIndexResult failed(
        String sourcePath,
        String message
    ) {
        return builder()
            .sourcePath(sourcePath)
            .success(false)
            .failedCount(1)
            .issue(
                RagIndexIssue.error(
                    message
                )
            )
            .build();
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getModel() {
        return model;
    }

    public int getTotalChunkCount() {
        return totalChunkCount;
    }

    public int getIndexedCount() {
        return indexedCount;
    }

    public int getReusedCount() {
        return reusedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public int getFailedCount() {
        return failedCount;
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

    public List<String> getIndexedRecordIds() {
        return indexedRecordIds;
    }

    public List<RagIndexIssue> getIssues() {
        return issues;
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    public boolean hasFailures() {
        return failedCount > 0;
    }

    public int getProcessedCount() {
        return indexedCount
            + reusedCount
            + skippedCount
            + failedCount;
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

    private static List<String> immutableTextList(
        List<String> values
    ) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> copy =
            new ArrayList<>(values.size());

        for (String value : values) {
            String normalized =
                normalize(value);

            if (!normalized.isBlank()) {
                copy.add(normalized);
            }
        }

        return Collections.unmodifiableList(copy);
    }

    private static List<RagIndexIssue> immutableIssues(
        List<RagIndexIssue> issues
    ) {
        if (issues == null || issues.isEmpty()) {
            return Collections.emptyList();
        }

        List<RagIndexIssue> copy =
            new ArrayList<>(issues.size());

        for (RagIndexIssue issue : issues) {
            if (issue != null) {
                copy.add(issue);
            }
        }

        return Collections.unmodifiableList(copy);
    }

    public static final class Builder {

        private String sourcePath;
        private String model;
        private int totalChunkCount;
        private int indexedCount;
        private int reusedCount;
        private int skippedCount;
        private int deletedCount;
        private int failedCount;
        private long durationNanos;
        private boolean success = true;

        private final List<String> indexedRecordIds =
            new ArrayList<>();

        private final List<RagIndexIssue> issues =
            new ArrayList<>();

        private Builder() {
        }

        public Builder sourcePath(
            String sourcePath
        ) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder model(
            String model
        ) {
            this.model = model;
            return this;
        }

        public Builder totalChunkCount(
            int totalChunkCount
        ) {
            this.totalChunkCount =
                totalChunkCount;

            return this;
        }

        public Builder indexedCount(
            int indexedCount
        ) {
            this.indexedCount =
                indexedCount;

            return this;
        }

        public Builder reusedCount(
            int reusedCount
        ) {
            this.reusedCount =
                reusedCount;

            return this;
        }

        public Builder skippedCount(
            int skippedCount
        ) {
            this.skippedCount =
                skippedCount;

            return this;
        }

        public Builder deletedCount(
            int deletedCount
        ) {
            this.deletedCount =
                deletedCount;

            return this;
        }

        public Builder failedCount(
            int failedCount
        ) {
            this.failedCount =
                failedCount;

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

        public Builder indexedRecordId(
            String recordId
        ) {
            String normalized =
                normalize(recordId);

            if (!normalized.isBlank()) {
                indexedRecordIds.add(
                    normalized
                );
            }

            return this;
        }

        public Builder indexedRecordIds(
            List<String> recordIds
        ) {
            indexedRecordIds.clear();

            if (recordIds != null) {
                for (String recordId : recordIds) {
                    indexedRecordId(recordId);
                }
            }

            return this;
        }

        public Builder issue(
            RagIndexIssue issue
        ) {
            if (issue != null) {
                issues.add(issue);
            }

            return this;
        }

        public Builder issues(
            List<RagIndexIssue> issues
        ) {
            this.issues.clear();

            if (issues != null) {
                for (RagIndexIssue issue : issues) {
                    issue(issue);
                }
            }

            return this;
        }

        public RagIndexResult build() {
            return new RagIndexResult(this);
        }
    }
}