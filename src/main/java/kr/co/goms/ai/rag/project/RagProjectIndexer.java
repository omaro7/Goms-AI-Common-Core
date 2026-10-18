/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.rag.index.RagIndexRequest;
import kr.co.goms.ai.rag.index.RagIndexResult;

/**
 * EPUB 프로젝트 전체의 RAG 인덱스를 생성하고 동기화하는 서비스입니다.
 *
 * <p>구현체는 일반적으로 다음 작업을 수행합니다.</p>
 *
 * <ol>
 *     <li>프로젝트 루트 및 EPUB 구조 검증</li>
 *     <li>XHTML, HTML 등 지원 문서 탐색</li>
 *     <li>각 문서를 DocumentSource로 로드</li>
 *     <li>RagIndexer를 이용한 문서별 인덱싱</li>
 *     <li>삭제되거나 제외된 문서의 기존 인덱스 정리</li>
 *     <li>프로젝트 전체 인덱싱 결과 집계</li>
 * </ol>
 *
 * <pre>
 * EPUB 프로젝트
 *      ↓
 * RagProjectIndexer
 *      ├─ 문서 탐색
 *      ├─ 문서별 인덱싱
 *      ├─ 삭제 문서 동기화
 *      └─ 결과 집계
 *      ↓
 * RagProjectIndexResult
 * </pre>
 */
public interface RagProjectIndexer {

    /**
     * 기본 설정으로 EPUB 프로젝트 전체를 인덱싱합니다.
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @return 프로젝트 인덱싱 결과
     * @throws RagProjectIndexException 인덱싱 실패 시
     */
    RagProjectIndexResult indexProject(
    	String projectId,
        Path projectRoot
    ) throws RagProjectIndexException;

    /**
     * 지정된 설정으로 EPUB 프로젝트 전체를 인덱싱합니다.
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @param request 프로젝트 인덱싱 요청
     * @return 프로젝트 인덱싱 결과
     * @throws RagProjectIndexException 인덱싱 실패 시
     */
    RagProjectIndexResult indexProject(
    	String projectId,
        Path projectRoot,
        RagProjectIndexRequest request
    ) throws RagProjectIndexException;

    /**
     * 지정된 문서 경로만 인덱싱합니다.
     *
     * <p>경로는 프로젝트 루트 기준 상대 경로여야 합니다.</p>
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @param relativePaths 인덱싱할 상대 경로 목록
     * @param request 문서 인덱싱 설정
     * @return 문서별 인덱싱 결과
     * @throws RagProjectIndexException 인덱싱 실패 시
     */
    List<RagIndexResult> indexDocuments(
    	String projectId,
        Path projectRoot,
        List<Path> relativePaths,
        RagIndexRequest request
    ) throws RagProjectIndexException;

    /**
     * 지정된 문서 경로만 기본 설정으로 인덱싱합니다.
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @param relativePaths 인덱싱할 상대 경로 목록
     * @return 문서별 인덱싱 결과
     * @throws RagProjectIndexException 인덱싱 실패 시
     */
    default List<RagIndexResult> indexDocuments(
    	String projectId,
        Path projectRoot,
        List<Path> relativePaths
    ) throws RagProjectIndexException {

        return indexDocuments(
        	projectId,
            projectRoot,
            relativePaths,
            getDefaultIndexRequest()
        );
    }

    /**
     * 문서 하나를 인덱싱합니다.
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @param relativePath 인덱싱할 상대 경로
     * @param request 문서 인덱싱 설정
     * @return 문서 인덱싱 결과
     * @throws RagProjectIndexException 인덱싱 실패 시
     */
    default RagIndexResult indexDocument(
    	String projectId,
        Path projectRoot,
        Path relativePath,
        RagIndexRequest request
    ) throws RagProjectIndexException {

        Objects.requireNonNull(
            relativePath,
            "relativePath must not be null"
        );

        List<RagIndexResult> results =
            indexDocuments(
            	projectId,
                projectRoot,
                List.of(relativePath),
                request
            );

        if (results.isEmpty()) {
            throw new RagProjectIndexException(
                "No index result was created for document: "
                    + normalizePath(relativePath),
                RagProjectIndexOperation.INDEX_DOCUMENT,
                normalizePath(projectRoot),
                normalizePath(relativePath),
                null
            );
        }

        return results.get(0);
    }

    /**
     * 문서 하나를 기본 설정으로 인덱싱합니다.
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @param relativePath 인덱싱할 상대 경로
     * @return 문서 인덱싱 결과
     * @throws RagProjectIndexException 인덱싱 실패 시
     */
    default RagIndexResult indexDocument(
    	String projectId,
        Path projectRoot,
        Path relativePath
    ) throws RagProjectIndexException {

        return indexDocument(
        	projectId,
            projectRoot,
            relativePath,
            getDefaultIndexRequest()
        );
    }

    /**
     * 프로젝트 내에서 인덱싱 가능한 문서를 탐색합니다.
     *
     * <p>반환 경로는 프로젝트 루트 기준 상대 경로입니다.</p>
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @return 지원 문서 상대 경로 목록
     * @throws RagProjectIndexException 문서 탐색 실패 시
     */
    List<Path> discoverDocuments(
        Path projectRoot
    ) throws RagProjectIndexException;

    /**
     * 프로젝트 인덱스를 현재 파일 상태와 동기화합니다.
     *
     * <p>현재 프로젝트에 존재하지 않는 문서의 기존 벡터를 삭제하고,
     * 새 문서와 변경된 문서는 다시 인덱싱합니다.</p>
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @return 동기화 결과
     * @throws RagProjectIndexException 동기화 실패 시
     */
    RagProjectSyncResult synchronize(
    	String projectId,
        Path projectRoot
    ) throws RagProjectIndexException;

    /**
     * 지정된 설정으로 프로젝트 인덱스를 동기화합니다.
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @param request 프로젝트 인덱싱 요청
     * @return 동기화 결과
     * @throws RagProjectIndexException 동기화 실패 시
     */
    RagProjectSyncResult synchronize(
    	String projectId,    		
        Path projectRoot,
        RagProjectIndexRequest request
    ) throws RagProjectIndexException;

    /**
     * 특정 문서의 현재 임베딩 모델 인덱스를 제거합니다.
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @param relativePath 프로젝트 기준 문서 경로
     * @return 삭제된 벡터 레코드 수
     * @throws RagProjectIndexException 삭제 실패 시
     */
    int removeDocument(
        Path projectRoot,
        Path relativePath
    ) throws RagProjectIndexException;

    /**
     * 프로젝트의 현재 임베딩 모델 인덱스를 모두 제거합니다.
     *
     * <p>VectorStore가 프로젝트별로 분리되어 있지 않은 경우,
     * 구현체는 프로젝트 경로 또는 메타데이터를 기준으로 삭제해야 합니다.</p>
     *
     * @param projectRoot EPUB 프로젝트 루트
     * @return 삭제된 벡터 레코드 수
     * @throws RagProjectIndexException 삭제 실패 시
     */
    int removeProject(
        Path projectRoot
    ) throws RagProjectIndexException;

    /**
     * 프로젝트가 인덱싱 가능한 EPUB 구조인지 확인합니다.
     *
     * @param projectRoot 프로젝트 루트
     * @return 지원 여부
     */
    boolean supportsProject(
        Path projectRoot
    );

    /**
     * 파일이 현재 프로젝트 인덱서의 지원 대상인지 확인합니다.
     *
     * @param relativePath 프로젝트 기준 상대 경로
     * @return 지원 여부
     */
    boolean supportsDocument(
        Path relativePath
    );

    /**
     * 프로젝트 인덱서와 내부 구성요소가 사용 가능한지 확인합니다.
     *
     * @return 사용 가능 여부
     */
    boolean isAvailable();

    /**
     * 기본 문서 인덱싱 설정을 반환합니다.
     *
     * @return 기본 인덱싱 요청
     */
    RagIndexRequest getDefaultIndexRequest();

    /**
     * 프로젝트 경로를 검증하고 정규화합니다.
     *
     * @param projectRoot 프로젝트 루트
     * @return 절대 정규화 경로
     */
    default Path validateProjectRoot(
        Path projectRoot
    ) {
        Objects.requireNonNull(
            projectRoot,
            "projectRoot must not be null"
        );

        return projectRoot
            .toAbsolutePath()
            .normalize();
    }

    /**
     * 상대 문서 경로 목록을 정규화합니다.
     *
     * <p>절대 경로와 프로젝트 외부 경로는 허용하지 않습니다.</p>
     *
     * @param projectRoot 프로젝트 루트
     * @param relativePaths 상대 경로 목록
     * @return 검증된 상대 경로 목록
     */
    default List<Path> validateRelativePaths(
        Path projectRoot,
        List<Path> relativePaths
    ) {
        Path normalizedRoot =
            validateProjectRoot(projectRoot);

        if (relativePaths == null
            || relativePaths.isEmpty()) {

            return List.of();
        }

        List<Path> validated =
            new ArrayList<>(relativePaths.size());

        for (Path relativePath : relativePaths) {
            if (relativePath == null) {
                continue;
            }

            Path normalizedRelativePath =
                relativePath.normalize();

            if (normalizedRelativePath.isAbsolute()) {
                throw new IllegalArgumentException(
                    "Document path must be relative: "
                        + normalizedRelativePath
                );
            }

            Path resolved =
                normalizedRoot
                    .resolve(normalizedRelativePath)
                    .normalize();

            if (!resolved.startsWith(normalizedRoot)) {
                throw new IllegalArgumentException(
                    "Document path escapes project root: "
                        + normalizedRelativePath
                );
            }

            validated.add(
                normalizedRoot
                    .relativize(resolved)
            );
        }

        return List.copyOf(validated);
    }

    private static String normalizePath(
        Path path
    ) {
        if (path == null) {
            return "";
        }

        return path
            .normalize()
            .toString()
            .replace('\\', '/');
    }
}