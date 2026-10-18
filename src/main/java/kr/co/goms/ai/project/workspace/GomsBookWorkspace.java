/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.project.workspace;

import java.nio.file.Path;
import java.util.Objects;

public final class GomsBookWorkspace {

    private final Path gomsBookRoot;
    private final Path projectsRoot;
    private final Path publishRoot;

    public GomsBookWorkspace(Path gomsBookRoot, Path projectsRoot, Path publishRoot) {
        this.gomsBookRoot = Objects.requireNonNull(gomsBookRoot, "gomsBookRoot must not be null").toAbsolutePath().normalize();
        this.projectsRoot = Objects.requireNonNull(projectsRoot, "projectsRoot must not be null").toAbsolutePath().normalize();
        this.publishRoot = Objects.requireNonNull(publishRoot, "publishRoot must not be null").toAbsolutePath().normalize();
    }

    public Path getGomsBookRoot() {
        return gomsBookRoot;
    }

    public Path getProjectsRoot() {
        return projectsRoot;
    }

    public Path getPublishRoot() {
        return publishRoot;
    }
}