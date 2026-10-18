/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.config;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import kr.co.goms.ai.llm.model.ChatModelProvider;
import kr.co.goms.ai.rag.RagService;
import kr.co.goms.ai.rag.context.RagContextBuilder;
import kr.co.goms.ai.rag.embedding.EmbeddingModelProvider;
import kr.co.goms.ai.rag.hash.HashService;
import kr.co.goms.ai.rag.index.RagIndexRequest;
import kr.co.goms.ai.rag.index.RagIndexer;
import kr.co.goms.ai.rag.pipeline.RagPipeline;
import kr.co.goms.ai.rag.prompt.PromptAugmentor;
import kr.co.goms.ai.rag.retrieval.Retriever;
import kr.co.goms.ai.rag.vector.VectorStore;
import kr.co.goms.ai.rag.vector.VectorStoreException;

/**
 * RagComponentFactory가 생성한 RAG 구성요소 모음입니다.
 *
 * <p>애플리케이션에서는 이 객체를 하나 보관하고 필요한 구성요소를
 * Getter로 조회하여 사용합니다.</p>
 */
public final class RagComponents implements AutoCloseable {

    private final RagConfiguration configuration;
    private final ChatModelProvider chatModelProvider;
    private final EmbeddingModelProvider embeddingModelProvider;
    private final VectorStore vectorStore;
    private final HashService hashService;
    private final RagIndexer ragIndexer;
    private final Retriever retriever;
    private final RagContextBuilder ragContextBuilder;
    private final PromptAugmentor promptAugmentor;
    private final RagService ragService;
    private final RagPipeline ragPipeline;
    private final RagIndexRequest defaultIndexRequest;

    private final AtomicBoolean closed =
        new AtomicBoolean(false);

    private RagComponents(Builder builder) {
        this.configuration =
            Objects.requireNonNull(
                builder.configuration,
                "configuration must not be null"
            );

        this.chatModelProvider =
            Objects.requireNonNull(
                builder.chatModelProvider,
                "chatModelProvider must not be null"
            );

        this.embeddingModelProvider =
            Objects.requireNonNull(
                builder.embeddingModelProvider,
                "embeddingModelProvider must not be null"
            );

        this.vectorStore =
            Objects.requireNonNull(
                builder.vectorStore,
                "vectorStore must not be null"
            );

        this.hashService =
            Objects.requireNonNull(
                builder.hashService,
                "hashService must not be null"
            );

        this.ragIndexer =
            Objects.requireNonNull(
                builder.ragIndexer,
                "ragIndexer must not be null"
            );

        this.retriever =
            Objects.requireNonNull(
                builder.retriever,
                "retriever must not be null"
            );

        this.ragContextBuilder =
            Objects.requireNonNull(
                builder.ragContextBuilder,
                "ragContextBuilder must not be null"
            );

        this.promptAugmentor =
            Objects.requireNonNull(
                builder.promptAugmentor,
                "promptAugmentor must not be null"
            );

        this.ragService =
            Objects.requireNonNull(
                builder.ragService,
                "ragService must not be null"
            );

        this.ragPipeline =
            Objects.requireNonNull(
                builder.ragPipeline,
                "ragPipeline must not be null"
            );

        this.defaultIndexRequest =
            Objects.requireNonNull(
                builder.defaultIndexRequest,
                "defaultIndexRequest must not be null"
            );
    }

    public static Builder builder() {
        return new Builder();
    }

    public RagConfiguration getConfiguration() {
        return configuration;
    }

    public ChatModelProvider getChatModelProvider() {
        return chatModelProvider;
    }

    public EmbeddingModelProvider
        getEmbeddingModelProvider() {

        return embeddingModelProvider;
    }

    public VectorStore getVectorStore() {
        return vectorStore;
    }

    public HashService getHashService() {
        return hashService;
    }

    public RagIndexer getRagIndexer() {
        return ragIndexer;
    }

    public Retriever getRetriever() {
        return retriever;
    }

    public RagContextBuilder getRagContextBuilder() {
        return ragContextBuilder;
    }

    public PromptAugmentor getPromptAugmentor() {
        return promptAugmentor;
    }

    public RagService getRagService() {
        return ragService;
    }

    public RagPipeline getRagPipeline() {
        return ragPipeline;
    }

    public RagIndexRequest getDefaultIndexRequest() {
        return defaultIndexRequest;
    }

    public boolean isAvailable() {
        return !closed.get()
            && ragIndexer.isAvailable()
            && ragPipeline.isAvailable();
    }

    public boolean isClosed() {
        return closed.get();
    }

    /**
     * VectorStore가 사용하는 리소스를 해제합니다.
     */
    @Override
    public void close()
        throws VectorStoreException {

        if (!closed.compareAndSet(false, true)) {
            return;
        }

        vectorStore.close();
    }

    @Override
    public String toString() {
        return "RagComponents{" +
            "configuration=" + configuration +
            ", vectorStore="
                + vectorStore
                    .getClass()
                    .getSimpleName() +
            ", ragIndexer="
                + ragIndexer
                    .getClass()
                    .getSimpleName() +
            ", retriever="
                + retriever
                    .getClass()
                    .getSimpleName() +
            ", ragService="
                + ragService
                    .getClass()
                    .getSimpleName() +
            ", ragPipeline="
                + ragPipeline
                    .getClass()
                    .getSimpleName() +
            ", closed=" + closed +
            '}';
    }

    public static final class Builder {

        private RagConfiguration configuration;
        private ChatModelProvider chatModelProvider;
        private EmbeddingModelProvider embeddingModelProvider;
        private VectorStore vectorStore;
        private HashService hashService;
        private RagIndexer ragIndexer;
        private Retriever retriever;
        private RagContextBuilder ragContextBuilder;
        private PromptAugmentor promptAugmentor;
        private RagService ragService;
        private RagPipeline ragPipeline;
        private RagIndexRequest defaultIndexRequest;

        private Builder() {
        }

        public Builder configuration(
            RagConfiguration configuration
        ) {
            this.configuration = configuration;
            return this;
        }

        public Builder chatModelProvider(
            ChatModelProvider chatModelProvider
        ) {
            this.chatModelProvider =
                chatModelProvider;

            return this;
        }

        public Builder embeddingModelProvider(
            EmbeddingModelProvider embeddingModelProvider
        ) {
            this.embeddingModelProvider =
                embeddingModelProvider;

            return this;
        }

        public Builder vectorStore(
            VectorStore vectorStore
        ) {
            this.vectorStore = vectorStore;
            return this;
        }

        public Builder hashService(
            HashService hashService
        ) {
            this.hashService = hashService;
            return this;
        }

        public Builder ragIndexer(
            RagIndexer ragIndexer
        ) {
            this.ragIndexer = ragIndexer;
            return this;
        }

        public Builder retriever(
            Retriever retriever
        ) {
            this.retriever = retriever;
            return this;
        }

        public Builder ragContextBuilder(
            RagContextBuilder ragContextBuilder
        ) {
            this.ragContextBuilder =
                ragContextBuilder;

            return this;
        }

        public Builder promptAugmentor(
            PromptAugmentor promptAugmentor
        ) {
            this.promptAugmentor =
                promptAugmentor;

            return this;
        }

        public Builder ragService(
            RagService ragService
        ) {
            this.ragService = ragService;
            return this;
        }

        public Builder ragPipeline(
            RagPipeline ragPipeline
        ) {
            this.ragPipeline = ragPipeline;
            return this;
        }

        public Builder defaultIndexRequest(
            RagIndexRequest defaultIndexRequest
        ) {
            this.defaultIndexRequest =
                defaultIndexRequest;

            return this;
        }

        public RagComponents build() {
            return new RagComponents(this);
        }
    }
}