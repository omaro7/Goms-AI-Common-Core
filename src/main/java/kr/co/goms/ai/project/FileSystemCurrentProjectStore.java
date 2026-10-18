/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.project;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kr.co.goms.ai.project.workspace.GomsBookWorkspace;
import kr.co.goms.ai.project.workspace.GomsBookWorkspaceService;

public final class FileSystemCurrentProjectStore implements CurrentProjectStore {

    private static final int VERSION = 1;

    private static final String GOMSBOOK_DIRECTORY = ".gomsbook";

    private static final String CURRENT_PROJECT_FILE = "current-project.json";

    private static final String TEMP_SUFFIX = ".tmp";

    private static final String OEBPS_DIRECTORY = "OEBPS";

    private static final String PACKAGE_FILE = "content.opf";

    private static final DateTimeFormatter UPDATED_AT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final GomsBookWorkspaceService workspaceService;

    private final Gson gson;

    private final Object lock = new Object();

    public FileSystemCurrentProjectStore(GomsBookWorkspaceService workspaceService) {
        this.workspaceService = Objects.requireNonNull(workspaceService, "workspaceService must not be null");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public Path getCurrentProjectRoot() {

        synchronized (lock) {

            CurrentProjectState state = readState();

            if (state == null) return null;

            return resolveProjectRoot(state.projectId());
        }
    }

    @Override
    public void setCurrentProjectRoot(Path projectRoot) {

        Objects.requireNonNull(projectRoot, "projectRoot must not be null");

        synchronized (lock) {

            Path projectsRoot = requireProjectsRoot();

            Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();

            validateProjectRoot(projectsRoot, normalizedProjectRoot);

            String projectId = resolveProjectId(normalizedProjectRoot);

            CurrentProjectState state = new CurrentProjectState(VERSION, projectId, LocalDateTime.now().format(UPDATED_AT_FORMATTER));

            writeState(state);
        }
    }

    @Override
    public boolean hasCurrentProject() {
        return getCurrentProjectRoot() != null;
    }

    @Override
    public void clear() {

        synchronized (lock) {

            Path statePath = resolveStatePath();

            try {

                Files.deleteIfExists(statePath);

                Path metadataDirectory = statePath.getParent();

                if (metadataDirectory != null && Files.isDirectory(metadataDirectory) && isDirectoryEmpty(metadataDirectory)) Files.deleteIfExists(metadataDirectory);

            } catch (IOException exception) {

                throw new IllegalStateException("Failed to clear current EPUB project. path=" + statePath, exception);
            }
        }
    }

    private CurrentProjectState readState() {

        Path statePath = resolveStatePath();

        if (!Files.isRegularFile(statePath)) return null;

        try {

            String json = Files.readString(statePath, StandardCharsets.UTF_8);

            if (json == null || json.isBlank()) throw new IllegalStateException("Current project state file is empty: " + statePath);

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            int version = getRequiredInt(root, "version");

            if (version != VERSION) throw new IllegalStateException("Unsupported current project state version. expected=" + VERSION + ", actual=" + version + ", path=" + statePath);

            String projectId = getRequiredString(root, "projectId");

            String updatedAt = getOptionalString(root, "updatedAt");

            return new CurrentProjectState(version, projectId, updatedAt);

        } catch (RuntimeException exception) {

            if (exception instanceof IllegalStateException) throw exception;

            throw new IllegalStateException("Failed to read current EPUB project state. path=" + statePath, exception);

        } catch (IOException exception) {

            throw new IllegalStateException("Failed to read current EPUB project state. path=" + statePath, exception);
        }
    }

    private void writeState(CurrentProjectState state) {

        Path statePath = resolveStatePath();

        Path metadataDirectory = statePath.getParent();

        if (metadataDirectory == null) throw new IllegalStateException("Current project metadata directory is not available: " + statePath);

        Path tempPath = statePath.resolveSibling(CURRENT_PROJECT_FILE + TEMP_SUFFIX);

        try {

            Files.createDirectories(metadataDirectory);

            JsonObject root = new JsonObject();

            root.addProperty("version", state.version());
            root.addProperty("projectId", state.projectId());
            root.addProperty("updatedAt", state.updatedAt());

            String json = gson.toJson(root);

            Files.writeString(tempPath, json + System.lineSeparator(), StandardCharsets.UTF_8);

            moveAtomically(tempPath, statePath);

        } catch (IOException exception) {

            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                // Preserve original write failure.
            }

            throw new IllegalStateException("Failed to write current EPUB project state. projectId=" + state.projectId() + ", path=" + statePath, exception);
        }
    }

    private Path resolveStatePath() {

        String userHome = System.getProperty("user.home");

        if (userHome == null || userHome.isBlank()) throw new IllegalStateException("user.home is not configured.");

        return Path.of(userHome).toAbsolutePath().normalize().resolve(GOMSBOOK_DIRECTORY).resolve(CURRENT_PROJECT_FILE).normalize();
    }

    private Path resolveProjectRoot(String projectId) {

        String normalizedProjectId = requireProjectId(projectId);

        Path projectsRoot = requireProjectsRoot();

        Path projectRoot = projectsRoot.resolve(normalizedProjectId).toAbsolutePath().normalize();

        if (!projectRoot.startsWith(projectsRoot)) throw new IllegalArgumentException("projectId resolves outside projectsRoot: " + normalizedProjectId);

        validateProjectRoot(projectsRoot, projectRoot);

        return projectRoot;
    }

    private void validateProjectRoot(Path projectsRoot, Path projectRoot) {

        if (!projectRoot.startsWith(projectsRoot)) throw new IllegalArgumentException("Current project must be under projectsRoot. projectsRoot=" + projectsRoot + ", projectRoot=" + projectRoot);

        if (!Files.isDirectory(projectRoot)) throw new IllegalStateException("Current EPUB project directory does not exist: " + projectRoot);

        Path packageDocument = projectRoot.resolve(OEBPS_DIRECTORY).resolve(PACKAGE_FILE).normalize();

        if (!Files.isRegularFile(packageDocument)) throw new IllegalStateException("Current EPUB project package document does not exist: " + packageDocument);
    }

    private GomsBookWorkspace requireWorkspace() {

        GomsBookWorkspace workspace = workspaceService.getCurrentWorkspace();

        if (workspace == null) throw new IllegalStateException("GomsBook workspace is not configured.");

        return workspace;
    }

    private Path requireProjectsRoot() {

        Path projectsRoot = normalize(requireWorkspace().getProjectsRoot());

        if (projectsRoot == null) throw new IllegalStateException("GomsBook projectsRoot is not configured.");

        return projectsRoot;
    }

    private String resolveProjectId(Path projectRoot) {

        Path fileName = projectRoot.getFileName();

        if (fileName == null) throw new IllegalArgumentException("Unable to resolve projectId from projectRoot: " + projectRoot);

        return requireProjectId(fileName.toString());
    }

    private String requireProjectId(String projectId) {

        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank.");

        String normalizedProjectId = projectId.trim();

        if (normalizedProjectId.contains("/") || normalizedProjectId.contains("\\")) throw new IllegalArgumentException("projectId must be a project folder name: " + normalizedProjectId);

        return normalizedProjectId;
    }

    private int getRequiredInt(JsonObject root, String name) {

        JsonElement value = root.get(name);

        if (value == null || value.isJsonNull()) throw new IllegalStateException("Current project field is missing: " + name);

        return value.getAsInt();
    }

    private String getRequiredString(JsonObject root, String name) {

        String value = getOptionalString(root, name);

        if (value == null) throw new IllegalStateException("Current project field is missing or blank: " + name);

        return value;
    }

    private String getOptionalString(JsonObject root, String name) {

        JsonElement value = root.get(name);

        if (value == null || value.isJsonNull()) return null;

        String text = value.getAsString();

        return text == null || text.isBlank() ? null : text.trim();
    }

    private void moveAtomically(Path source, Path target) throws IOException {

        try {

            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        } catch (AtomicMoveNotSupportedException exception) {

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {

        try (var stream = Files.list(directory)) {
            return stream.findAny().isEmpty();
        }
    }

    private Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    private record CurrentProjectState(
            int version,
            String projectId,
            String updatedAt) {
    }
}
