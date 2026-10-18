/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag;

import kr.co.goms.ai.rag.context.RagContext;
import kr.co.goms.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;

/**
 * RAG 검색, 컨텍스트 생성 및 프롬프트 증강을 통합하는 서비스입니다.
 *
 * <p>Agent나 LLM 계층에서는 Retriever, RagContextBuilder,
 * PromptAugmentor를 각각 직접 호출하지 않고 이 서비스를 통해
 * RAG 처리 결과를 얻습니다.</p>
 *
 * <pre>
 * 사용자 요청
 *      ↓
 * RagService
 *      ├─ Retriever
 *      ├─ RagContextBuilder
 *      └─ PromptAugmentor
 *      ↓
 * RagResponse
 * </pre>
 */
public interface RagService {

    /**
     * 기본 검색 설정을 사용하여 사용자 프롬프트를 증강합니다.
     *
     * @param userPrompt 사용자 요청
     * @return RAG 처리 결과
     * @throws RagException RAG 처리 실패 시
     */
    RagResponse augment(
        String userPrompt
    ) throws RagException;

    /**
     * 검색 조건을 지정하여 사용자 프롬프트를 증강합니다.
     *
     * <p>RetrievalRequest의 query가 검색 질의로 사용되며,
     * userPrompt는 최종 LLM 요청으로 사용됩니다.</p>
     *
     * @param userPrompt 최종 사용자 요청
     * @param retrievalRequest 문서 검색 조건
     * @return RAG 처리 결과
     * @throws RagException RAG 처리 실패 시
     */
    RagResponse augment(
        String userPrompt,
        RetrievalRequest retrievalRequest
    ) throws RagException;

    /**
     * 검색 질의와 사용자 프롬프트를 동일하게 사용하여 처리합니다.
     *
     * @param retrievalRequest 검색 요청
     * @return RAG 처리 결과
     * @throws RagException RAG 처리 실패 시
     */
    default RagResponse augment(
        RetrievalRequest retrievalRequest
    ) throws RagException {

        if (retrievalRequest == null) {
            throw new IllegalArgumentException(
                "retrievalRequest must not be null"
            );
        }

        return augment(
            retrievalRequest.getQuery(),
            retrievalRequest
        );
    }

    /**
     * 문서 검색과 컨텍스트 생성까지만 수행합니다.
     *
     * <p>증강 프롬프트가 필요하지 않고 검색된 프로젝트 문서만
     * 필요한 Tool에서 사용할 수 있습니다.</p>
     *
     * @param query 검색 질의
     * @return RAG 컨텍스트
     * @throws RagException 처리 실패 시
     */
    RagContext buildContext(
        String query
    ) throws RagException;

    /**
     * 검색 조건을 적용하여 컨텍스트를 생성합니다.
     *
     * @param retrievalRequest 검색 요청
     * @return RAG 컨텍스트
     * @throws RagException 처리 실패 시
     */
    RagContext buildContext(
        RetrievalRequest retrievalRequest
    ) throws RagException;

    /**
     * 검색만 수행하고 RetrievalResult를 반환합니다.
     *
     * @param query 검색 질의
     * @return 검색 결과
     * @throws RagException 검색 실패 시
     */
    RetrievalResult retrieve(
        String query
    ) throws RagException;

    /**
     * 지정된 조건으로 검색만 수행합니다.
     *
     * @param retrievalRequest 검색 요청
     * @return 검색 결과
     * @throws RagException 검색 실패 시
     */
    RetrievalResult retrieve(
        RetrievalRequest retrievalRequest
    ) throws RagException;

    /**
     * RAG 구성요소가 현재 사용 가능한지 확인합니다.
     *
     * @return 사용 가능 여부
     */
    boolean isAvailable();

    /**
     * 사용자 프롬프트를 검증하고 정규화합니다.
     *
     * @param userPrompt 사용자 요청
     * @return 정규화된 사용자 요청
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