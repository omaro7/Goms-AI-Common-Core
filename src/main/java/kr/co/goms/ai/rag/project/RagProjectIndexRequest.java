/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.project;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import kr.co.goms.ai.rag.index.RagIndexRequest;

/**
 * EPUB 프로젝트 전체 인덱싱 설정입니다.
 */
public final class RagProjectIndexRequest {

    private static final Set<String> DEFAULT_EXTENSIONS =
        Set.of(
            "xhtml",
            "html",
            "htm"
        );

    /**
     * 문서별 인덱싱 설정입니다.
     */
    private final RagIndexRequest indexRequest;

    /**
     * 탐색 대상 디렉터리입니다.
     *
     * 비어 있으면 프로젝트 전체를 탐색합니다.
     */
    private final Set<String> includeDirectories;

    /**
     * 제외할 디렉터리입니다.
     */
    private final Set<String> excludeDirectories;

    /**
     * 지원 파일 확장자입니다.
     */
    private final Set<String> extensions;

    /**
     * 숨김 파일 및 숨김 디렉터리를 포함할지 여부입니다.
     */
    private final boolean includeHidden;

    /**
     * 하위 디렉터리를 재귀적으로 탐색할지 여부입니다.
     */
    private final boolean recursive;

    /**
     * 현재 프로젝트에 없는 기존 문서 인덱스를 삭제할지 여부입니다.
     */
    private final boolean deleteMissingDocuments;

    /**
     * 문서 하나의 실패 후 다음 문서를 계속 처리할지 여부입니다.
     */
    private final boolean continueOnError;

    /**
     * 탐색 결과를 경로순으로 정렬할지 여부입니다.
     */
    private final boolean sortDocuments;

    private RagProjectIndexRequest(
        Builder builder
    ) {
        this.indexRequest =
            builder.indexRequest == null
                ? RagIndexRequest.defaults()
                : builder.indexRequest;

        this.includeDirectories =
            immutablePaths(
                builder.includeDirectories
            );

        this.excludeDirectories =
            immutablePaths(
                builder.excludeDirectories
            );

        this.extensions =
            immutableExtensions(
                builder.extensions
            );

        this.includeHidden =
            builder.includeHidden;

        this.recursive =
            builder.recursive;

        this.deleteMissingDocuments =
            builder.deleteMissingDocuments;

        this.continueOnError =
            builder.continueOnError;

        this.sortDocuments =
            builder.sortDocuments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RagProjectIndexRequest defaults() {
        return builder().build();
    }

    public RagIndexRequest getIndexRequest() {
        return indexRequest;
    }

    public Set<String> getIncludeDirectories() {
        return includeDirectories;
    }

    public Set<String> getExcludeDirectories() {
        return excludeDirectories;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public boolean isIncludeHidden() {
        return includeHidden;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public boolean isDeleteMissingDocuments() {
        return deleteMissingDocuments;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public boolean isSortDocuments() {
        return sortDocuments;
    }

    public boolean hasIncludeDirectories() {
        return !includeDirectories.isEmpty();
    }

    public boolean hasExcludeDirectories() {
        return !excludeDirectories.isEmpty();
    }

    public boolean supportsExtension(
        String extension
    ) {
        if (extension == null
            || extension.isBlank()) {

            return false;
        }

        String normalized =
            extension.startsWith(".")
                ? extension.substring(1)
                : extension;

        return extensions.contains(
            normalized
                .trim()
                .toLowerCase(Locale.ROOT)
        );
    }

    private static Set<String> immutablePaths(
        Set<String> values
    ) {
        if (values == null
            || values.isEmpty()) {

            return Collections.emptySet();
        }

        Set<String> copy =
            new LinkedHashSet<>();

        for (String value : values) {
            String normalized =
                normalizePath(value);

            if (!normalized.isBlank()) {
                copy.add(normalized);
            }
        }

        return Collections.unmodifiableSet(copy);
    }

    private static Set<String> immutableExtensions(
        Set<String> values
    ) {
        Set<String> source =
            values == null || values.isEmpty()
                ? DEFAULT_EXTENSIONS
                : values;

        Set<String> copy =
            new LinkedHashSet<>();

        for (String value : source) {
            if (value == null) {
                continue;
            }

            String normalized =
                value.trim();

            if (normalized.startsWith(".")) {
                normalized =
                    normalized.substring(1);
            }

            normalized =
                normalized.toLowerCase(
                    Locale.ROOT
                );

            if (!normalized.isBlank()) {
                copy.add(normalized);
            }
        }

        if (copy.isEmpty()) {
            throw new IllegalArgumentException(
                "extensions must not be empty"
            );
        }

        return Collections.unmodifiableSet(copy);
    }

    private static String normalizePath(
        String value
    ) {
        return value == null
            ? ""
            : value
                .trim()
                .replace('\\', '/');
    }

    public static final class Builder {

        private RagIndexRequest indexRequest;

        private final Set<String> includeDirectories =
            new LinkedHashSet<>();

        private final Set<String> excludeDirectories =
            new LinkedHashSet<>();

        private final Set<String> extensions =
            new LinkedHashSet<>(
                DEFAULT_EXTENSIONS
            );

        private boolean includeHidden;
        private boolean recursive = true;
        private boolean deleteMissingDocuments = true;
        private boolean continueOnError;
        private boolean sortDocuments = true;

        private Builder() {
        }

        public Builder indexRequest(
            RagIndexRequest indexRequest
        ) {
            this.indexRequest = indexRequest;
            return this;
        }

        public Builder includeDirectory(
            String directory
        ) {
            String normalized =
                normalizePath(directory);

            if (!normalized.isBlank()) {
                includeDirectories.add(
                    normalized
                );
            }

            return this;
        }

        public Builder includeDirectories(
            Set<String> directories
        ) {
            includeDirectories.clear();

            if (directories != null) {
                for (String directory : directories) {
                    includeDirectory(directory);
                }
            }

            return this;
        }

        public Builder excludeDirectory(
            String directory
        ) {
            String normalized =
                normalizePath(directory);

            if (!normalized.isBlank()) {
                excludeDirectories.add(
                    normalized
                );
            }

            return this;
        }

        public Builder excludeDirectories(
            Set<String> directories
        ) {
            excludeDirectories.clear();

            if (directories != null) {
                for (String directory : directories) {
                    excludeDirectory(directory);
                }
            }

            return this;
        }

        public Builder extension(
            String extension
        ) {
            if (extension == null) {
                return this;
            }

            String normalized =
                extension.trim();

            if (normalized.startsWith(".")) {
                normalized =
                    normalized.substring(1);
            }

            normalized =
                normalized.toLowerCase(
                    Locale.ROOT
                );

            if (!normalized.isBlank()) {
                extensions.add(normalized);
            }

            return this;
        }

        public Builder extensions(
            Set<String> extensions
        ) {
            this.extensions.clear();

            if (extensions != null) {
                for (String extension : extensions) {
                    extension(extension);
                }
            }

            return this;
        }

        public Builder includeHidden(
            boolean includeHidden
        ) {
            this.includeHidden = includeHidden;
            return this;
        }

        public Builder recursive(
            boolean recursive
        ) {
            this.recursive = recursive;
            return this;
        }

        public Builder deleteMissingDocuments(
            boolean deleteMissingDocuments
        ) {
            this.deleteMissingDocuments =
                deleteMissingDocuments;

            return this;
        }

        public Builder continueOnError(
            boolean continueOnError
        ) {
            this.continueOnError =
                continueOnError;

            return this;
        }

        public Builder sortDocuments(
            boolean sortDocuments
        ) {
            this.sortDocuments =
                sortDocuments;

            return this;
        }

        public RagProjectIndexRequest build() {
            return new RagProjectIndexRequest(this);
        }
    }
}