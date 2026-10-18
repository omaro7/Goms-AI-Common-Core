/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

import java.util.ArrayList;
import java.util.List;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.model.DocumentSource;
import kr.co.goms.ai.rag.model.DocumentSourceType;

/**
 * 원본 문서를 RAG 검색 단위인 {@link DocumentChunk} 목록으로 변환합니다.
 *
 * 구현체는 문서 유형에 따라 제목, 문단, 목록, 이미지 대체 텍스트,
 * 메타데이터, 목차 항목, CSS 규칙 등을 의미 있는 단위로 분리합니다.
 *
 * <pre>
 * DocumentSource
 *      ↓
 * DocumentIndexer
 *      ↓
 * List&lt;DocumentChunk&gt;
 * </pre>
 */
public interface DocumentIndexer {

    /**
     * 문서 하나를 Chunk 목록으로 변환합니다.
     *
     * @param source 인덱싱할 원본 문서
     * @return 생성된 Chunk 목록
     * @throws DocumentIndexException 문서 분석 또는 Chunk 생성 실패 시
     */
    List<DocumentChunk> index(
        DocumentSource source
    ) throws DocumentIndexException;

    /**
     * 여러 문서를 순차적으로 인덱싱합니다.
     *
     * 기본 구현은 각 문서에 대해 {@link #index(DocumentSource)}를 호출하고
     * 결과를 하나의 목록으로 병합합니다.
     *
     * @param sources 인덱싱할 문서 목록
     * @return 전체 Chunk 목록
     * @throws DocumentIndexException 인덱싱 실패 시
     */
    default List<DocumentChunk> indexAll(
        List<DocumentSource> sources
    ) throws DocumentIndexException {

        if (sources == null || sources.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> chunks = new ArrayList<>();

        for (DocumentSource source : sources) {
            if (source == null) {
                continue;
            }

            chunks.addAll(index(source));
        }

        return List.copyOf(chunks);
    }

    /**
     * 현재 인덱서가 해당 문서를 처리할 수 있는지 확인합니다.
     *
     * @param source 확인할 원본 문서
     * @return 처리 가능 여부
     */
    default boolean supports(DocumentSource source) {
        return source != null && supports(source.getType());
    }

    /**
     * 현재 인덱서가 해당 문서 유형을 처리할 수 있는지 확인합니다.
     *
     * @param type 문서 유형
     * @return 처리 가능 여부
     */
    boolean supports(DocumentSourceType type);

    /**
     * 문서가 인덱싱 가능한 상태인지 검증합니다.
     *
     * 기본 구현은 null 여부, 지원 유형, 본문 존재 여부를 확인합니다.
     *
     * @param source 검증할 원본 문서
     * @throws DocumentIndexException 인덱싱할 수 없는 경우
     */
    default void validate(
        DocumentSource source
    ) throws DocumentIndexException {

        if (source == null) {
            throw new DocumentIndexException(
                "Document source must not be null"
            );
        }

        if (!supports(source)) {
            throw new DocumentIndexException(
                "Unsupported document source type: "
                    + source.getType(),
                source.getRelativePath()
            );
        }

        if (!source.hasContent()) {
            throw new DocumentIndexException(
                "Document source content is empty",
                source.getRelativePath()
            );
        }
    }

    /**
     * 인덱싱 결과에 중복된 Chunk ID가 있는지 확인합니다.
     *
     * @param chunks 검증할 Chunk 목록
     * @throws DocumentIndexException 중복 ID가 존재하는 경우
     */
    default void validateChunkIds(
        List<DocumentChunk> chunks
    ) throws DocumentIndexException {

        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        java.util.Set<String> ids =
            new java.util.HashSet<>();

        for (DocumentChunk chunk : chunks) {
            if (chunk == null) {
                continue;
            }

            if (!ids.add(chunk.getId())) {
                throw new DocumentIndexException(
                    "Duplicate document chunk id: "
                        + chunk.getId(),
                    chunk.getSourcePath()
                );
            }
        }
    }
}