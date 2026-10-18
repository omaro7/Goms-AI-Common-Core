/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.project;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents the current EPUB project context.
 *
 * <p>
 * This object contains the filesystem locations
 * required by Agent tools and RAG services without
 * exposing GomsBookEditor or Eclipse UI APIs.
 * </p>
 */
public final class EpubProjectContext {

    private final String projectId;
    
    private final String projectName;

    private final Path projectRoot;

    private final Path textDirectory;

    private final Path navigationFile;

    private final Path packageDocument;


    public EpubProjectContext(String projectId, String projectName, Path projectRoot, Path textDirectory, Path navigationFile, Path packageDocument) {

        this.projectId = requireText(projectId, "projectId");
        
        this.projectName = requireText(projectName, "projectName");

        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot");

        this.textDirectory = Objects.requireNonNull(textDirectory, "textDirectory");

        this.navigationFile = Objects.requireNonNull(navigationFile, "navigationFile");

        this.packageDocument = Objects.requireNonNull(packageDocument, "packageDocument");
    }

    

    public String getProjectId() {
		return projectId;
	}


	public String getProjectName() {

        return projectName;
    }


    public Path getProjectRoot() {

        return projectRoot;
    }


    public Path getTextDirectory() {

        return textDirectory;
    }


    public Path getNavigationFile() {

        return navigationFile;
    }


    public Path getPackageDocument() {

        return packageDocument;
    }

    /**
     * Path targetStylesheet = project.getContentRoot()
        .resolve("Styles")
        .resolve("style1.css");
     * @return
     */
    public Path getContentRoot() {

        return packageDocument.getParent();
    }
    

    /**
     * Returns whether the TEXT directory exists.
     */
    public boolean hasTextDirectory() {

        return java.nio.file.Files.isDirectory(
                textDirectory
        );
    }


    /**
     * Returns whether nav.xhtml exists.
     */
    public boolean hasNavigationFile() {

        return java.nio.file.Files.isRegularFile(
                navigationFile
        );
    }


    /**
     * Returns whether the OPF package document exists.
     */
    public boolean hasPackageDocument() {

        return java.nio.file.Files.isRegularFile(
                packageDocument
        );
    }


    private static String requireText( String value, String name) {

        Objects.requireNonNull(value, name);

        String normalized = value.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    name + " must not be empty."
            );
        }

        return normalized;
    }


    @Override
    public String toString() {

        return "EpubProjectContext{"
                + "projectId='"
                + projectId
                + '\''
                + ", projectName="
                + projectName
                + ", projectRoot="
                + projectRoot
                + ", textDirectory="
                + textDirectory
                + ", navigationFile="
                + navigationFile
                + ", packageDocument="
                + packageDocument
                + '}';
    }
}