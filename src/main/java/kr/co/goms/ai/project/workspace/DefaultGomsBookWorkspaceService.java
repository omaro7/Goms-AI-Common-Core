/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.project.workspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class DefaultGomsBookWorkspaceService implements GomsBookWorkspaceService {

    private static final String PROJECTS_DIRECTORY = "03.Project";
    private static final String PUBLISH_DIRECTORY = "02.Publish";

    private final GomsBookWorkspaceStore workspaceStore;

    public DefaultGomsBookWorkspaceService(GomsBookWorkspaceStore workspaceStore) {
        this.workspaceStore = Objects.requireNonNull(workspaceStore, "workspaceStore must not be null");
    }

    @Override
    public GomsBookWorkspace configure(Path gomsBookRoot) {

        Path root = normalizeRoot(gomsBookRoot);
        Path projectsRoot = root.resolve(PROJECTS_DIRECTORY).normalize();
        Path publishRoot = root.resolve(PUBLISH_DIRECTORY).normalize();

        try {

            Files.createDirectories(root);
            Files.createDirectories(projectsRoot);
            Files.createDirectories(publishRoot);

            GomsBookWorkspace workspace = new GomsBookWorkspace(root, projectsRoot, publishRoot);

            workspaceStore.save(workspace);

            return workspace;

        } catch (Exception exception) {

            throw new IllegalStateException("Failed to configure GomsBook workspace: " + root, exception);
        }
    }

    @Override
    public GomsBookWorkspace getCurrentWorkspace() {
        return workspaceStore.get();
    }

    private Path normalizeRoot(Path root) {

        Objects.requireNonNull(root, "gomsBookRoot must not be null");

        Path normalized = root.toAbsolutePath().normalize();

        if (!normalized.isAbsolute()) throw new IllegalArgumentException("gomsBookRoot must be an absolute path.");

        return normalized;
    }
}