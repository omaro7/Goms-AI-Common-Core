/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * EPUB 프로젝트 RAG 인덱스 동기화 결과입니다.
 *
 * <p>
 * 프로젝트의 TEXT 문서를 대상으로 수행한 증분 인덱싱 결과를
 * 표현합니다.
 * </p>
 *
 * <p>
 * 다음 상태를 집계합니다.
 * </p>
 *
 * <ul>
 *     <li>NEW - 새로 발견되어 인덱싱된 문서</li>
 *     <li>CHANGED - 변경되어 다시 인덱싱된 문서</li>
 *     <li>UNCHANGED - 변경되지 않아 건너뛴 문서</li>
 *     <li>DELETED - 삭제되어 VectorStore에서 제거된 문서</li>
 * </ul>
 */
public final class ProjectIndexResult {

    private final String projectId;

    private final String projectName;

    private final String textDirectory;

    private final String embeddingModel;


    private final int discoveredFiles;

    private final int processedFiles;

    private final int newFiles;

    private final int reindexedFiles;

    private final int skippedFiles;

    private final int deletedFiles;


    private final int createdChunks;

    private final int createdEmbeddings;

    private final int storedVectors;

    private final int deletedVectors;


    private final long vectorStoreSize;


    private final List<String> indexedFiles;

    private final List<String> skippedFilePaths;

    private final List<String> deletedFilePaths;


    private ProjectIndexResult(
            Builder builder) {

        this.projectId =
                requireText(
                        builder.projectId,
                        "projectId"
                );

        this.projectName =
                normalizeText(
                        builder.projectName
                );

        this.textDirectory =
                normalizeText(
                        builder.textDirectory
                );

        this.embeddingModel =
                requireText(
                        builder.embeddingModel,
                        "embeddingModel"
                );


        this.discoveredFiles =
                requireNonNegative(
                        builder.discoveredFiles,
                        "discoveredFiles"
                );

        this.processedFiles =
                requireNonNegative(
                        builder.processedFiles,
                        "processedFiles"
                );

        this.newFiles =
                requireNonNegative(
                        builder.newFiles,
                        "newFiles"
                );

        this.reindexedFiles =
                requireNonNegative(
                        builder.reindexedFiles,
                        "reindexedFiles"
                );

        this.skippedFiles =
                requireNonNegative(
                        builder.skippedFiles,
                        "skippedFiles"
                );

        this.deletedFiles =
                requireNonNegative(
                        builder.deletedFiles,
                        "deletedFiles"
                );


        this.createdChunks =
                requireNonNegative(
                        builder.createdChunks,
                        "createdChunks"
                );

        this.createdEmbeddings =
                requireNonNegative(
                        builder.createdEmbeddings,
                        "createdEmbeddings"
                );

        this.storedVectors =
                requireNonNegative(
                        builder.storedVectors,
                        "storedVectors"
                );

        this.deletedVectors =
                requireNonNegative(
                        builder.deletedVectors,
                        "deletedVectors"
                );


        this.vectorStoreSize =
                requireNonNegative(
                        builder.vectorStoreSize,
                        "vectorStoreSize"
                );


        this.indexedFiles =
                immutableCopy(
                        builder.indexedFiles
                );

        this.skippedFilePaths =
                immutableCopy(
                        builder.skippedFilePaths
                );

        this.deletedFilePaths =
                immutableCopy(
                        builder.deletedFilePaths
                );
    }


    public static Builder builder() {

        return new Builder();
    }


    public String getProjectId() {

        return projectId;
    }


    public String getProjectName() {

        return projectName;
    }


    public String getTextDirectory() {

        return textDirectory;
    }


    public String getEmbeddingModel() {

        return embeddingModel;
    }


    public int getDiscoveredFiles() {

        return discoveredFiles;
    }


    public int getProcessedFiles() {

        return processedFiles;
    }


    public int getNewFiles() {

        return newFiles;
    }


    public int getReindexedFiles() {

        return reindexedFiles;
    }


    public int getSkippedFiles() {

        return skippedFiles;
    }


    public int getDeletedFiles() {

        return deletedFiles;
    }


    public int getCreatedChunks() {

        return createdChunks;
    }


    public int getCreatedEmbeddings() {

        return createdEmbeddings;
    }


    public int getStoredVectors() {

        return storedVectors;
    }


    public int getDeletedVectors() {

        return deletedVectors;
    }


    public long getVectorStoreSize() {

        return vectorStoreSize;
    }


    public List<String> getIndexedFiles() {

        return indexedFiles;
    }


    public List<String> getSkippedFilePaths() {

        return skippedFilePaths;
    }


    public List<String> getDeletedFilePaths() {

        return deletedFilePaths;
    }


    /**
     * 실제 인덱스 변경이 발생했는지 확인합니다.
     */
    public boolean isChanged() {

        return newFiles > 0
                || reindexedFiles > 0
                || deletedFiles > 0;
    }


    /**
     * 새로운 Embedding이 생성되었는지 확인합니다.
     */
    public boolean hasNewEmbeddings() {

        return createdEmbeddings > 0;
    }


    /**
     * 모든 발견 문서가 변경되지 않았는지 확인합니다.
     */
    public boolean isUnchanged() {

        return discoveredFiles > 0
                && newFiles == 0
                && reindexedFiles == 0
                && deletedFiles == 0
                && skippedFiles == discoveredFiles;
    }


    @Override
    public String toString() {

        return "ProjectIndexResult{" +
                "projectId='" + projectId + '\'' +
                ", projectName='" + projectName + '\'' +
                ", embeddingModel='" + embeddingModel + '\'' +
                ", discoveredFiles=" + discoveredFiles +
                ", processedFiles=" + processedFiles +
                ", newFiles=" + newFiles +
                ", reindexedFiles=" + reindexedFiles +
                ", skippedFiles=" + skippedFiles +
                ", deletedFiles=" + deletedFiles +
                ", createdChunks=" + createdChunks +
                ", createdEmbeddings=" + createdEmbeddings +
                ", storedVectors=" + storedVectors +
                ", deletedVectors=" + deletedVectors +
                ", vectorStoreSize=" + vectorStoreSize +
                '}';
    }


    private static String requireText(
            String value,
            String name) {

        String normalized =
                normalizeText(
                        value
                );

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    name + " must not be blank"
            );
        }

        return normalized;
    }


    private static String normalizeText(
            String value) {

        return value == null
                ? ""
                : value.trim();
    }


    private static int requireNonNegative(
            int value,
            String name) {

        if (value < 0) {

            throw new IllegalArgumentException(
                    name + " must not be negative"
            );
        }

        return value;
    }


    private static long requireNonNegative(
            long value,
            String name) {

        if (value < 0L) {

            throw new IllegalArgumentException(
                    name + " must not be negative"
            );
        }

        return value;
    }


    private static List<String> immutableCopy(
            List<String> values) {

        if (values == null
                || values.isEmpty()) {

            return Collections.emptyList();
        }


        List<String> result =
                new ArrayList<>();


        for (String value : values) {

            if (value == null
                    || value.isBlank()) {

                continue;
            }

            result.add(
                    value.trim()
            );
        }


        return Collections.unmodifiableList(
                result
        );
    }


    public static final class Builder {

        private String projectId;

        private String projectName;

        private String textDirectory;

        private String embeddingModel;


        private int discoveredFiles;

        private int processedFiles;

        private int newFiles;

        private int reindexedFiles;

        private int skippedFiles;

        private int deletedFiles;


        private int createdChunks;

        private int createdEmbeddings;

        private int storedVectors;

        private int deletedVectors;


        private long vectorStoreSize;


        private List<String> indexedFiles =
                Collections.emptyList();

        private List<String> skippedFilePaths =
                Collections.emptyList();

        private List<String> deletedFilePaths =
                Collections.emptyList();


        private Builder() {
        }


        public Builder projectId(
                String projectId) {

            this.projectId =
                    projectId;

            return this;
        }


        public Builder projectName(
                String projectName) {

            this.projectName =
                    projectName;

            return this;
        }


        public Builder textDirectory(
                String textDirectory) {

            this.textDirectory =
                    textDirectory;

            return this;
        }


        public Builder embeddingModel(
                String embeddingModel) {

            this.embeddingModel =
                    embeddingModel;

            return this;
        }


        public Builder discoveredFiles(
                int discoveredFiles) {

            this.discoveredFiles =
                    discoveredFiles;

            return this;
        }


        public Builder processedFiles(
                int processedFiles) {

            this.processedFiles =
                    processedFiles;

            return this;
        }


        public Builder newFiles(
                int newFiles) {

            this.newFiles =
                    newFiles;

            return this;
        }


        public Builder reindexedFiles(
                int reindexedFiles) {

            this.reindexedFiles =
                    reindexedFiles;

            return this;
        }


        public Builder skippedFiles(
                int skippedFiles) {

            this.skippedFiles =
                    skippedFiles;

            return this;
        }


        public Builder deletedFiles(
                int deletedFiles) {

            this.deletedFiles =
                    deletedFiles;

            return this;
        }


        public Builder createdChunks(
                int createdChunks) {

            this.createdChunks =
                    createdChunks;

            return this;
        }


        public Builder createdEmbeddings(
                int createdEmbeddings) {

            this.createdEmbeddings =
                    createdEmbeddings;

            return this;
        }


        public Builder storedVectors(
                int storedVectors) {

            this.storedVectors =
                    storedVectors;

            return this;
        }


        public Builder deletedVectors(
                int deletedVectors) {

            this.deletedVectors =
                    deletedVectors;

            return this;
        }


        public Builder vectorStoreSize(
                long vectorStoreSize) {

            this.vectorStoreSize =
                    vectorStoreSize;

            return this;
        }


        public Builder indexedFiles(
                List<String> indexedFiles) {

            this.indexedFiles =
                    Objects.requireNonNullElse(
                            indexedFiles,
                            Collections.emptyList()
                    );

            return this;
        }


        public Builder skippedFilePaths(
                List<String> skippedFilePaths) {

            this.skippedFilePaths =
                    Objects.requireNonNullElse(
                            skippedFilePaths,
                            Collections.emptyList()
                    );

            return this;
        }


        public Builder deletedFilePaths(
                List<String> deletedFilePaths) {

            this.deletedFilePaths =
                    Objects.requireNonNullElse(
                            deletedFilePaths,
                            Collections.emptyList()
                    );

            return this;
        }


        public ProjectIndexResult build() {

            return new ProjectIndexResult(
                    this
            );
        }
    }
}