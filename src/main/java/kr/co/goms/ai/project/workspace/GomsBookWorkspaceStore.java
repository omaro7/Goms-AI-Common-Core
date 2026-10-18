/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.project.workspace;

public interface GomsBookWorkspaceStore {

    GomsBookWorkspace get();

    void save(GomsBookWorkspace workspace);
}