/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.index;

/**
 * EPUB 프로젝트 RAG 인덱싱 진행 상태를 수신하는 Listener입니다.
 *
 * <p>
 * Core 계층에서는 SSE 또는 UI 구현을 직접 참조하지 않고,
 * 호출 계층에서 전달한 Listener를 통해 진행 상태를 전달합니다.
 * </p>
 */
@FunctionalInterface
public interface ProjectIndexProgressListener {

    /**
     * RAG 인덱싱 진행 상태가 변경되었을 때 호출됩니다.
     *
     * @param progress 현재 진행 상태
     */
    void onProgress(ProjectIndexProgress progress);

    /**
     * 진행 상태 처리가 필요하지 않은 경우 사용하는 No-Op Listener입니다.
     *
     * @return 아무 작업도 수행하지 않는 Listener
     */
    static ProjectIndexProgressListener noop() {
        return progress -> {};
    }
}