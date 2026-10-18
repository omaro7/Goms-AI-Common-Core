/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.config;

import java.util.Objects;

import kr.co.goms.ai.llm.LlmClient;
import kr.co.goms.ai.llm.model.ChatModelProvider;
import kr.co.goms.ai.llm.model.DefaultChatModelProvider;
import kr.co.goms.ai.rag.DefaultRagService;
import kr.co.goms.ai.rag.RagService;
import kr.co.goms.ai.rag.context.RagContextBuilder;
import kr.co.goms.ai.rag.embedding.DefaultEmbeddingModelProvider;
import kr.co.goms.ai.rag.embedding.EmbeddingClient;
import kr.co.goms.ai.rag.embedding.EmbeddingModelProvider;
import kr.co.goms.ai.rag.hash.HashService;
import kr.co.goms.ai.rag.hash.Sha256HashService;
import kr.co.goms.ai.rag.index.DefaultRagIndexer;
import kr.co.goms.ai.rag.index.DocumentIndexer;
import kr.co.goms.ai.rag.index.RagIndexRequest;
import kr.co.goms.ai.rag.index.RagIndexer;
import kr.co.goms.ai.rag.pipeline.DefaultRagPipeline;
import kr.co.goms.ai.rag.pipeline.RagPipeline;
import kr.co.goms.ai.rag.prompt.DefaultPromptAugmentor;
import kr.co.goms.ai.rag.prompt.PromptAugmentor;
import kr.co.goms.ai.rag.retrieval.DefaultRetriever;
import kr.co.goms.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.ai.rag.retrieval.Retriever;
import kr.co.goms.ai.rag.vector.InMemoryVectorStore;
import kr.co.goms.ai.rag.vector.VectorStore;

/**
 * {@link RagConfiguration}을 기준으로 GomsBook RAG 구성요소를 생성하고
 * 서로 연결하는 팩토리입니다.
 *
 * <pre>
 * RagConfiguration
 *      ↓
 * RagComponentFactory
 *      │
 *      ├─ ChatModelProvider
 *      ├─ EmbeddingModelProvider
 *      ├─ HashService
 *      ├─ VectorStore
 *      ├─ RagIndexer
 *      ├─ Retriever
 *      ├─ RagService
 *      └─ RagPipeline
 * </pre>
 *
 * <h2>외부 제공 구성요소</h2>
 *
 * <ul>
 *     <li>{@link DocumentIndexer}</li>
 *     <li>{@link EmbeddingClient}</li>
 *     <li>{@link LlmClient}</li>
 * </ul>
 *
 * <h2>기본 생성 구성요소</h2>
 *
 * <ul>
 *     <li>{@link ChatModelProvider}</li>
 *     <li>{@link EmbeddingModelProvider}</li>
 *     <li>{@link HashService}</li>
 *     <li>{@link VectorStore}</li>
 *     <li>{@link RagIndexer}</li>
 *     <li>{@link Retriever}</li>
 *     <li>{@link RagService}</li>
 *     <li>{@link RagPipeline}</li>
 * </ul>
 *
 * <h2>Project ID 정책</h2>
 *
 * <p>{@code projectId}는 이 Factory 또는 DefaultRagIndexer의 고정 상태로
 * 저장하지 않습니다.</p>
 *
 * <p>{@code projectId}는 {@code lunchwork_seoul}과 같은 GomsBook 논리
 * 프로젝트 ID이며 인덱싱 및 검색 요청이 실행될 때 전달되는 실행 Context입니다.</p>
 *
 * <pre>
 * RagRuntime
 *      ↓
 * projectId = lunchwork_seoul
 *      │
 *      ├─ RagIndexer.index(projectId, ...)
 *      └─ RetrievalRequest.projectId
 * </pre>
 *
 * <h2>곰스북 1줄 원칙</h2>
 *
 * <p>메서드 선언, 단순 조건문, 변수 선언, return 및 단순 호출은 가능한 한
 * 한 줄로 작성합니다. Builder 및 복합 생성 로직은 의미 단위로 줄을 나눕니다.</p>
 */
public final class RagComponentFactory {

    public static final String DEFAULT_PIPELINE_SYSTEM_PROMPT = """
        당신은 GomsBook Editor에 통합된 AI Agent입니다.
        검색된 프로젝트 문서를 우선하여 정확하게 답하십시오.
        프로젝트 문서에 없는 내용을 사실처럼 생성하지 마십시오.
        """;

    public static final String DEFAULT_AUGMENTOR_SYSTEM_INSTRUCTION = """
        당신은 GomsBook Editor에 통합된 EPUB 제작 AI Agent입니다.

        아래 참고 문서는 현재 EPUB 프로젝트에서 검색된 실제 자료입니다.
        참고 문서 내용을 우선하여 사용자 요청을 처리하십시오.

        참고 문서에 없는 프로젝트 정보를 임의로 생성하지 마십시오.
        XHTML, EPUB, 접근성 또는 메타데이터를 다룰 때는
        원본 파일 경로와 요소 식별자를 보존하십시오.
        """;

    public static final String DEFAULT_RESPONSE_INSTRUCTION = """
        응답 규칙:
        1. 사용자 요청에 직접 답하십시오.
        2. 참고 문서에 근거한 내용과 일반적인 제안을 구분하십시오.
        3. 문서 위치를 언급할 때는 파일 경로와 요소 ID를 사용하십시오.
        4. 수정이 필요한 경우 대상, 변경 내용과 이유를 명확히 제시하십시오.
        5. 참고 문서가 부족한 경우 임의로 추측하지 마십시오.
        """;

    /**
     * RAG 전체 기본 설정입니다.
     */
    private final RagConfiguration configuration;

    /**
     * 원본 문서를 DocumentChunk로 분할하는 인덱서입니다.
     */
    private final DocumentIndexer documentIndexer;

    /**
     * Document 및 Query Embedding을 생성하는 클라이언트입니다.
     */
    private final EmbeddingClient embeddingClient;

    /**
     * 최종 응답 생성에 사용하는 LLM 클라이언트입니다.
     */
    private final LlmClient llmClient;

    /**
     * 외부에서 직접 주입한 VectorStore입니다.
     *
     * <p>null이면 {@link InMemoryVectorStore}를 기본 생성합니다.</p>
     */
    private final VectorStore suppliedVectorStore;

    /**
     * 외부에서 직접 주입한 HashService입니다.
     *
     * <p>null이면 {@link Sha256HashService}를 기본 생성합니다.</p>
     */
    private final HashService suppliedHashService;

    private final String pipelineSystemPrompt;
    private final String augmentorSystemInstruction;
    private final String responseInstruction;

    /**
     * Builder 설정으로 Factory를 생성합니다.
     */
    private RagComponentFactory(Builder builder) {

        this.configuration = Objects.requireNonNullElseGet(builder.configuration, RagConfiguration::defaults);
        this.documentIndexer = Objects.requireNonNull(builder.documentIndexer, "documentIndexer must not be null");
        this.embeddingClient = Objects.requireNonNull(builder.embeddingClient, "embeddingClient must not be null");
        this.llmClient = Objects.requireNonNull(builder.llmClient, "llmClient must not be null");

        this.suppliedVectorStore = builder.vectorStore;
        this.suppliedHashService = builder.hashService;

        this.pipelineSystemPrompt = resolveText(builder.pipelineSystemPrompt, DEFAULT_PIPELINE_SYSTEM_PROMPT);
        this.augmentorSystemInstruction = resolveText(builder.augmentorSystemInstruction, DEFAULT_AUGMENTOR_SYSTEM_INSTRUCTION);
        this.responseInstruction = resolveText(builder.responseInstruction, DEFAULT_RESPONSE_INSTRUCTION);
    }

    public static Builder builder() {

        return new Builder();
    }

    /**
     * 전체 RAG 구성요소를 생성하고 연결합니다.
     *
     * @return 조립된 RAG 구성요소
     */
    public RagComponents create() {

        ChatModelProvider chatModelProvider = createChatModelProvider();
        EmbeddingModelProvider embeddingModelProvider = createEmbeddingModelProvider();
        VectorStore vectorStore = createVectorStore();
        HashService hashService = createHashService();

        RagIndexer ragIndexer = createRagIndexer(
            embeddingModelProvider,
            vectorStore,
            hashService
        );

        Retriever retriever = createRetriever(
            embeddingModelProvider,
            vectorStore
        );

        RagContextBuilder ragContextBuilder = createRagContextBuilder();
        PromptAugmentor promptAugmentor = createPromptAugmentor();

        RagService ragService = createRagService(
            retriever,
            ragContextBuilder,
            promptAugmentor
        );

        RagPipeline ragPipeline = createRagPipeline(
            ragService,
            chatModelProvider
        );

        RagIndexRequest defaultIndexRequest = createDefaultIndexRequest();

        return RagComponents.builder()
            .configuration(configuration)
            .chatModelProvider(chatModelProvider)
            .embeddingModelProvider(embeddingModelProvider)
            .vectorStore(vectorStore)
            .hashService(hashService)
            .ragIndexer(ragIndexer)
            .retriever(retriever)
            .ragContextBuilder(ragContextBuilder)
            .promptAugmentor(promptAugmentor)
            .ragService(ragService)
            .ragPipeline(ragPipeline)
            .defaultIndexRequest(defaultIndexRequest)
            .build();
    }

    /**
     * 기본 Chat Model Provider를 생성합니다.
     */
    public ChatModelProvider createChatModelProvider() {

        return new DefaultChatModelProvider(configuration.getChatModel());
    }

    /**
     * 기본 Embedding Model Provider를 생성합니다.
     */
    public EmbeddingModelProvider createEmbeddingModelProvider() {

        return new DefaultEmbeddingModelProvider(configuration.getEmbeddingModel());
    }

    /**
     * VectorStore를 생성합니다.
     *
     * <p>외부 VectorStore가 주입되어 있으면 해당 구현체를 사용합니다.</p>
     *
     * <p>지정하지 않으면 {@link InMemoryVectorStore}를 사용합니다.
     * QdrantVectorStore를 적용할 경우 Builder를 통해 외부 주입합니다.</p>
     */
    public VectorStore createVectorStore() {

        if (suppliedVectorStore != null) return suppliedVectorStore;

        return new InMemoryVectorStore();
    }

    /**
     * HashService를 생성합니다.
     *
     * <p>외부 HashService가 없으면 SHA-256 구현을 생성합니다.</p>
     */
    public HashService createHashService() {

        if (suppliedHashService != null) return suppliedHashService;

        return new Sha256HashService();
    }

    /**
     * 기본 RagIndexer를 생성합니다.
     *
     * <p>projectId는 생성자에 전달하지 않습니다.
     * projectId는 index/remove 실행 시 전달되는 Context입니다.</p>
     */
    public RagIndexer createRagIndexer(EmbeddingModelProvider embeddingModelProvider, VectorStore vectorStore, HashService hashService) {

        return new DefaultRagIndexer(
            documentIndexer,
            embeddingClient,
            embeddingModelProvider,
            vectorStore,
            hashService
        );
    }

    /**
     * 기본 Retriever를 생성합니다.
     *
     * <p>projectId는 Retriever 생성자에 고정하지 않고
     * RetrievalRequest에서 실행 시 전달합니다.</p>
     */
    public Retriever createRetriever(EmbeddingModelProvider embeddingModelProvider, VectorStore vectorStore) {

        return new DefaultRetriever(
            embeddingClient,
            embeddingModelProvider,
            vectorStore,
            configuration.getTopK(),
            configuration.getMinimumScore()
        );
    }

    /**
     * 검색 결과를 RAG Context로 변환하는 Builder를 생성합니다.
     */
    public RagContextBuilder createRagContextBuilder() {

        return new RagContextBuilder(configuration.getMaximumContextCharacters());
    }

    /**
     * PromptAugmentor를 생성합니다.
     */
    public PromptAugmentor createPromptAugmentor() {

        return new DefaultPromptAugmentor(
            configuration.getMaximumPromptCharacters(),
            configuration.getMaximumContextCharacters(),
            configuration.isAllowEmptyContext(),
            configuration.isIncludeSources(),
            augmentorSystemInstruction,
            responseInstruction
        );
    }

    /**
     * RagService를 생성합니다.
     */
    public RagService createRagService(Retriever retriever, RagContextBuilder ragContextBuilder, PromptAugmentor promptAugmentor) {

        return new DefaultRagService(
            retriever,
            ragContextBuilder,
            promptAugmentor
        );
    }

    /**
     * 최종 RagPipeline을 생성합니다.
     */
    public RagPipeline createRagPipeline(RagService ragService, ChatModelProvider chatModelProvider) {

        return new DefaultRagPipeline(
            ragService,
            llmClient,
            chatModelProvider,
            pipelineSystemPrompt,
            configuration.getMaximumOutputTokens()
        );
    }

    /**
     * RagConfiguration 값을 적용한 기본 인덱싱 요청을 생성합니다.
     */
    public RagIndexRequest createDefaultIndexRequest() {

        return RagIndexRequest.builder()
            .batchSize(configuration.getIndexBatchSize())
            .reuseUnchanged(configuration.isReuseUnchangedChunks())
            .replaceSource(configuration.isReplaceSourceOnIndex())
            .continueOnError(configuration.isContinueOnIndexError())
            .normalize(configuration.isNormalizeEmbeddings())
            .truncate(configuration.isTruncateEmbeddingInput())
            .version(configuration.getIndexVersion())
            .build();
    }

    /**
     * 현재 프로젝트 Scope가 적용된 기본 RetrievalRequest를 생성합니다.
     *
     * <p>projectId는 {@code lunchwork_seoul}과 같은 GomsBook 논리 프로젝트 ID이며
     * VectorStore 및 Qdrant 검색 시 프로젝트 Filter로 사용됩니다.</p>
     *
     * <p>projectId 없는 검색을 허용하지 않기 위해 기존
     * {@code createRetrievalRequest(String query)}는 제공하지 않습니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param query 사용자 검색 질의
     * @return 프로젝트 Scope가 적용된 검색 요청
     */
    public RetrievalRequest createRetrievalRequest(String projectId, String query) {

        return RetrievalRequest.builder()
            .projectId(requireText(projectId, "projectId"))
            .query(requireText(query, "query"))
            .topK(configuration.getTopK())
            .minimumScore(configuration.getMinimumScore())
            .similarityType(configuration.getSimilarityType())
            .preserveDocumentOrder(configuration.isPreserveDocumentOrder())
            .build();
    }

    public RagConfiguration getConfiguration() {

        return configuration;
    }

    public DocumentIndexer getDocumentIndexer() {

        return documentIndexer;
    }

    public EmbeddingClient getEmbeddingClient() {

        return embeddingClient;
    }

    public LlmClient getLlmClient() {

        return llmClient;
    }

    /**
     * 사용자 지정 텍스트가 없으면 기본값을 반환합니다.
     */
    private static String resolveText(String value, String defaultValue) {

        if (value == null) return defaultValue.trim();

        return normalizeMultiline(value);
    }

    /**
     * 여러 줄 Prompt의 공백 및 줄바꿈을 정규화합니다.
     */
    private static String normalizeMultiline(String value) {

        if (value == null) return "";

        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replaceAll("[\\t ]+", " ")
            .replaceAll("\\n[\\t ]+", "\n")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    /**
     * 필수 문자열을 검증하고 trim 처리된 값을 반환합니다.
     */
    private static String requireText(String value, String fieldName) {

        String normalized = value == null ? "" : value.trim();

        if (normalized.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");

        return normalized;
    }

    @Override
    public String toString() {

        return "RagComponentFactory{"
            + "configuration=" + configuration
            + ", documentIndexer=" + documentIndexer.getClass().getSimpleName()
            + ", embeddingClient=" + embeddingClient.getClass().getSimpleName()
            + ", llmClient=" + llmClient.getClass().getSimpleName()
            + ", suppliedVectorStore=" + (suppliedVectorStore == null ? "default" : suppliedVectorStore.getClass().getSimpleName())
            + ", suppliedHashService=" + (suppliedHashService == null ? "default" : suppliedHashService.getClass().getSimpleName())
            + '}';
    }

    /**
     * RagComponentFactory Builder입니다.
     */
    public static final class Builder {

        private RagConfiguration configuration;
        private DocumentIndexer documentIndexer;
        private EmbeddingClient embeddingClient;
        private LlmClient llmClient;
        private VectorStore vectorStore;
        private HashService hashService;

        private String pipelineSystemPrompt;
        private String augmentorSystemInstruction;
        private String responseInstruction;

        private Builder() {
        }

        public Builder configuration(RagConfiguration configuration) {

            this.configuration = configuration;

            return this;
        }

        public Builder documentIndexer(DocumentIndexer documentIndexer) {

            this.documentIndexer = documentIndexer;

            return this;
        }

        public Builder embeddingClient(EmbeddingClient embeddingClient) {

            this.embeddingClient = embeddingClient;

            return this;
        }

        public Builder llmClient(LlmClient llmClient) {

            this.llmClient = llmClient;

            return this;
        }

        /**
         * VectorStore를 직접 지정합니다.
         *
         * <p>지정하지 않으면 InMemoryVectorStore가 사용됩니다.</p>
         */
        public Builder vectorStore(VectorStore vectorStore) {

            this.vectorStore = vectorStore;

            return this;
        }

        /**
         * HashService를 직접 지정합니다.
         *
         * <p>지정하지 않으면 Sha256HashService가 사용됩니다.</p>
         */
        public Builder hashService(HashService hashService) {

            this.hashService = hashService;

            return this;
        }

        public Builder pipelineSystemPrompt(String pipelineSystemPrompt) {

            this.pipelineSystemPrompt = pipelineSystemPrompt;

            return this;
        }

        public Builder augmentorSystemInstruction(String augmentorSystemInstruction) {

            this.augmentorSystemInstruction = augmentorSystemInstruction;

            return this;
        }

        public Builder responseInstruction(String responseInstruction) {

            this.responseInstruction = responseInstruction;

            return this;
        }

        public RagComponentFactory build() {

            return new RagComponentFactory(this);
        }
    }
}