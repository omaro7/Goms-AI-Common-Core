/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.runtime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import kr.co.goms.ai.llm.LlmClient;
import kr.co.goms.ai.rag.config.RagComponentFactory;
import kr.co.goms.ai.rag.config.RagComponents;
import kr.co.goms.ai.rag.config.RagConfiguration;
import kr.co.goms.ai.rag.document.DocumentLoader;
import kr.co.goms.ai.rag.embedding.EmbeddingClient;
import kr.co.goms.ai.rag.index.DocumentIndexer;
import kr.co.goms.ai.rag.index.RagIndexException;
import kr.co.goms.ai.rag.index.RagIndexRequest;
import kr.co.goms.ai.rag.index.RagIndexResult;
import kr.co.goms.ai.rag.model.DocumentSource;
import kr.co.goms.ai.rag.pipeline.RagPipeline;
import kr.co.goms.ai.rag.pipeline.RagPipelineException;
import kr.co.goms.ai.rag.pipeline.RagPipelineResponse;
import kr.co.goms.ai.rag.retrieval.RetrievalRequest;
import kr.co.goms.ai.rag.vector.VectorStore;
import kr.co.goms.ai.rag.vector.VectorStoreException;

/**
 * GomsBook Editor에서 RAG 구성요소와 현재 프로젝트 Context를 관리하는 런타임입니다.
 *
 * <pre>
 * RagRuntime
 *      │
 *      ├─ projectRoot
 *      ├─ projectId
 *      │
 *      ├─ index()
 *      ├─ removeIndex()
 *      ├─ clearIndex()
 *      └─ execute()
 *              ↓
 *        RetrievalRequest
 *              ↓
 *          projectId
 * </pre>
 *
 * <h2>Project ID 정책</h2>
 *
 * <p>{@code projectId}는 DB UUID가 아니라 프로젝트 디렉터리명 기반의
 * GomsBook 논리 프로젝트 ID입니다.</p>
 *
 * <pre>
 * C:/1004.GomsBook/03.Project/lunchwork_seoul
 *                              ↓
 * projectId = lunchwork_seoul
 * </pre>
 *
 * <p>projectRoot와 projectId는 항상 하나의 Project Context로 함께 설정하고
 * 함께 해제합니다.</p>
 *
 * <h2>Project Scope 정책</h2>
 *
 * <p>인덱싱, 삭제, 초기화, 검색은 모두 현재 projectId 범위 안에서 수행합니다.
 * 따라서 여러 프로젝트가 동일한 VectorStore 또는 Qdrant Collection을
 * 공유하더라도 서로의 데이터를 조회하거나 삭제하지 않습니다.</p>
 *
 * <h2>곰스북 1줄 원칙</h2>
 *
 * <p>메서드 선언, 단순 조건문, 변수 선언, return 및 단순 메서드 호출은
 * 가능한 한 한 줄로 작성합니다. Builder 및 복합 로직은 의미 단위로
 * 줄을 나눕니다.</p>
 */
public final class RagRuntime implements AutoCloseable {

    /**
     * 기본 전역 Runtime입니다.
     */
    private static final AtomicReference<RagRuntime> DEFAULT_INSTANCE = new AtomicReference<>();

    private final RagConfiguration configuration;
    private final DocumentLoader documentLoader;
    private final RagComponents components;

    /**
     * 프로젝트 전환, 인덱싱 및 종료 간 동시 실행 충돌을 방지합니다.
     */
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();

    private volatile RagRuntimeState state = RagRuntimeState.CREATED;

    /**
     * 현재 GomsBook 논리 프로젝트 ID입니다.
     *
     * <p>예: {@code lunchwork_seoul}</p>
     */
    private volatile String projectId;

    /**
     * 현재 GomsBook 프로젝트 Root 경로입니다.
     */
    private volatile Path projectRoot;

    /**
     * RAG Runtime을 생성합니다.
     *
     * <p>Builder에 projectRoot가 지정되어 있으면 projectRoot와 projectId를
     * 동시에 초기화합니다.</p>
     */
    private RagRuntime(Builder builder) {

        this.configuration = Objects.requireNonNullElseGet(builder.configuration, RagConfiguration::defaults);
        this.documentLoader = Objects.requireNonNull(builder.documentLoader, "documentLoader must not be null");

        DocumentIndexer documentIndexer = Objects.requireNonNull(builder.documentIndexer, "documentIndexer must not be null");
        EmbeddingClient embeddingClient = Objects.requireNonNull(builder.embeddingClient, "embeddingClient must not be null");
        LlmClient llmClient = Objects.requireNonNull(builder.llmClient, "llmClient must not be null");

        RagComponentFactory.Builder factoryBuilder = RagComponentFactory.builder()
            .configuration(configuration)
            .documentIndexer(documentIndexer)
            .embeddingClient(embeddingClient)
            .llmClient(llmClient);

        if (builder.vectorStore != null) factoryBuilder.vectorStore(builder.vectorStore);
        if (builder.pipelineSystemPrompt != null) factoryBuilder.pipelineSystemPrompt(builder.pipelineSystemPrompt);
        if (builder.augmentorSystemInstruction != null) factoryBuilder.augmentorSystemInstruction(builder.augmentorSystemInstruction);
        if (builder.responseInstruction != null) factoryBuilder.responseInstruction(builder.responseInstruction);

        this.components = factoryBuilder.build().create();

        if (builder.projectRoot != null) {

            this.projectRoot = normalizeProjectRoot(builder.projectRoot);
            this.projectId = resolveProjectId(this.projectRoot);
        }
    }

    public static Builder builder() {

        return new Builder();
    }

    /**
     * 기본 전역 Runtime을 등록합니다.
     */
    public static void installDefault(RagRuntime runtime) {

        Objects.requireNonNull(runtime, "runtime must not be null");

        if (!DEFAULT_INSTANCE.compareAndSet(null, runtime)) throw new IllegalStateException("Default RagRuntime is already installed");
    }

    /**
     * 기본 전역 Runtime을 반환합니다.
     */
    public static RagRuntime getDefault() {

        RagRuntime runtime = DEFAULT_INSTANCE.get();

        if (runtime == null) throw new IllegalStateException("Default RagRuntime is not installed");

        return runtime;
    }

    public static boolean hasDefault() {

        return DEFAULT_INSTANCE.get() != null;
    }

    /**
     * 기본 Runtime을 해제하고 종료합니다.
     */
    public static void closeDefault() throws RagRuntimeException {

        RagRuntime runtime = DEFAULT_INSTANCE.getAndSet(null);

        if (runtime != null) runtime.close();
    }

    /**
     * RAG Runtime을 시작합니다.
     */
    public void start() throws RagRuntimeException {

        lifecycleLock.writeLock().lock();

        try {

            if (state == RagRuntimeState.RUNNING) return;

            if (state == RagRuntimeState.CLOSED) {
                throw new RagRuntimeException("RagRuntime is already closed", RagRuntimeOperation.START, state);
            }

            state = RagRuntimeState.STARTING;

            if (!components.isAvailable()) {

                state = RagRuntimeState.FAILED;

                throw new RagRuntimeException("RAG components are not available", RagRuntimeOperation.START, state);
            }

            state = RagRuntimeState.RUNNING;

        } catch (RagRuntimeException exception) {

            throw exception;

        } catch (RuntimeException exception) {

            state = RagRuntimeState.FAILED;

            throw new RagRuntimeException("Failed to start RagRuntime", RagRuntimeOperation.START, state, exception);

        } finally {

            lifecycleLock.writeLock().unlock();
        }
    }

    /**
     * 현재 GomsBook 프로젝트를 엽니다.
     *
     * <p>프로젝트가 변경되면 projectRoot와 projectId를 함께 갱신합니다.</p>
     *
     * <p>{@code clearExistingIndex=true}인 경우 새 프로젝트를 설정하기 전에
     * 기존 프로젝트의 현재 Embedding Model Vector만 삭제합니다.</p>
     */
    public void openProject(Path newProjectRoot, boolean clearExistingIndex) throws RagRuntimeException {

        Path normalizedRoot = normalizeProjectRoot(newProjectRoot);
        String normalizedProjectId = resolveProjectId(normalizedRoot);

        lifecycleLock.writeLock().lock();

        try {

            ensureRunning(RagRuntimeOperation.OPEN_PROJECT);

            boolean changed = projectRoot == null || !projectRoot.equals(normalizedRoot);

            /*
             * 기존 프로젝트가 존재하는 경우에만 기존 프로젝트 Scope를 정리합니다.
             *
             * 반드시 새 projectId를 설정하기 전에 수행해야 합니다.
             */
            if (changed && clearExistingIndex && hasProjectContext()) clearIndexInternal();

            this.projectRoot = normalizedRoot;
            this.projectId = normalizedProjectId;

        } finally {

            lifecycleLock.writeLock().unlock();
        }
    }

    /**
     * 현재 GomsBook 프로젝트를 엽니다.
     *
     * <p>Qdrant와 같은 영속 VectorStore에서는 프로젝트 전환 시 기존 인덱스를
     * 유지하는 것이 기본이므로 자동 삭제하지 않습니다.</p>
     */
    public void openProject(Path newProjectRoot) throws RagRuntimeException {

        openProject(newProjectRoot, false);
    }

    /**
     * 현재 프로젝트를 닫습니다.
     *
     * <p>{@code clearExistingIndex=true}이면 현재 프로젝트의 현재 Embedding Model
     * Vector만 삭제하고 Project Context를 해제합니다.</p>
     */
    public void closeProject(boolean clearExistingIndex) throws RagRuntimeException {

        lifecycleLock.writeLock().lock();

        try {

            ensureRunning(RagRuntimeOperation.CLOSE_PROJECT);

            if (clearExistingIndex && hasProjectContext()) clearIndexInternal();

            projectRoot = null;
            projectId = null;

        } finally {

            lifecycleLock.writeLock().unlock();
        }
    }

    /**
     * 현재 프로젝트를 닫습니다.
     *
     * <p>영속 VectorStore의 인덱스는 기본적으로 유지합니다.</p>
     */
    public void closeProject() throws RagRuntimeException {

        closeProject(false);
    }

    /**
     * 현재 프로젝트 문서 하나를 기본 설정으로 인덱싱합니다.
     */
    public RagIndexResult index(Path relativePath) throws RagRuntimeException {

        return index(relativePath, components.getDefaultIndexRequest());
    }

    /**
     * 현재 프로젝트 문서 하나를 지정된 설정으로 인덱싱합니다.
     *
     * <p>현재 Runtime의 projectId를 RagIndexer에 전달합니다.</p>
     */
    public RagIndexResult index(Path relativePath, RagIndexRequest request) throws RagRuntimeException {

        Objects.requireNonNull(relativePath, "relativePath must not be null");
        Objects.requireNonNull(request, "request must not be null");

        lifecycleLock.readLock().lock();

        try {

            ensureRunning(RagRuntimeOperation.INDEX);

            Path root = requireProjectRoot();
            String currentProjectId = requireProjectId(RagRuntimeOperation.INDEX);

            DocumentSource source;

            try {

                source = documentLoader.load(root, relativePath);

            } catch (Exception exception) {

                throw new RagRuntimeException(
                    "Failed to load document: " + relativePath,
                    RagRuntimeOperation.LOAD_DOCUMENT,
                    state,
                    exception
                );
            }

            try {

                return components.getRagIndexer().index(currentProjectId, source, request);

            } catch (RagIndexException exception) {

                throw new RagRuntimeException(
                    "Failed to index document: " + relativePath,
                    RagRuntimeOperation.INDEX,
                    state,
                    exception
                );
            }

        } finally {

            lifecycleLock.readLock().unlock();
        }
    }

    /**
     * 현재 프로젝트의 여러 문서를 기본 설정으로 인덱싱합니다.
     */
    public List<RagIndexResult> indexAll(List<Path> relativePaths) throws RagRuntimeException {

        return indexAll(relativePaths, components.getDefaultIndexRequest());
    }

    /**
     * 현재 프로젝트의 여러 문서를 순차적으로 인덱싱합니다.
     */
    public List<RagIndexResult> indexAll(List<Path> relativePaths, RagIndexRequest request) throws RagRuntimeException {

        if (relativePaths == null || relativePaths.isEmpty()) return List.of();

        Objects.requireNonNull(request, "request must not be null");

        List<RagIndexResult> results = new ArrayList<>(relativePaths.size());

        for (Path relativePath : relativePaths) {

            if (relativePath == null) continue;

            try {

                results.add(index(relativePath, request));

            } catch (RagRuntimeException exception) {

                if (!request.isContinueOnError()) throw exception;

                results.add(RagIndexResult.failed(normalizePath(relativePath), safeMessage(exception)));
            }
        }

        return List.copyOf(results);
    }

    /**
     * 현재 프로젝트 Scope로 기본 RAG Pipeline을 실행합니다.
     *
     * <p>기존 {@code RagPipeline.execute(userPrompt)}를 직접 호출하지 않고
     * 현재 projectId가 포함된 RetrievalRequest를 생성하여 명시적으로 전달합니다.</p>
     */
    public RagPipelineResponse execute(String userPrompt) throws RagRuntimeException {

        lifecycleLock.readLock().lock();

        try {

            ensureRunning(RagRuntimeOperation.EXECUTE);

            RetrievalRequest retrievalRequest = createDefaultRetrievalRequest(userPrompt);

            try {

                return components.getRagPipeline().execute(userPrompt, retrievalRequest);

            } catch (RagPipelineException exception) {

                throw new RagRuntimeException(
                    "Failed to execute RAG pipeline",
                    RagRuntimeOperation.EXECUTE,
                    state,
                    exception
                );
            }

        } finally {

            lifecycleLock.readLock().unlock();
        }
    }

    /**
     * 지정된 검색 조건으로 현재 프로젝트 RAG Pipeline을 실행합니다.
     *
     * <p>RetrievalRequest에 다른 projectId가 지정되어 있으면 현재 Runtime
     * Project Context와 충돌하므로 요청을 거부합니다.</p>
     *
     * <p>projectId가 없거나 현재 projectId와 동일하면 현재 Runtime의 projectId를
     * 명시적으로 적용한 새 RetrievalRequest를 생성합니다.</p>
     */
    public RagPipelineResponse execute(String userPrompt, RetrievalRequest retrievalRequest) throws RagRuntimeException {

        Objects.requireNonNull(retrievalRequest, "retrievalRequest must not be null");

        lifecycleLock.readLock().lock();

        try {

            ensureRunning(RagRuntimeOperation.EXECUTE);

            RetrievalRequest scopedRequest = bindProjectScope(retrievalRequest);

            try {

                return components.getRagPipeline().execute(userPrompt, scopedRequest);

            } catch (RagPipelineException exception) {

                throw new RagRuntimeException(
                    "Failed to execute RAG pipeline",
                    RagRuntimeOperation.EXECUTE,
                    state,
                    exception
                );
            }

        } finally {

            lifecycleLock.readLock().unlock();
        }
    }

    /**
     * 기본 검색 설정으로 현재 프로젝트 RetrievalRequest를 생성합니다.
     */
    private RetrievalRequest createDefaultRetrievalRequest(String query) throws RagRuntimeException {

        String currentProjectId = requireProjectId(RagRuntimeOperation.EXECUTE);
        String normalizedQuery = requireText(query, "query");

        return RetrievalRequest.builder()
            .projectId(currentProjectId)
            .query(normalizedQuery)
            .topK(configuration.getTopK())
            .minimumScore(configuration.getMinimumScore())
            .similarityType(configuration.getSimilarityType())
            .preserveDocumentOrder(configuration.isPreserveDocumentOrder())
            .build();
    }

    /**
     * 외부 RetrievalRequest를 현재 Runtime Project Scope에 바인딩합니다.
     *
     * <p>검색 필터와 검색 옵션은 그대로 보존합니다.</p>
     */
    private RetrievalRequest bindProjectScope(RetrievalRequest request) throws RagRuntimeException {

        String currentProjectId = requireProjectId(RagRuntimeOperation.EXECUTE);
        String requestedProjectId = normalizeText(request.getProjectId());

        if (!requestedProjectId.isBlank() && !currentProjectId.equals(requestedProjectId)) {
            throw new RagRuntimeException(
                "RetrievalRequest projectId does not match current project. current=" + currentProjectId + ", requested=" + requestedProjectId,
                RagRuntimeOperation.EXECUTE,
                state
            );
        }

        return RetrievalRequest.builder()
            .projectId(currentProjectId)
            .query(request.getQuery())
            .topK(request.getTopK())
            .minimumScore(request.getMinimumScore())
            .similarityType(request.getSimilarityType())
            .chunkTypes(request.getChunkTypes())
            .sourcePaths(request.getSourcePaths())
            .epubTypes(request.getEpubTypes())
            .languages(request.getLanguages())
            .metadataFilters(request.getMetadataFilters())
            .includeRejected(request.isIncludeRejected())
            .preserveDocumentOrder(request.isPreserveDocumentOrder())
            .build();
    }

    public String chat(String userPrompt) throws RagRuntimeException {

        return execute(userPrompt).getAnswer();
    }

    public String chat(String userPrompt, RetrievalRequest retrievalRequest) throws RagRuntimeException {

        return execute(userPrompt, retrievalRequest).getAnswer();
    }

    /**
     * 현재 프로젝트 문서의 현재 Embedding Model 인덱스를 제거합니다.
     */
    public int removeIndex(String sourcePath) throws RagRuntimeException {

        lifecycleLock.writeLock().lock();

        try {

            ensureRunning(RagRuntimeOperation.REMOVE_INDEX);

            String currentProjectId = requireProjectId(RagRuntimeOperation.REMOVE_INDEX);
            String normalizedSourcePath = requireText(sourcePath, "sourcePath");

            try {

                return components.getRagIndexer().remove(currentProjectId, normalizedSourcePath);

            } catch (RagIndexException exception) {

                throw new RagRuntimeException(
                    "Failed to remove document index",
                    RagRuntimeOperation.REMOVE_INDEX,
                    state,
                    exception
                );
            }

        } finally {

            lifecycleLock.writeLock().unlock();
        }
    }

    /**
     * 현재 프로젝트의 현재 Embedding Model Vector를 모두 삭제합니다.
     *
     * <p>다른 projectId의 Vector는 삭제하지 않습니다.</p>
     */
    public void clearIndex() throws RagRuntimeException {

        lifecycleLock.writeLock().lock();

        try {

            ensureRunning(RagRuntimeOperation.CLEAR_INDEX);
            clearIndexInternal();

        } finally {

            lifecycleLock.writeLock().unlock();
        }
    }

    /**
     * 현재 Project Scope의 현재 Embedding Model Vector만 삭제합니다.
     *
     * <p>RagIndexer.clear()는 VectorStore 전체 삭제이므로 Runtime 일반 흐름에서는
     * 사용하지 않습니다.</p>
     */
    private void clearIndexInternal() throws RagRuntimeException {

        String currentProjectId = requireProjectId(RagRuntimeOperation.CLEAR_INDEX);

        try {

            components.getRagIndexer().removeCurrentModel(currentProjectId);

        } catch (RagIndexException exception) {

            throw new RagRuntimeException(
                "Failed to clear current project RAG index",
                RagRuntimeOperation.CLEAR_INDEX,
                state,
                exception
            );
        }
    }

    /**
     * RAG 구성요소 사용 가능 여부를 반환합니다.
     */
    public boolean isAvailable() {

        lifecycleLock.readLock().lock();

        try {

            return state == RagRuntimeState.RUNNING && components.isAvailable();

        } catch (RuntimeException exception) {

            return false;

        } finally {

            lifecycleLock.readLock().unlock();
        }
    }

    public RagRuntimeState getState() {

        return state;
    }

    public boolean isRunning() {

        return state == RagRuntimeState.RUNNING;
    }

    public boolean isClosed() {

        return state == RagRuntimeState.CLOSED;
    }

    /**
     * 현재 Project Context가 존재하는지 확인합니다.
     */
    public boolean hasProject() {

        return hasProjectContext();
    }

    /**
     * 현재 GomsBook 논리 projectId를 반환합니다.
     */
    public String getProjectId() {

        return projectId;
    }

    public Path getProjectRoot() {

        return projectRoot;
    }

    public RagConfiguration getConfiguration() {

        return configuration;
    }

    public RagComponents getComponents() {

        return components;
    }

    public RagPipeline getRagPipeline() {

        return components.getRagPipeline();
    }

    public DocumentLoader getDocumentLoader() {

        return documentLoader;
    }

    /**
     * Runtime과 VectorStore 리소스를 종료합니다.
     *
     * <p>프로젝트 Vector 데이터를 명시적으로 삭제하지 않습니다.
     * QdrantVectorStore와 같은 영속 저장소는 데이터를 유지할 수 있습니다.</p>
     */
    @Override
    public void close() throws RagRuntimeException {

        lifecycleLock.writeLock().lock();

        try {

            if (state == RagRuntimeState.CLOSED) return;

            state = RagRuntimeState.STOPPING;

            try {

                components.close();

            } catch (VectorStoreException exception) {

                state = RagRuntimeState.FAILED;

                throw new RagRuntimeException(
                    "Failed to close RAG components",
                    RagRuntimeOperation.CLOSE,
                    state,
                    exception
                );
            }

            projectRoot = null;
            projectId = null;
            state = RagRuntimeState.CLOSED;

            DEFAULT_INSTANCE.compareAndSet(this, null);

        } finally {

            lifecycleLock.writeLock().unlock();
        }
    }

    /**
     * Runtime 실행 상태를 확인합니다.
     */
    private void ensureRunning(RagRuntimeOperation operation) throws RagRuntimeException {

        if (state != RagRuntimeState.RUNNING) {
            throw new RagRuntimeException(
                "RagRuntime is not running. state=" + state,
                operation,
                state
            );
        }
    }

    /**
     * 현재 Project Context가 완전하게 존재하는지 확인합니다.
     */
    private boolean hasProjectContext() {

        return projectRoot != null && projectId != null && !projectId.isBlank();
    }

    /**
     * 현재 projectId를 반환합니다.
     *
     * <p>Project Context가 없으면 현재 작업 종류를 포함한 RuntimeException을 발생시킵니다.</p>
     */
    private String requireProjectId(RagRuntimeOperation operation) throws RagRuntimeException {

        String value = projectId;

        if (value == null || value.isBlank()) {
            throw new RagRuntimeException(
                "No GomsBook project is currently open",
                operation,
                state
            );
        }

        return value;
    }

    /**
     * 현재 projectRoot를 반환합니다.
     */
    private Path requireProjectRoot() throws RagRuntimeException {

        Path root = projectRoot;

        if (root == null) {
            throw new RagRuntimeException(
                "No EPUB project is currently open",
                RagRuntimeOperation.LOAD_DOCUMENT,
                state
            );
        }

        return root;
    }

    /**
     * 프로젝트 Root의 마지막 디렉터리명을 논리 projectId로 사용합니다.
     */
    private static String resolveProjectId(Path projectRoot) {

        Path normalizedRoot = normalizeProjectRoot(projectRoot);
        Path fileName = normalizedRoot.getFileName();

        if (fileName == null) throw new IllegalArgumentException("Unable to resolve projectId from projectRoot: " + normalizedRoot);

        return requireText(fileName.toString(), "projectId");
    }

    /**
     * projectRoot를 절대 정규 경로로 변환합니다.
     */
    private static Path normalizeProjectRoot(Path path) {

        Objects.requireNonNull(path, "projectRoot must not be null");

        return path.toAbsolutePath().normalize();
    }

    /**
     * Path를 RAG 공통 slash 형식으로 변환합니다.
     */
    private static String normalizePath(Path path) {

        return path.normalize().toString().replace('\\', '/');
    }

    /**
     * 필수 문자열을 검증합니다.
     */
    private static String requireText(String value, String fieldName) {

        String normalized = normalizeText(value);

        if (normalized.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");

        return normalized;
    }

    /**
     * Nullable 문자열을 trim 처리합니다.
     */
    private static String normalizeText(String value) {

        return value == null ? "" : value.trim();
    }

    /**
     * 예외 메시지를 안전하게 반환합니다.
     */
    private static String safeMessage(Throwable throwable) {

        if (throwable == null) return "Unknown error";

        String message = throwable.getMessage();

        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message.trim();
    }

    @Override
    public String toString() {

        return "RagRuntime{"
            + "state=" + state
            + ", projectId='" + projectId + '\''
            + ", projectRoot=" + projectRoot
            + ", configuration=" + configuration
            + ", components=" + components
            + '}';
    }

    public static final class Builder {

        private RagConfiguration configuration;
        private DocumentLoader documentLoader;
        private DocumentIndexer documentIndexer;
        private EmbeddingClient embeddingClient;
        private LlmClient llmClient;
        private VectorStore vectorStore;
        private Path projectRoot;

        private String pipelineSystemPrompt;
        private String augmentorSystemInstruction;
        private String responseInstruction;

        private Builder() {
        }

        public Builder configuration(RagConfiguration configuration) {

            this.configuration = configuration;

            return this;
        }

        public Builder documentLoader(DocumentLoader documentLoader) {

            this.documentLoader = documentLoader;

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

        public Builder vectorStore(VectorStore vectorStore) {

            this.vectorStore = vectorStore;

            return this;
        }

        public Builder projectRoot(Path projectRoot) {

            this.projectRoot = projectRoot;

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

        public RagRuntime build() {

            return new RagRuntime(this);
        }
    }
}