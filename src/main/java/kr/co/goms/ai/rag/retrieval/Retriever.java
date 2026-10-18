/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * 사용자 질의와 관련된 문서 Chunk를 검색하는 RAG 검색기입니다.
 *
 * <p>Retriever 구현체는 일반적으로 다음 작업을 수행합니다.</p>
 *
 * <ol>
 *     <li>검색 질의를 임베딩 벡터로 변환</li>
 *     <li>VectorStore에서 관련 벡터 검색</li>
 *     <li>최소 점수 및 메타데이터 필터 적용</li>
 *     <li>검색 결과를 RetrievalResult로 변환</li>
 * </ol>
 */
public interface Retriever {

    /**
     * 기본 설정으로 관련 문서를 검색합니다.
     *
     * @param query 사용자 검색 질의
     * @return 검색 결과
     * @throws RetrievalException 검색 실패 시
     */
    RetrievalResult retrieve(
        String query
    ) throws RetrievalException;

    /**
     * 검색 옵션을 지정하여 관련 문서를 검색합니다.
     *
     * @param request 검색 요청
     * @return 검색 결과
     * @throws RetrievalException 검색 실패 시
     */
    RetrievalResult retrieve(
        RetrievalRequest request
    ) throws RetrievalException;

    /**
     * 검색된 DocumentChunk 목록만 반환합니다.
     *
     * @param query 사용자 검색 질의
     * @return 관련 문서 Chunk 목록
     * @throws RetrievalException 검색 실패 시
     */
    default List<DocumentChunk> retrieveChunks(
        String query
    ) throws RetrievalException {

        return retrieve(query).getChunks();
    }

    /**
     * 검색 설정을 적용한 DocumentChunk 목록만 반환합니다.
     *
     * @param request 검색 요청
     * @return 관련 문서 Chunk 목록
     * @throws RetrievalException 검색 실패 시
     */
    default List<DocumentChunk> retrieveChunks(
        RetrievalRequest request
    ) throws RetrievalException {

        return retrieve(request).getChunks();
    }

    /**
     * 검색 결과의 VectorSearchResult 목록만 반환합니다.
     *
     * @param query 사용자 검색 질의
     * @return 벡터 검색 결과 목록
     * @throws RetrievalException 검색 실패 시
     */
    default List<VectorSearchResult> retrieveSearchResults(
        String query
    ) throws RetrievalException {

        return retrieve(query).getSearchResults();
    }

    /**
     * 검색 결과가 존재하는지 확인합니다.
     *
     * @param query 사용자 검색 질의
     * @return 관련 결과 존재 여부
     * @throws RetrievalException 검색 실패 시
     */
    default boolean hasRelevantDocuments(
        String query
    ) throws RetrievalException {

        return !retrieve(query).isEmpty();
    }

    /**
     * Retriever와 임베딩 모델, VectorStore가 사용 가능한지 확인합니다.
     *
     * @return 사용 가능 여부
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 검색 질의 기본 검증입니다.
     *
     * @param query 사용자 질의
     * @return 정규화된 질의
     */
    default String validateQuery(String query) {
        String normalized =
            query == null ? "" : query.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                "query must not be blank"
            );
        }

        return normalized;
    }

    /**
     * VectorSearchResult 목록에서 DocumentChunk 목록을 추출합니다.
     */
    default List<DocumentChunk> extractChunks(
        List<VectorSearchResult> searchResults
    ) {
        if (searchResults == null || searchResults.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> chunks =
            new ArrayList<>(searchResults.size());

        for (VectorSearchResult searchResult : searchResults) {
            if (searchResult == null) {
                continue;
            }

            chunks.add(searchResult.getChunk());
        }

        return List.copyOf(chunks);
    }

    /**
     * 검색 결과 목록의 null 여부를 검증합니다.
     */
    default List<VectorSearchResult> requireResults(
        List<VectorSearchResult> results
    ) {
        return List.copyOf(
            Objects.requireNonNull(
                results,
                "results must not be null"
            )
        );
    }
}