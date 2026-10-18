/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.document;

import java.nio.file.Path;

import kr.co.goms.ai.rag.model.DocumentSource;

/**
 * 파일 시스템 문서를 RAG 인덱싱용 DocumentSource로 로드합니다.
 */
public interface DocumentLoader {

    /**
     * 단독 파일을 로드합니다.
     *
     * @param path 실제 파일 경로
     * @return 로드된 문서
     * @throws DocumentLoadException 로딩 실패 시
     */
    DocumentSource load(
        Path path
    ) throws DocumentLoadException;

    /**
     * 프로젝트의 문서를 로드합니다.
     *
     * <p>실제 파일은 projectRoot.resolve(relativePath)에서 읽고,
     * DocumentSource에는 relativePath를 논리적인 sourcePath로
     * 보존해야 합니다.</p>
     *
     * @param projectRoot 프로젝트 루트
     * @param relativePath 프로젝트 기준 상대 경로
     * @return 로드된 문서
     * @throws DocumentLoadException 로딩 실패 시
     */
    DocumentSource load(
        Path projectRoot,
        Path relativePath
    ) throws DocumentLoadException;

    /**
     * 지원 가능한 문서 형식인지 확인합니다.
     *
     * @param path 문서 경로
     * @return 지원 여부
     */
    boolean supports(
        Path path
    );
}