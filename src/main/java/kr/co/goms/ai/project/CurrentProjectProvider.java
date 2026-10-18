/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.project;

/**
 * 현재 프로젝트 조회 인터페이스 입니다.
 * Provides access to the EPUB project
 * currently opened in GomsBookEditor.
 *
 * <p>
 * Agent tools should not directly depend on
 * Eclipse workspace or UI APIs.
 * Instead, they should access the current
 * project through this provider.
 * </p>
 */
public interface CurrentProjectProvider {

    /**
     * Returns the current EPUB project context.
     *
     * @return current EPUB project context
     *
     * @throws IllegalStateException
     *         if no EPUB project is currently open
     */
    EpubProjectContext getCurrentProject();

    /**
     * Returns whether a current EPUB project
     * is available.
     *
     * @return {@code true} if a project is available
     */
    default boolean hasCurrentProject() {

        try {

            return getCurrentProject() != null;

        } catch (IllegalStateException e) {

            return false;
        }
    }
}