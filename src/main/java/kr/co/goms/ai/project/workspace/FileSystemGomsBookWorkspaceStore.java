/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.project.workspace;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Properties;

public final class FileSystemGomsBookWorkspaceStore implements GomsBookWorkspaceStore {

    private static final String KEY_GOMSBOOK_ROOT = "gomsBookRoot";
    private static final String KEY_PROJECTS_ROOT = "projectsRoot";
    private static final String KEY_PUBLISH_ROOT = "publishRoot";

    private final Path configurationFile;

    public FileSystemGomsBookWorkspaceStore(Path configurationFile) {
        this.configurationFile = Objects.requireNonNull(configurationFile, "configurationFile must not be null").toAbsolutePath().normalize();
    }

    @Override
    public synchronized GomsBookWorkspace get() {

        if (!Files.isRegularFile(configurationFile)) return null;

        try (InputStream inputStream = Files.newInputStream(configurationFile)) {

            Properties properties = new Properties();
            properties.load(inputStream);

            Path gomsBookRoot = Path.of(require(properties, KEY_GOMSBOOK_ROOT));
            Path projectsRoot = Path.of(require(properties, KEY_PROJECTS_ROOT));
            Path publishRoot = Path.of(require(properties, KEY_PUBLISH_ROOT));

            return new GomsBookWorkspace(gomsBookRoot, projectsRoot, publishRoot);

        } catch (Exception exception) {

            throw new IllegalStateException("Failed to read GomsBook workspace configuration: " + configurationFile, exception);
        }
    }

    @Override
    public synchronized void save(GomsBookWorkspace workspace) {

        Objects.requireNonNull(workspace, "workspace must not be null");

        try {

            Path parent = configurationFile.getParent();

            if (parent != null) Files.createDirectories(parent);

            Properties properties = new Properties();

            properties.setProperty(KEY_GOMSBOOK_ROOT, workspace.getGomsBookRoot().toString());
            properties.setProperty(KEY_PROJECTS_ROOT, workspace.getProjectsRoot().toString());
            properties.setProperty(KEY_PUBLISH_ROOT, workspace.getPublishRoot().toString());

            try (OutputStream outputStream = Files.newOutputStream(configurationFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                properties.store(outputStream, "GomsBook AI Workspace");
            }

        } catch (Exception exception) {

            throw new IllegalStateException("Failed to save GomsBook workspace configuration: " + configurationFile, exception);
        }
    }

    private String require(Properties properties, String key) {

        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) throw new IllegalStateException("Missing workspace configuration property: " + key);

        return value.trim();
    }
}