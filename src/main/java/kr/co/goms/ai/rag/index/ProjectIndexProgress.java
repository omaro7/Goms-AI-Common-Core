/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.index;

import java.util.Objects;

/**
 * EPUB 프로젝트 RAG 인덱싱 진행 상태입니다.
 *
 * <p>
 * Core 계층에서는 UI 또는 SSE 구현을 직접 알지 않고,
 * 현재 인덱싱 상태만 이 모델을 통해 전달합니다.
 * </p>
 */
public final class ProjectIndexProgress {

    private final String projectId;
    private final ProjectIndexProgressStage stage;
    private final int current;
    private final int total;
    private final int percent;
    private final String sourcePath;
    private final String message;

    private ProjectIndexProgress(Builder builder) {

        this.projectId = builder.projectId;
        this.stage = Objects.requireNonNull(builder.stage, "stage must not be null");
        this.current = Math.max(0, builder.current);
        this.total = Math.max(0, builder.total);
        this.percent = normalizePercent(builder.percent);
        this.sourcePath = builder.sourcePath;
        this.message = builder.message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProjectId() {
        return projectId;
    }

    public ProjectIndexProgressStage getStage() {
        return stage;
    }

    public int getCurrent() {
        return current;
    }

    public int getTotal() {
        return total;
    }

    public int getPercent() {
        return percent;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getMessage() {
        return message;
    }

    public boolean isCompleted() {
        return stage == ProjectIndexProgressStage.COMPLETED;
    }

    public boolean isFailed() {
        return stage == ProjectIndexProgressStage.FAILED;
    }

    private static int normalizePercent(int percent) {

        if (percent < 0) return 0;

        if (percent > 100) return 100;

        return percent;
    }

    @Override
    public String toString() {

        return "ProjectIndexProgress{"
                + "projectId='" + projectId + '\''
                + ", stage=" + stage
                + ", current=" + current
                + ", total=" + total
                + ", percent=" + percent
                + ", sourcePath='" + sourcePath + '\''
                + ", message='" + message + '\''
                + '}';
    }

    public static final class Builder {

        private String projectId;
        private ProjectIndexProgressStage stage;
        private int current;
        private int total;
        private int percent;
        private String sourcePath;
        private String message;

        private Builder() {
        }

        public Builder projectId(String projectId) {

            this.projectId = projectId;

            return this;
        }

        public Builder stage(ProjectIndexProgressStage stage) {

            this.stage = stage;

            return this;
        }

        public Builder current(int current) {

            this.current = current;

            return this;
        }

        public Builder total(int total) {

            this.total = total;

            return this;
        }

        public Builder percent(int percent) {

            this.percent = percent;

            return this;
        }

        public Builder sourcePath(String sourcePath) {

            this.sourcePath = sourcePath;

            return this;
        }

        public Builder message(String message) {

            this.message = message;

            return this;
        }

        public ProjectIndexProgress build() {

            return new ProjectIndexProgress(this);
        }
    }
}