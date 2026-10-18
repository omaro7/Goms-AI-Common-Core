/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 텍스트를 임베딩 벡터로 변환하는 클라이언트 인터페이스입니다.
 *
 * <p>구현체는 Ollama, 로컬 임베딩 서버 또는 다른 임베딩 제공자와
 * 통신하여 {@link EmbeddingResponse}를 반환합니다.</p>
 *
 * <pre>
 * EmbeddingRequest request =
 *     EmbeddingRequest.forQuery(
 *         "bge-m3",
 *         "3장의 이미지 대체 텍스트를 찾아줘"
 *     );
 *
 * EmbeddingResponse response =
 *     embeddingClient.embed(request);
 *
 * float[] vector = response.getEmbedding();
 * </pre>
 */
public interface EmbeddingClient {

    /**
     * 임베딩 요청을 실행합니다.
     *
     * <p>단일 입력과 배치 입력을 모두 처리합니다.</p>
     *
     * @param request 임베딩 요청
     * @return 임베딩 응답
     * @throws EmbeddingException 임베딩 요청 또는 응답 처리 실패 시
     */
    EmbeddingResponse embed(
        EmbeddingRequest request
    ) throws EmbeddingException;

    /**
     * 단일 문서 텍스트를 임베딩합니다.
     *
     * @param model 임베딩 모델명
     * @param text 임베딩할 문서 텍스트
     * @return 임베딩 벡터
     * @throws EmbeddingException 임베딩 실패 시
     */
    default float[] embedDocument(
        String model,
        String text
    ) throws EmbeddingException {

        EmbeddingRequest request =
            EmbeddingRequest.forDocument(
                model,
                text
            );

        EmbeddingResponse response = embed(request);

        validateResponse(request, response);

        return response.getEmbedding();
    }

    /**
     * 검색 질의를 임베딩합니다.
     *
     * @param model 임베딩 모델명
     * @param query 검색 질의
     * @return 질의 임베딩 벡터
     * @throws EmbeddingException 임베딩 실패 시
     */
    default float[] embedQuery(
        String model,
        String query
    ) throws EmbeddingException {

        EmbeddingRequest request =
            EmbeddingRequest.forQuery(
                model,
                query
            );

        EmbeddingResponse response = embed(request);

        validateResponse(request, response);

        return response.getEmbedding();
    }

    /**
     * 여러 문서 텍스트를 배치로 임베딩합니다.
     *
     * @param model 임베딩 모델명
     * @param texts 임베딩할 텍스트 목록
     * @return 입력 순서와 동일한 임베딩 벡터 목록
     * @throws EmbeddingException 임베딩 실패 시
     */
    default List<float[]> embedDocuments(
        String model,
        List<String> texts
    ) throws EmbeddingException {

        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        EmbeddingRequest request =
            EmbeddingRequest.builder()
                .model(model)
                .inputs(texts)
                .purpose(EmbeddingPurpose.DOCUMENT)
                .normalize(true)
                .truncate(true)
                .build();

        EmbeddingResponse response = embed(request);

        validateResponse(request, response);

        return response.getEmbeddings();
    }

    /**
     * 여러 검색 질의를 배치로 임베딩합니다.
     *
     * 일반적인 RAG 검색에서는 단일 질의를 사용하지만,
     * 질의 확장 또는 멀티쿼리 검색에서 사용할 수 있습니다.
     *
     * @param model 임베딩 모델명
     * @param queries 검색 질의 목록
     * @return 입력 순서와 동일한 임베딩 벡터 목록
     * @throws EmbeddingException 임베딩 실패 시
     */
    default List<float[]> embedQueries(
        String model,
        List<String> queries
    ) throws EmbeddingException {

        if (queries == null || queries.isEmpty()) {
            return List.of();
        }

        EmbeddingRequest request =
            EmbeddingRequest.builder()
                .model(model)
                .inputs(queries)
                .purpose(EmbeddingPurpose.QUERY)
                .normalize(true)
                .truncate(true)
                .build();

        EmbeddingResponse response = embed(request);

        validateResponse(request, response);

        return response.getEmbeddings();
    }

    /**
     * 입력 목록을 지정된 배치 크기로 나누어 임베딩합니다.
     *
     * 대량의 Chunk를 한 번에 Ollama로 전달하면 메모리 사용량이 커질 수
     * 있으므로, 프로젝트 전체 인덱싱에서는 이 메서드를 사용하는 것이
     * 안전합니다.
     *
     * @param model 임베딩 모델명
     * @param texts 임베딩할 문서 텍스트 목록
     * @param batchSize 한 요청에 포함할 최대 입력 개수
     * @return 전체 임베딩 벡터 목록
     * @throws EmbeddingException 임베딩 실패 시
     */
    default List<float[]> embedDocuments(
        String model,
        List<String> texts,
        int batchSize
    ) throws EmbeddingException {

        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        if (batchSize < 1) {
            throw new IllegalArgumentException(
                "batchSize must be greater than zero"
            );
        }

        List<float[]> embeddings =
            new ArrayList<>(texts.size());

        for (int start = 0;
             start < texts.size();
             start += batchSize) {

            int end = Math.min(
                start + batchSize,
                texts.size()
            );

            List<String> batch =
                texts.subList(start, end);

            embeddings.addAll(
                embedDocuments(model, batch)
            );
        }

        return List.copyOf(embeddings);
    }

    /**
     * 임베딩 서버 또는 모델 사용 가능 여부를 확인합니다.
     *
     * 기본 구현은 별도의 상태 확인 없이 true를 반환합니다.
     * Ollama 구현체에서는 서버 연결과 모델 존재 여부를 검사하도록
     * 재정의할 수 있습니다.
     *
     * @param model 확인할 임베딩 모델명
     * @return 사용 가능 여부
     */
    default boolean isAvailable(String model) {
        return model != null && !model.isBlank();
    }

    /**
     * 현재 클라이언트가 특정 모델을 지원하는지 확인합니다.
     *
     * 기본 구현은 모델명이 비어 있지 않은지만 확인합니다.
     *
     * @param model 모델명
     * @return 지원 여부
     */
    default boolean supports(String model) {
        return model != null && !model.isBlank();
    }

    /**
     * 임베딩 요청과 응답의 기본 정합성을 검증합니다.
     *
     * @param request 원본 요청
     * @param response 임베딩 응답
     * @throws EmbeddingException 응답이 유효하지 않은 경우
     */
    default void validateResponse(
        EmbeddingRequest request,
        EmbeddingResponse response
    ) throws EmbeddingException {

        Objects.requireNonNull(
            request,
            "request must not be null"
        );

        if (response == null) {
            throw new EmbeddingException(
                "Embedding response must not be null",
                request.getModel(),
                request.getRequestId()
            );
        }

        if (!response.hasEmbeddings()) {
            throw new EmbeddingException(
                "Embedding response does not contain embeddings",
                request.getModel(),
                request.getRequestId()
            );
        }

        if (response.size() != request.size()) {
            throw new EmbeddingException(
                "Embedding count mismatch. expected="
                    + request.size()
                    + ", actual="
                    + response.size(),
                request.getModel(),
                request.getRequestId()
            );
        }

        if (response.getDimensions() <= 0) {
            throw new EmbeddingException(
                "Embedding dimensions must be greater than zero",
                request.getModel(),
                request.getRequestId()
            );
        }

        try {
            response.validateValues();

        } catch (IllegalStateException exception) {
            throw new EmbeddingException(
                "Embedding response contains invalid values",
                request.getModel(),
                request.getRequestId(),
                exception
            );
        }
    }
}