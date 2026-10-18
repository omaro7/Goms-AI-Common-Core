/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.model;

/**
 * EPUB 프로젝트에서 추출할 수 있는 Chunk 유형입니다.
 */
public enum DocumentChunkType {

    /**
     * 장 또는 절 제목.
     */
    HEADING,

    /**
     * 일반 본문 문단.
     */
    PARAGRAPH,

    /**
     * 목록 항목.
     */
    LIST_ITEM,

    /**
     * 이미지 및 figure 요소.
     */
    IMAGE,

    /**
     * 이미지 대체 텍스트.
     */
    ALT_TEXT,

    /**
     * 표 내용.
     */
    TABLE,

    /**
     * 인용문.
     */
    QUOTE,

    /**
     * EPUB 메타데이터.
     */
    METADATA,

    /**
     * 목차 항목.
     */
    TOC_ENTRY,

    /**
     * CSS 스타일 정보.
     */
    STYLE,

    /**
     * 분류되지 않은 일반 텍스트.
     */
    TEXT
}