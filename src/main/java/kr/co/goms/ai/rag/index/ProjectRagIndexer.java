/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

import kr.co.goms.ai.project.EpubProjectContext;

/**
 * EPUB 프로젝트의 RAG 인덱스를 동기화하는 서비스입니다.
 *
 * <p>
 * 현재 프로젝트의 TEXT 문서를 확인하여 VectorStore 상태를
 * 실제 파일 상태와 일치시키는 역할을 담당합니다.
 * </p>
 *
 * <p>
 * 증분 인덱싱 시 다음 상태를 처리합니다.
 * </p>
 *
 * <ul>
 *     <li>NEW - 새 문서를 인덱싱합니다.</li>
 *     <li>CHANGED - 변경된 문서를 다시 인덱싱합니다.</li>
 *     <li>UNCHANGED - 변경되지 않은 문서는 건너뜁니다.</li>
 *     <li>DELETED - 삭제된 문서의 Vector를 제거합니다.</li>
 * </ul>
 *
 * <pre>
 * EpubProjectContext
 *        ↓
 * ProjectRagIndexer
 *        ↓
 * NEW / CHANGED / UNCHANGED / DELETED
 *        ↓
 * VectorStore
 *        ↓
 * ProjectIndexResult
 * </pre>
 */
public interface ProjectRagIndexer {

    /**
     * 지정된 EPUB 프로젝트의 RAG 인덱스를
     * 현재 프로젝트 파일 상태와 동기화합니다.
     *
     * <p>
     * 구현체는 프로젝트의 XHTML 문서를 검사하고,
     * 필요한 문서만 Embedding하여 VectorStore를 갱신해야 합니다.
     * </p>
     *
     * @param project 동기화할 EPUB 프로젝트
     * @return 프로젝트 인덱스 동기화 결과
     * @throws ProjectIndexException 인덱싱 또는 동기화 실패 시
     */
    ProjectIndexResult synchronize(
            EpubProjectContext project
    ) throws ProjectIndexException;

    /**
     * 지정된 EPUB 프로젝트의 RAG 인덱스를
     * 인덱싱 옵션과 함께 현재 프로젝트 파일 상태와 동기화합니다.
     *
     * <p>
     * {@link ProjectIndexOptions}를 통해 특정 XHTML 파일을
     * 인덱싱 대상에서 추가로 제외할 수 있습니다.
     * </p>
     *
     * <p>
     * 제외된 파일이 기존 VectorStore에 이미 인덱싱되어 있는 경우,
     * 구현체는 해당 문서를 DELETED 상태로 처리하여
     * 관련 Vector와 Chunk Context를 제거해야 합니다.
     * </p>
     *
     * @param project 동기화할 EPUB 프로젝트
     * @param options 인덱싱 옵션
     * @return 프로젝트 인덱스 동기화 결과
     * @throws ProjectIndexException 인덱싱 또는 동기화 실패 시
     */
    ProjectIndexResult synchronize(
            EpubProjectContext project,
            ProjectIndexOptions options
    ) throws ProjectIndexException;
    

    /**
     * 지정된 EPUB 프로젝트의 RAG 인덱스를
     * 인덱싱 옵션 및 진행 상태 Listener와 함께 동기화합니다.
     *
     * <p>
     * 구현체는 인덱싱 진행 단계가 변경될 때마다
     * {@link ProjectIndexProgressListener#onProgress(ProjectIndexProgress)}를
     * 호출할 수 있습니다.
     * </p>
     *
     * <p>
     * Listener는 Core 계층과 SSE/UI 계층을 분리하기 위한 콜백이며,
     * 구현체는 SSE, React 또는 API 계층에 직접 의존하지 않아야 합니다.
     * </p>
     *
     * <p>
     * {@code progressListener}가 null인 경우 구현체는
     * {@link ProjectIndexProgressListener#noop()}과 동일하게
     * 처리하는 것을 권장합니다.
     * </p>
     *
     * @param project 동기화할 EPUB 프로젝트
     * @param options 인덱싱 옵션
     * @param progressListener 인덱싱 진행 상태 Listener
     * @return 프로젝트 인덱스 동기화 결과
     * @throws ProjectIndexException 인덱싱 또는 동기화 실패 시
     */
    ProjectIndexResult synchronize(
            EpubProjectContext project,
            ProjectIndexOptions options,
            ProjectIndexProgressListener progressListener
    ) throws ProjectIndexException;

    
    /**
     * 현재 인덱서가 사용 가능한 상태인지 확인합니다.
     *
     * <p>
     * EmbeddingClient 또는 VectorStore가 사용할 수 없는 경우
     * false를 반환할 수 있습니다.
     * </p>
     *
     * @return 사용 가능하면 true
     */
    default boolean isAvailable() {

        return true;
    }
}