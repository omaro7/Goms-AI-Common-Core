/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.index;

import java.util.List;

public final class ProjectIndexOptions {

    private static final ProjectIndexOptions DEFAULT = new ProjectIndexOptions(List.of());

    private final List<String> excludeFiles;

    private ProjectIndexOptions(List<String> excludeFiles) {
        this.excludeFiles = excludeFiles == null ? List.of() : excludeFiles.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().toList();
    }

    public static ProjectIndexOptions defaults() {
        return DEFAULT;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<String> getExcludeFiles() {
        return excludeFiles;
    }

    public boolean hasExcludedFiles() {
        return !excludeFiles.isEmpty();
    }

    public boolean isEmpty() {
        return excludeFiles.isEmpty();
    }

    @Override
    public String toString() {
        return "ProjectIndexOptions{excludeFiles=" + excludeFiles + "}";
    }

    public static final class Builder {

        private List<String> excludeFiles = List.of();

        private Builder() {
        }

        public Builder excludeFiles(List<String> excludeFiles) {
            this.excludeFiles = excludeFiles;
            return this;
        }

        public ProjectIndexOptions build() {
            return new ProjectIndexOptions(excludeFiles);
        }
    }
}