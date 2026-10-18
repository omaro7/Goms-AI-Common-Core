/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.retrieval;

/**
 * RAG 검색 질의의 의도를 정의합니다.
 *
 * <p>질의 의도에 따라 Controlled Lexical Rerank의 가중치를
 * 다르게 적용하기 위해 사용합니다.</p>
 */
public enum RagQueryIntent {

    /**
     * 일반 의미 검색 질의입니다.
     */
    DEFAULT,

    /**
     * 장소 또는 위치를 식별하는 질의입니다.
     */
    LOCATION
}