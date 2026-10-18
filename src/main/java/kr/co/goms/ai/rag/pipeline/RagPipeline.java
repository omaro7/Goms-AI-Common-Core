/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.pipeline;

import kr.co.goms.ai.rag.RagResponse;
import kr.co.goms.ai.rag.context.RagContext;
import kr.co.goms.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;

/**
 * RAG 검색부터 LLM 응답 생성까지 통합하여 실행하는 파이프라인입니다.
 *
 * <p>{@code RagService}는 검색 및 증강 프롬프트 생성까지 담당하고,
 * {@code RagPipeline}은 증강 프롬프트를 LLM에 전달하여 최종 응답까지
 * 생성합니다.</p>
 *
 * <pre>
 * 사용자 요청
 *      ↓
 * RagService
 *      ├─ Retriever
 *      ├─ RagContextBuilder
 *      └─ PromptAugmentor
 *      ↓
 * 증강 프롬프트
 *      ↓
 * LlmClient
 *      ↓
 * RagPipelineResponse
 * </pre>
 */
public interface RagPipeline {

    /**
     * 사용자 요청을 검색 질의로도 사용하여 RAG 응답을 생성합니다.
     *
     * @param userPrompt 사용자 요청
     * @return 최종 RAG 파이프라인 응답
     * @throws RagPipelineException 처리 실패 시
     */
    RagPipelineResponse execute(
        String userPrompt
    ) throws RagPipelineException;

    /**
     * 사용자 요청과 별도의 검색 조건을 사용하여 응답을 생성합니다.
     *
     * <p>{@code retrievalRequest.query}는 문서 검색에 사용되고,
     * {@code userPrompt}는 최종 LLM 명령으로 사용됩니다.</p>
     *
     * @param userPrompt 최종 사용자 요청
     * @param retrievalRequest 문서 검색 조건
     * @return 최종 RAG 파이프라인 응답
     * @throws RagPipelineException 처리 실패 시
     */
    RagPipelineResponse execute(
        String userPrompt,
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException;

    /**
     * RetrievalRequest의 질의를 사용자 요청으로도 사용합니다.
     *
     * @param retrievalRequest 검색 요청
     * @return 최종 RAG 파이프라인 응답
     * @throws RagPipelineException 처리 실패 시
     */
    default RagPipelineResponse execute(
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException {

        if (retrievalRequest == null) {
            throw new IllegalArgumentException(
                "retrievalRequest must not be null"
            );
        }

        return execute(
            retrievalRequest.getQuery(),
            retrievalRequest
        );
    }

    /**
     * 기본 설정으로 최종 답변 문자열만 반환합니다.
     *
     * @param userPrompt 사용자 요청
     * @return LLM 최종 답변
     * @throws RagPipelineException 처리 실패 시
     */
    default String chat(
        String userPrompt
    ) throws RagPipelineException {

        return execute(userPrompt)
            .getAnswer();
    }

    /**
     * 검색 조건을 지정하여 최종 답변 문자열만 반환합니다.
     *
     * @param userPrompt 사용자 요청
     * @param retrievalRequest 검색 요청
     * @return LLM 최종 답변
     * @throws RagPipelineException 처리 실패 시
     */
    default String chat(
        String userPrompt,
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException {

        return execute(
            userPrompt,
            retrievalRequest
        ).getAnswer();
    }

    /**
     * RetrievalRequest의 질의를 사용자 요청으로도 사용하여
     * 최종 답변 문자열만 반환합니다.
     *
     * @param retrievalRequest 검색 요청
     * @return LLM 최종 답변
     * @throws RagPipelineException 처리 실패 시
     */
    default String chat(
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException {

        return execute(retrievalRequest)
            .getAnswer();
    }

    /**
     * RAG 검색과 증강 프롬프트 생성까지만 수행합니다.
     *
     * <p>LLM 호출 전에 증강 프롬프트를 확인하거나,
     * Agent가 직접 LLM 요청을 구성할 때 사용할 수 있습니다.</p>
     *
     * @param userPrompt 사용자 요청
     * @return RAG 처리 결과
     * @throws RagPipelineException 처리 실패 시
     */
    RagResponse prepare(
        String userPrompt
    ) throws RagPipelineException;

    /**
     * 검색 조건을 지정하여 증강 프롬프트를 생성합니다.
     *
     * @param userPrompt 사용자 요청
     * @param retrievalRequest 검색 요청
     * @return RAG 처리 결과
     * @throws RagPipelineException 처리 실패 시
     */
    RagResponse prepare(
        String userPrompt,
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException;

    /**
     * 검색만 수행합니다.
     *
     * @param query 검색 질의
     * @return 검색 결과
     * @throws RagPipelineException 검색 실패 시
     */
    RetrievalResult retrieve(
        String query
    ) throws RagPipelineException;

    /**
     * 조건을 지정하여 검색만 수행합니다.
     *
     * @param retrievalRequest 검색 요청
     * @return 검색 결과
     * @throws RagPipelineException 검색 실패 시
     */
    RetrievalResult retrieve(
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException;

    /**
     * 관련 문서를 검색하여 RAG 컨텍스트까지만 생성합니다.
     *
     * @param query 검색 질의
     * @return RAG 컨텍스트
     * @throws RagPipelineException 처리 실패 시
     */
    RagContext buildContext(
        String query
    ) throws RagPipelineException;

    /**
     * 검색 조건을 지정하여 RAG 컨텍스트를 생성합니다.
     *
     * @param retrievalRequest 검색 요청
     * @return RAG 컨텍스트
     * @throws RagPipelineException 처리 실패 시
     */
    RagContext buildContext(
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException;

    /**
     * RAG 및 LLM 구성요소가 모두 사용 가능한지 확인합니다.
     *
     * @return 사용 가능 여부
     */
    boolean isAvailable();

    /**
     * 사용자 요청을 검증하고 정규화합니다.
     *
     * @param userPrompt 사용자 요청
     * @return 정규화된 요청
     */
    default String validateUserPrompt(
        String userPrompt
    ) {
        String normalized =
            userPrompt == null
                ? ""
                : userPrompt
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                "userPrompt must not be blank"
            );
        }

        return normalized;
    }
}