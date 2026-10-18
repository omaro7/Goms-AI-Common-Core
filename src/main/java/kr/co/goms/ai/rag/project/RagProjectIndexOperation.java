/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.project;

/**
 * EPUB 프로젝트 단위 RAG 인덱싱 처리 단계입니다.
 *
 * <p>{@link RagProjectIndexException}에서 예외가 발생한 위치를
 * 식별하는 데 사용합니다.</p>
 */
public enum RagProjectIndexOperation {

    /**
     * 프로젝트 경로, 요청값 및 문서 경로 검증.
     */
    VALIDATE,

    /**
     * 프로젝트에서 인덱싱 대상 문서를 탐색.
     */
    DISCOVER_DOCUMENTS,

    /**
     * 원본 문서를 DocumentSource로 로드.
     */
    LOAD_DOCUMENT,

    /**
     * 문서 하나를 인덱싱.
     */
    INDEX_DOCUMENT,

    /**
     * 프로젝트 전체 문서를 인덱싱.
     */
    INDEX_PROJECT,

    /**
     * 기존 VectorStore의 프로젝트 인덱스 조회.
     */
    READ_INDEX,

    /**
     * 현재 프로젝트 파일과 기존 인덱스를 동기화.
     */
    SYNCHRONIZE,

    /**
     * 특정 문서의 기존 벡터 인덱스 제거.
     */
    REMOVE_DOCUMENT,

    /**
     * 프로젝트에 속한 전체 벡터 인덱스 제거.
     */
    REMOVE_PROJECT,

    /**
     * 프로젝트 인덱스 초기화.
     */
    CLEAR_INDEX,

    /**
     * 프로젝트 인덱서 가용성 확인.
     */
    AVAILABILITY_CHECK,

    /**
     * 정의되지 않은 작업.
     */
    UNKNOWN
}