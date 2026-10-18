/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.index;

/**
 * EPUB 프로젝트 RAG 인덱싱 진행 단계를 정의합니다.
 */
public enum ProjectIndexProgressStage {

    /**
     * RAG 인덱싱 시작.
     */
    STARTED,

    /**
     * TEXT 디렉터리 및 XHTML 파일 탐색.
     */
    SCANNING,

    /**
     * 기존 VectorStore에서 삭제되었거나 제외된 문서 제거.
     */
    DELETING,

    /**
     * XHTML 문서 분석 및 Chunk 생성.
     */
    INDEXING,

    /**
     * Chunk Embedding 생성 및 VectorStore 저장.
     */
    EMBEDDING,

    /**
     * RAG 인덱싱 완료.
     */
    COMPLETED,

    /**
     * RAG 인덱싱 실패.
     */
    FAILED
}