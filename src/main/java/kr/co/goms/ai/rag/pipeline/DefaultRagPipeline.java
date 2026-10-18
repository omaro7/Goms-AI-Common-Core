/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.pipeline;

import java.util.Objects;

import kr.co.goms.ai.llm.LlmClient;
import kr.co.goms.ai.llm.LlmException;
import kr.co.goms.ai.llm.LlmMessage;
import kr.co.goms.ai.llm.LlmRequest;
import kr.co.goms.ai.llm.LlmResponse;
import kr.co.goms.ai.llm.LlmRole;
import kr.co.goms.ai.llm.model.ChatModelProvider;
import kr.co.goms.ai.rag.RagException;
import kr.co.goms.ai.rag.RagResponse;
import kr.co.goms.ai.rag.RagService;
import kr.co.goms.ai.rag.context.RagContext;
import kr.co.goms.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;

/**
 * RAG 검색, 프롬프트 증강 및 LLM 호출을 연결하는
 * 기본 {@link RagPipeline} 구현체입니다.
 *
 * <pre>
 * 사용자 요청
 *      ↓
 * RagService
 *      ↓
 * 증강 프롬프트
 *      ↓
 * LlmClient
 *      ↓
 * RagPipelineResponse
 * </pre>
 */
public final class DefaultRagPipeline
    implements RagPipeline {

    /**
     * RAG 검색과 프롬프트 증강을 담당합니다.
     */
    private final RagService ragService;

    /**
     * 증강 프롬프트를 실행할 LLM 클라이언트입니다.
     */
    private final LlmClient llmClient;

    /**
     * Agent/Chat 모델명을 제공합니다.
     */
    private final ChatModelProvider chatModelProvider;

    /**
     * LLM System 메시지입니다.
     *
     * PromptAugmentor에도 역할 지침이 포함될 수 있으므로
     * 기본값은 간결하게 유지합니다.
     */
    private final String systemPrompt;

    /**
     * LLM 출력 토큰 제한입니다.
     *
     * 0이면 LlmClient 또는 모델 기본값을 사용합니다.
     */
    private final int maximumOutputTokens;

    /**
     * 기본 RAG 파이프라인을 생성합니다.
     */
    public DefaultRagPipeline(
        RagService ragService,
        LlmClient llmClient,
        ChatModelProvider chatModelProvider
    ) {
        this(
            ragService,
            llmClient,
            chatModelProvider,
            """
            당신은 GomsBook Editor에 통합된 AI Agent입니다.
            제공된 참고 문서와 사용자 요청을 바탕으로 정확하게 답하십시오.
            프로젝트 문서에 없는 내용을 사실처럼 생성하지 마십시오.
            """,
            0
        );
    }

    /**
     * 상세 설정을 지정하여 RAG 파이프라인을 생성합니다.
     *
     * @param ragService RAG 서비스
     * @param llmClient LLM 클라이언트
     * @param chatModelProvider Chat 모델 제공자
     * @param systemPrompt LLM System 메시지
     * @param maximumOutputTokens 최대 출력 토큰 수. 0이면 기본값
     */
    public DefaultRagPipeline(
        RagService ragService,
        LlmClient llmClient,
        ChatModelProvider chatModelProvider,
        String systemPrompt,
        int maximumOutputTokens
    ) {
        this.ragService = Objects.requireNonNull(
            ragService,
            "ragService must not be null"
        );

        this.llmClient = Objects.requireNonNull(
            llmClient,
            "llmClient must not be null"
        );

        this.chatModelProvider = Objects.requireNonNull(
            chatModelProvider,
            "chatModelProvider must not be null"
        );

        this.systemPrompt = normalizeMultiline(
            systemPrompt
        );

        if (maximumOutputTokens < 0) {
            throw new IllegalArgumentException(
                "maximumOutputTokens must be greater than or equal to zero"
            );
        }

        this.maximumOutputTokens =
            maximumOutputTokens;
    }

    /**
     * 사용자 요청을 검색 질의로도 사용하여 실행합니다.
     */
    @Override
    public RagPipelineResponse execute(
        String userPrompt
    ) throws RagPipelineException {

        String normalizedPrompt =
            validatePrompt(userPrompt);

        RagResponse ragResponse =
            prepare(normalizedPrompt);

        return executeLlm(
            normalizedPrompt,
            ragResponse
        );
    }

    /**
     * 별도의 검색 요청을 적용하여 실행합니다.
     */
    @Override
    public RagPipelineResponse execute(
        String userPrompt,
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException {

        String normalizedPrompt =
            validatePrompt(userPrompt);

        validateRetrievalRequest(
            retrievalRequest
        );

        RagResponse ragResponse =
            prepare(
                normalizedPrompt,
                retrievalRequest
            );

        return executeLlm(
            normalizedPrompt,
            ragResponse
        );
    }

    /**
     * RAG 처리 결과를 LLM에 전달하고 최종 응답을 생성합니다.
     */
    private RagPipelineResponse executeLlm(
        String userPrompt,
        RagResponse ragResponse
    ) throws RagPipelineException {

        Objects.requireNonNull(
            ragResponse,
            "ragResponse must not be null"
        );

        long pipelineStartedAt =
            System.nanoTime();

        String chatModel =
            resolveChatModel(
                ragResponse.getRetrievalQuery()
            );

        LlmRequest llmRequest =
            createLlmRequest(
                ragResponse,
                chatModel
            );

        long llmStartedAt =
            System.nanoTime();

        LlmResponse llmResponse =
            callLlm(
                llmRequest,
                ragResponse.getRetrievalQuery(),
                chatModel
            );

        long llmDurationNanos =
            System.nanoTime() - llmStartedAt;

        String answer =
            extractAnswer(
                llmResponse,
                ragResponse.getRetrievalQuery(),
                chatModel
            );

        try {
            return RagPipelineResponse.builder()
                .userPrompt(userPrompt)
                .retrievalQuery(
                    ragResponse.getRetrievalQuery()
                )
                .augmentedPrompt(
                    ragResponse.getAugmentedPrompt()
                )
                .answer(answer)
                .ragResponse(ragResponse)
                .llmResponse(llmResponse)
                .chatModel(chatModel)
                .durationNanos(
                    System.nanoTime()
                        - pipelineStartedAt
                        + ragResponse.getDurationNanos()
                )
                .llmDurationNanos(
                    llmDurationNanos
                )
                .createdAt(
                    System.currentTimeMillis()
                )
                .build();

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Failed to build RAG pipeline response",
                RagPipelineOperation.BUILD_RESPONSE,
                ragResponse.getRetrievalQuery(),
                chatModel,
                exception
            );
        }
    }

    /**
     * 기본 검색 설정으로 증강 프롬프트를 생성합니다.
     */
    @Override
    public RagResponse prepare(
        String userPrompt
    ) throws RagPipelineException {

        String normalizedPrompt =
            validatePrompt(userPrompt);

        try {
            RagResponse response =
                ragService.augment(
                    normalizedPrompt
                );

            return requireRagResponse(
                response,
                normalizedPrompt
            );

        } catch (RagException exception) {
            throw new RagPipelineException(
                "Failed to prepare RAG prompt",
                RagPipelineOperation.PREPARE_PROMPT,
                normalizedPrompt,
                "",
                exception
            );

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Unexpected error while preparing RAG prompt",
                RagPipelineOperation.PREPARE_PROMPT,
                normalizedPrompt,
                "",
                exception
            );
        }
    }

    /**
     * 별도의 검색 설정으로 증강 프롬프트를 생성합니다.
     */
    @Override
    public RagResponse prepare(
        String userPrompt,
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException {

        String normalizedPrompt =
            validatePrompt(userPrompt);

        validateRetrievalRequest(
            retrievalRequest
        );

        try {
            RagResponse response =
                ragService.augment(
                    normalizedPrompt,
                    retrievalRequest
                );

            return requireRagResponse(
                response,
                retrievalRequest.getQuery()
            );

        } catch (RagException exception) {
            throw new RagPipelineException(
                "Failed to prepare RAG prompt",
                RagPipelineOperation.PREPARE_PROMPT,
                retrievalRequest.getQuery(),
                "",
                exception
            );

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Unexpected error while preparing RAG prompt",
                RagPipelineOperation.PREPARE_PROMPT,
                retrievalRequest.getQuery(),
                "",
                exception
            );
        }
    }

    /**
     * 기본 조건으로 관련 문서를 검색합니다.
     */
    @Override
    public RetrievalResult retrieve(
        String query
    ) throws RagPipelineException {

        String normalizedQuery =
            validateQuery(query);

        try {
            RetrievalResult result =
                ragService.retrieve(
                    normalizedQuery
                );

            return requireRetrievalResult(
                result,
                normalizedQuery
            );

        } catch (RagException exception) {
            throw new RagPipelineException(
                "Failed to retrieve related documents",
                RagPipelineOperation.RETRIEVE,
                normalizedQuery,
                "",
                exception
            );

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Unexpected error while retrieving documents",
                RagPipelineOperation.RETRIEVE,
                normalizedQuery,
                "",
                exception
            );
        }
    }

    /**
     * 검색 조건을 지정하여 관련 문서를 검색합니다.
     */
    @Override
    public RetrievalResult retrieve(
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException {

        validateRetrievalRequest(
            retrievalRequest
        );

        try {
            RetrievalResult result =
                ragService.retrieve(
                    retrievalRequest
                );

            return requireRetrievalResult(
                result,
                retrievalRequest.getQuery()
            );

        } catch (RagException exception) {
            throw new RagPipelineException(
                "Failed to retrieve related documents",
                RagPipelineOperation.RETRIEVE,
                retrievalRequest.getQuery(),
                "",
                exception
            );

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Unexpected error while retrieving documents",
                RagPipelineOperation.RETRIEVE,
                retrievalRequest.getQuery(),
                "",
                exception
            );
        }
    }

    /**
     * 기본 검색 조건으로 RAG 컨텍스트를 생성합니다.
     */
    @Override
    public RagContext buildContext(
        String query
    ) throws RagPipelineException {

        String normalizedQuery =
            validateQuery(query);

        try {
            RagContext context =
                ragService.buildContext(
                    normalizedQuery
                );

            return requireContext(
                context,
                normalizedQuery
            );

        } catch (RagException exception) {
            throw new RagPipelineException(
                "Failed to build RAG context",
                RagPipelineOperation.BUILD_CONTEXT,
                normalizedQuery,
                "",
                exception
            );

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Unexpected error while building RAG context",
                RagPipelineOperation.BUILD_CONTEXT,
                normalizedQuery,
                "",
                exception
            );
        }
    }

    /**
     * 검색 조건을 지정하여 RAG 컨텍스트를 생성합니다.
     */
    @Override
    public RagContext buildContext(
        RetrievalRequest retrievalRequest
    ) throws RagPipelineException {

        validateRetrievalRequest(
            retrievalRequest
        );

        try {
            RagContext context =
                ragService.buildContext(
                    retrievalRequest
                );

            return requireContext(
                context,
                retrievalRequest.getQuery()
            );

        } catch (RagException exception) {
            throw new RagPipelineException(
                "Failed to build RAG context",
                RagPipelineOperation.BUILD_CONTEXT,
                retrievalRequest.getQuery(),
                "",
                exception
            );

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Unexpected error while building RAG context",
                RagPipelineOperation.BUILD_CONTEXT,
                retrievalRequest.getQuery(),
                "",
                exception
            );
        }
    }

    /**
     * 증강된 프롬프트로 LLM 요청을 생성합니다.
     */
    private LlmRequest createLlmRequest(
        RagResponse ragResponse,
        String chatModel
    ) throws RagPipelineException {

        String augmentedPrompt =
            normalizeMultiline(
                ragResponse.getAugmentedPrompt()
            );

        if (augmentedPrompt.isBlank()) {
            throw new RagPipelineException(
                "Augmented prompt must not be blank",
                RagPipelineOperation.CREATE_LLM_REQUEST,
                ragResponse.getRetrievalQuery(),
                chatModel,
                null
            );
        }

        try {
            LlmRequest.Builder builder =
                LlmRequest.builder()
                    .model(chatModel);

            if (!systemPrompt.isBlank()) {
                builder.message(
                    createMessage(
                        LlmRole.SYSTEM,
                        systemPrompt
                    )
                );
            }

            builder.message(
                createMessage(
                    LlmRole.USER,
                    augmentedPrompt
                )
            );

            if (maximumOutputTokens > 0) {
                builder.maxTokens(
                    maximumOutputTokens
                );
            }

            return builder.build();

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Failed to create LLM request",
                RagPipelineOperation.CREATE_LLM_REQUEST,
                ragResponse.getRetrievalQuery(),
                chatModel,
                exception
            );
        }
    }

    /**
     * 공통 LLM 메시지를 생성합니다.
     *
     * 현재 프로젝트의 LlmMessage 구현 방식에 따라 이 메서드만
     * 조정하면 됩니다.
     */
    private LlmMessage createMessage(
        LlmRole role,
        String content
    ) {
    	if (role == LlmRole.SYSTEM) {
    	    return LlmMessage.system(content);
    	}

    	return LlmMessage.user(content);
    }

    /**
     * LLM 요청을 실행합니다.
     */
    private LlmResponse callLlm(
        LlmRequest request,
        String query,
        String chatModel
    ) throws RagPipelineException {

        try {
            LlmResponse response =
                llmClient.chat(request);

            if (response == null) {
                throw new RagPipelineException(
                    "LlmClient returned null response",
                    RagPipelineOperation.CALL_LLM,
                    query,
                    chatModel,
                    null
                );
            }

            return response;

        } catch (LlmException exception) {
            throw new RagPipelineException(
                "Failed to generate RAG answer",
                RagPipelineOperation.CALL_LLM,
                query,
                chatModel,
                exception
            );

        } catch (RagPipelineException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Unexpected error while calling LLM",
                RagPipelineOperation.CALL_LLM,
                query,
                chatModel,
                exception
            );
        }
    }

    /**
     * LLM 응답에서 최종 답변을 추출합니다.
     */
    private String extractAnswer(
        LlmResponse llmResponse,
        String query,
        String chatModel
    ) throws RagPipelineException {

        try {
            String answer =
                normalizeMultiline(
                    llmResponse.getContent()
                );

            if (answer.isBlank()) {
                throw new RagPipelineException(
                    "LLM response content must not be blank",
                    RagPipelineOperation.VALIDATE_LLM_RESPONSE,
                    query,
                    chatModel,
                    null
                );
            }

            return answer;

        } catch (RagPipelineException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Failed to read LLM response content",
                RagPipelineOperation.VALIDATE_LLM_RESPONSE,
                query,
                chatModel,
                exception
            );
        }
    }

    /**
     * 현재 Chat 모델명을 확정합니다.
     */
    private String resolveChatModel(
        String query
    ) throws RagPipelineException {

        String model;

        try {
            model =
                chatModelProvider.getModel();

        } catch (RuntimeException exception) {
            throw new RagPipelineException(
                "Failed to resolve chat model",
                RagPipelineOperation.VALIDATE,
                query,
                "",
                exception
            );
        }

        if (model == null || model.isBlank()) {
            throw new RagPipelineException(
                "Chat model must not be blank",
                RagPipelineOperation.VALIDATE,
                query,
                "",
                null
            );
        }

        return model.trim();
    }

    private RagResponse requireRagResponse(
        RagResponse response,
        String query
    ) throws RagPipelineException {

        if (response == null) {
            throw new RagPipelineException(
                "RagService returned null response",
                RagPipelineOperation.PREPARE_PROMPT,
                query,
                "",
                null
            );
        }

        if (response.getAugmentedPrompt() == null
            || response.getAugmentedPrompt().isBlank()) {

            throw new RagPipelineException(
                "RAG augmented prompt must not be blank",
                RagPipelineOperation.PREPARE_PROMPT,
                query,
                "",
                null
            );
        }

        return response;
    }

    private RetrievalResult requireRetrievalResult(
        RetrievalResult result,
        String query
    ) throws RagPipelineException {

        if (result == null) {
            throw new RagPipelineException(
                "RagService returned null retrieval result",
                RagPipelineOperation.RETRIEVE,
                query,
                "",
                null
            );
        }

        return result;
    }

    private RagContext requireContext(
        RagContext context,
        String query
    ) throws RagPipelineException {

        if (context == null) {
            throw new RagPipelineException(
                "RagService returned null context",
                RagPipelineOperation.BUILD_CONTEXT,
                query,
                "",
                null
            );
        }

        return context;
    }

    private String validatePrompt(
        String userPrompt
    ) throws RagPipelineException {

        try {
            return validateUserPrompt(
                userPrompt
            );

        } catch (IllegalArgumentException exception) {
            throw new RagPipelineException(
                "User prompt is invalid",
                RagPipelineOperation.VALIDATE,
                "",
                "",
                exception
            );
        }
    }

    private String validateQuery(
        String query
    ) throws RagPipelineException {

        String normalized =
            normalizeMultiline(query);

        if (normalized.isBlank()) {
            throw new RagPipelineException(
                "Retrieval query must not be blank",
                RagPipelineOperation.VALIDATE,
                "",
                "",
                null
            );
        }

        return normalized;
    }

    private void validateRetrievalRequest(
        RetrievalRequest request
    ) throws RagPipelineException {

        if (request == null) {
            throw new RagPipelineException(
                "RetrievalRequest must not be null",
                RagPipelineOperation.VALIDATE,
                "",
                "",
                null
            );
        }

        validateQuery(
            request.getQuery()
        );

        if (request.getTopK() < 1) {
            throw new RagPipelineException(
                "Retrieval topK must be greater than zero",
                RagPipelineOperation.VALIDATE,
                request.getQuery(),
                "",
                null
            );
        }

        if (!Double.isFinite(
            request.getMinimumScore()
        )) {
            throw new RagPipelineException(
                "Retrieval minimumScore must be finite",
                RagPipelineOperation.VALIDATE,
                request.getQuery(),
                "",
                null
            );
        }

        if (request.getSimilarityType() == null) {
            throw new RagPipelineException(
                "Retrieval similarityType must not be null",
                RagPipelineOperation.VALIDATE,
                request.getQuery(),
                "",
                null
            );
        }
    }

    /**
     * RAG, Chat 모델 및 LLM 클라이언트의 가용성을 확인합니다.
     */
    @Override
    public boolean isAvailable() {
        try {
            String model = chatModelProvider.getModel();

            if (model == null
                || model.isBlank()) {

                return false;
            }

            return ragService.isAvailable();

        } catch (RuntimeException exception) {
            return false;
        }
    }
    
    public RagService getRagService() {
        return ragService;
    }

    public LlmClient getLlmClient() {
        return llmClient;
    }

    public ChatModelProvider getChatModelProvider() {
        return chatModelProvider;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public int getMaximumOutputTokens() {
        return maximumOutputTokens;
    }

    @Override
    public String toString() {
        return "DefaultRagPipeline{" +
            "ragService="
                + ragService.getClass().getSimpleName() +
            ", llmClient="
                + llmClient.getClass().getSimpleName() +
            ", chatModelProvider="
                + chatModelProvider.getClass().getSimpleName() +
            ", maximumOutputTokens="
                + maximumOutputTokens +
            '}';
    }

    private static String normalizeMultiline(
        String value
    ) {
        if (value == null) {
            return "";
        }

        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replaceAll("[\\t ]+", " ")
            .replaceAll("\\n[\\t ]+", "\n")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }
}