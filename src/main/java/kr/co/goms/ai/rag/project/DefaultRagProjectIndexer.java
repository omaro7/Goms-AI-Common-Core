/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.project;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import kr.co.goms.ai.rag.document.DocumentLoader;
import kr.co.goms.ai.rag.embedding.EmbeddingModelProvider;
import kr.co.goms.ai.rag.index.RagIndexException;
import kr.co.goms.ai.rag.index.RagIndexIssue;
import kr.co.goms.ai.rag.index.RagIndexRequest;
import kr.co.goms.ai.rag.index.RagIndexResult;
import kr.co.goms.ai.rag.index.RagIndexer;
import kr.co.goms.ai.rag.model.DocumentSource;
import kr.co.goms.ai.rag.vector.VectorRecord;
import kr.co.goms.ai.rag.vector.VectorStore;
import kr.co.goms.ai.rag.vector.VectorStoreException;

/**
 * EPUB 프로젝트의 문서를 탐색하고 RAG 인덱스를 생성하거나
 * 현재 파일 상태와 VectorStore를 동기화하는 기본 구현체입니다.
 *
 * <pre>
 * EPUB 프로젝트
 *      ↓
 * 문서 탐색
 *      ↓
 * DocumentLoader
 *      ↓
 * RagIndexer
 *      ↓
 * VectorStore
 *      ↓
 * 프로젝트 인덱싱 결과
 * </pre>
 *
 * <p>VectorStore에는 프로젝트 기준 상대 경로가 저장된다고
 * 가정합니다.</p>
 */

public final class DefaultRagProjectIndexer
    implements RagProjectIndexer {

    private final DocumentLoader documentLoader;
    private final RagIndexer ragIndexer;
    private final VectorStore vectorStore;

    private final EmbeddingModelProvider
        embeddingModelProvider;

    private final RagIndexRequest
        defaultIndexRequest;

    private final RagProjectIndexRequest
        defaultProjectRequest;

    /**
     * 기본 프로젝트 인덱서를 생성합니다.
     *
     * @param documentLoader 문서 로더
     * @param ragIndexer 문서별 RAG 인덱서
     * @param vectorStore 벡터 저장소
     * @param embeddingModelProvider 임베딩 모델 제공자
     * @param defaultIndexRequest 기본 문서 인덱싱 요청
     */
    public DefaultRagProjectIndexer(
        DocumentLoader documentLoader,
        RagIndexer ragIndexer,
        VectorStore vectorStore,
        EmbeddingModelProvider embeddingModelProvider,
        RagIndexRequest defaultIndexRequest
    ) {
        this(
            documentLoader,
            ragIndexer,
            vectorStore,
            embeddingModelProvider,
            defaultIndexRequest,
            RagProjectIndexRequest.builder()
                .indexRequest(defaultIndexRequest)
                .includeDirectory("OEBPS/Text")
                .extension("xhtml")
                .recursive(true)
                .deleteMissingDocuments(true)
                .sortDocuments(true)
                .build()
        );
    }

    /**
     * 기본 프로젝트 탐색 설정까지 지정하여 생성합니다.
     */
    public DefaultRagProjectIndexer(
        DocumentLoader documentLoader,
        RagIndexer ragIndexer,
        VectorStore vectorStore,
        EmbeddingModelProvider embeddingModelProvider,
        RagIndexRequest defaultIndexRequest,
        RagProjectIndexRequest defaultProjectRequest
    ) {
        this.documentLoader =
            Objects.requireNonNull(
                documentLoader,
                "documentLoader must not be null"
            );

        this.ragIndexer =
            Objects.requireNonNull(
                ragIndexer,
                "ragIndexer must not be null"
            );

        this.vectorStore =
            Objects.requireNonNull(
                vectorStore,
                "vectorStore must not be null"
            );

        this.embeddingModelProvider =
            Objects.requireNonNull(
                embeddingModelProvider,
                "embeddingModelProvider must not be null"
            );

        this.defaultIndexRequest =
            Objects.requireNonNull(
                defaultIndexRequest,
                "defaultIndexRequest must not be null"
            );

        this.defaultProjectRequest =
            Objects.requireNonNull(
                defaultProjectRequest,
                "defaultProjectRequest must not be null"
            );
    }

    @Override
    public RagProjectIndexResult indexProject(
    	String projectId,
        Path projectRoot
    ) throws RagProjectIndexException {

        return indexProject(
        	projectId,
            projectRoot,
            defaultProjectRequest
        );
    }

    @Override
    public RagProjectIndexResult indexProject(
    	String projectId,
        Path projectRoot,
        RagProjectIndexRequest request
    ) throws RagProjectIndexException {

        Path normalizedRoot =
            requireSupportedProject(projectRoot);

        Objects.requireNonNull(
            request,
            "request must not be null"
        );

        long startedAt =
            System.nanoTime();

        List<Path> documents =
            discoverDocuments(
                normalizedRoot,
                request
            );

        List<RagIndexResult> results =
            indexDocuments(
            	projectId,
                normalizedRoot,
                documents,
                request.getIndexRequest(),
                request.isContinueOnError()
            );

        List<String> removedSourcePaths =
            new ArrayList<>();

        int deletedRecordCount = 0;

        if (request.isDeleteMissingDocuments()) {
            MissingDocumentCleanup cleanup =
                removeMissingDocuments(
                    normalizedRoot,
                    documents
                );

            removedSourcePaths.addAll(
                cleanup.removedSourcePaths
            );

            deletedRecordCount =
                cleanup.deletedRecordCount;
        }

        return createProjectResult(
            normalizedRoot,
            documents,
            results,
            removedSourcePaths,
            deletedRecordCount,
            System.nanoTime() - startedAt
        );
    }

    @Override
    public List<RagIndexResult> indexDocuments(
    	String projectId,	
        Path projectRoot,
        List<Path> relativePaths,
        RagIndexRequest request
    ) throws RagProjectIndexException {

        return indexDocuments(
        	projectId,
            projectRoot,
            relativePaths,
            request,
            request != null
                && request.isContinueOnError()
        );
    }

    /**
     * 문서 목록을 순차적으로 로드하고 인덱싱합니다.
     */
    private List<RagIndexResult> indexDocuments(
    	String projectId,	
        Path projectRoot,
        List<Path> relativePaths,
        RagIndexRequest request,
        boolean continueOnError
    ) throws RagProjectIndexException {

        Path normalizedRoot =
            requireSupportedProject(projectRoot);

        Objects.requireNonNull(
            request,
            "request must not be null"
        );

        List<Path> validatedPaths;

        try {
            validatedPaths =
                validateRelativePaths(
                    normalizedRoot,
                    relativePaths
                );

        } catch (RuntimeException exception) {
            throw new RagProjectIndexException(
                "Invalid project document path",
                RagProjectIndexOperation.VALIDATE,
                normalizePath(normalizedRoot),
                "",
                exception
            );
        }

        if (validatedPaths.isEmpty()) {
            return List.of();
        }

        List<RagIndexResult> results =
            new ArrayList<>(
                validatedPaths.size()
            );

        for (Path relativePath
            : validatedPaths) {

            String sourcePath =
                normalizePath(relativePath);

            if (!supportsDocument(relativePath)) {
                results.add(
                    createSkippedResult(
                        sourcePath,
                        "Unsupported document type"
                    )
                );

                continue;
            }

            try {
                DocumentSource source =
                    loadDocument(
                        normalizedRoot,
                        relativePath
                    );

                if (!ragIndexer.supports(source)) {
                    results.add(
                        createSkippedResult(
                            sourcePath,
                            "DocumentIndexer does not "
                                + "support this source"
                        )
                    );

                    continue;
                }

                results.add(
                    ragIndexer.index(
                    	projectId,
                        source,
                        request
                    )
                );

            } catch (
                RagProjectIndexException exception
            ) {
                if (!continueOnError) {
                    throw exception;
                }

                results.add(
                    RagIndexResult.failed(
                        sourcePath,
                        safeMessage(exception)
                    )
                );

            } catch (RagIndexException exception) {
                if (!continueOnError) {
                    throw new RagProjectIndexException(
                        "Failed to index document: "
                            + sourcePath,
                        RagProjectIndexOperation
                            .INDEX_DOCUMENT,
                        normalizePath(
                            normalizedRoot
                        ),
                        sourcePath,
                        exception
                    );
                }

                results.add(
                    RagIndexResult.failed(
                        sourcePath,
                        safeMessage(exception)
                    )
                );

            } catch (RuntimeException exception) {
                if (!continueOnError) {
                    throw new RagProjectIndexException(
                        "Unexpected error while "
                            + "indexing document: "
                            + sourcePath,
                        RagProjectIndexOperation
                            .INDEX_DOCUMENT,
                        normalizePath(
                            normalizedRoot
                        ),
                        sourcePath,
                        exception
                    );
                }

                results.add(
                    RagIndexResult.failed(
                        sourcePath,
                        safeMessage(exception)
                    )
                );
            }
        }

        return List.copyOf(results);
    }

    @Override
    public List<Path> discoverDocuments(
        Path projectRoot
    ) throws RagProjectIndexException {

        return discoverDocuments(
            projectRoot,
            defaultProjectRequest
        );
    }

    /**
     * 프로젝트 설정을 적용하여 인덱싱 대상 문서를 탐색합니다.
     */
    private List<Path> discoverDocuments(
        Path projectRoot,
        RagProjectIndexRequest request
    ) throws RagProjectIndexException {

        Path normalizedRoot =
            requireSupportedProject(projectRoot);

        Objects.requireNonNull(
            request,
            "request must not be null"
        );

        Set<Path> documents =
            new LinkedHashSet<>();

        List<Path> searchRoots =
            resolveSearchRoots(
                normalizedRoot,
                request
            );

        for (Path searchRoot : searchRoots) {
            if (!Files.isDirectory(
                searchRoot,
                LinkOption.NOFOLLOW_LINKS
            )) {
                continue;
            }

            discoverFromDirectory(
                normalizedRoot,
                searchRoot,
                request,
                documents
            );
        }

        List<Path> result =
            new ArrayList<>(documents);

        if (request.isSortDocuments()) {
            result.sort(
                Comparator.comparing(
                    DefaultRagProjectIndexer
                        ::normalizePath
                )
            );
        }

        return List.copyOf(result);
    }

    /**
     * 지정된 디렉터리에서 지원 문서를 탐색합니다.
     */
    private void discoverFromDirectory(
        Path projectRoot,
        Path searchRoot,
        RagProjectIndexRequest request,
        Set<Path> output
    ) throws RagProjectIndexException {

        int maximumDepth =
            request.isRecursive()
                ? Integer.MAX_VALUE
                : 1;

        try (
            Stream<Path> stream =
                Files.walk(
                    searchRoot,
                    maximumDepth,
                    new FileVisitOption[0]
                )
        ) {
            stream
                .filter(path ->
                    Files.isRegularFile(
                        path,
                        LinkOption.NOFOLLOW_LINKS
                    )
                )
                .filter(path ->
                    request.isIncludeHidden()
                        || !isHiddenPath(
                            projectRoot,
                            path
                        )
                )
                .map(path ->
                    projectRoot
                        .relativize(
                            path.toAbsolutePath()
                                .normalize()
                        )
                        .normalize()
                )
                .filter(path ->
                    isIncludedPath(
                        path,
                        request
                    )
                )
                .filter(path ->
                    !isExcludedPath(
                        path,
                        request
                    )
                )
                .filter(path ->
                    request.supportsExtension(
                        extensionOf(path)
                    )
                )
                .filter(this::supportsDocument)
                .forEach(output::add);

        } catch (IOException exception) {
            throw new RagProjectIndexException(
                "Failed to discover project documents",
                RagProjectIndexOperation
                    .DISCOVER_DOCUMENTS,
                normalizePath(projectRoot),
                normalizeRelativePath(
                    projectRoot,
                    searchRoot
                ),
                exception
            );

        } catch (RuntimeException exception) {
            throw new RagProjectIndexException(
                "Unexpected error while "
                    + "discovering documents",
                RagProjectIndexOperation
                    .DISCOVER_DOCUMENTS,
                normalizePath(projectRoot),
                normalizeRelativePath(
                    projectRoot,
                    searchRoot
                ),
                exception
            );
        }
    }

    @Override
    public RagProjectSyncResult synchronize(
    	String projectId,
        Path projectRoot
    ) throws RagProjectIndexException {

        return synchronize(
        	projectId,
            projectRoot,
            defaultProjectRequest
        );
    }

    /**
     * 현재 프로젝트 문서와 기존 VectorStore 상태를 동기화합니다.
     *
     * <p>문서별 결과 집계는
     * {@link RagProjectSyncResult.Builder#summarizeIndexResults()}
     * 에 위임합니다.</p>
     */
    @Override
    public RagProjectSyncResult synchronize(
    	String projectId,
        Path projectRoot,
        RagProjectIndexRequest request
    ) throws RagProjectIndexException {

        Path normalizedRoot =
            requireSupportedProject(projectRoot);

        Objects.requireNonNull(
            request,
            "request must not be null"
        );

        long startedAt =
            System.nanoTime();

        /*
         * 현재 프로젝트에 존재하는 인덱싱 대상 문서를 탐색합니다.
         */
        List<Path> currentDocuments =
            discoverDocuments(
                normalizedRoot,
                request
            );

        Set<String> currentSourcePaths =
            toNormalizedPathSet(
                currentDocuments
            );

        String model =
            resolveEmbeddingModel(
                normalizedRoot
            );

        /*
         * VectorStore에 현재 임베딩 모델로 저장된 문서 경로를
         * 조회합니다.
         */
        Set<String> indexedSourcePaths =
            loadIndexedSourcePaths(
                model,
                normalizedRoot
            );

        /*
         * 기존 인덱스에는 있지만 현재 프로젝트에는 없는 경로를
         * 누락 문서로 판정합니다.
         */
        Set<String> missingSourcePaths =
            new LinkedHashSet<>(
                indexedSourcePaths
            );

        missingSourcePaths.removeAll(
            currentSourcePaths
        );

        List<String> removedSourcePaths =
            new ArrayList<>();

        int deletedRecordCount = 0;

        /*
         * 설정이 활성화된 경우 누락 문서의 VectorRecord를 제거합니다.
         */
        if (request.isDeleteMissingDocuments()) {
            for (String missingSourcePath
                : missingSourcePaths) {

                int deleted =
                    removeDocumentBySourcePath(
                        normalizedRoot,
                        missingSourcePath,
                        model
                    );

                if (deleted > 0) {
                    deletedRecordCount += deleted;

                    removedSourcePaths.add(
                        missingSourcePath
                    );
                }
            }
        }

        /*
         * 현재 프로젝트의 모든 문서를 증분 인덱싱합니다.
         *
         * 변경되지 않은 Chunk는 DefaultRagIndexer에서
         * contentHash를 기준으로 재사용됩니다.
         */
        List<RagIndexResult> indexResults =
            indexDocuments(
            	projectId,
                normalizedRoot,
                currentDocuments,
                request.getIndexRequest(),
                request.isContinueOnError()
            );

        /*
         * 문서 성공/실패 수와 Chunk 처리 수는
         * summarizeIndexResults()에서 자동 집계합니다.
         */
        return RagProjectSyncResult.builder()
            .projectRoot(
                normalizePath(normalizedRoot)
            )
            .model(model)
            .discoveredDocumentCount(
                currentDocuments.size()
            )
            .previouslyIndexedDocumentCount(
                indexedSourcePaths.size()
            )
            .missingDocumentCount(
                missingSourcePaths.size()
            )
            .removedDocumentCount(
                removedSourcePaths.size()
            )
            .deletedRecordCount(
                deletedRecordCount
            )
            .indexResults(
                indexResults
            )
            .removedSourcePaths(
                removedSourcePaths
            )
            .durationNanos(
                System.nanoTime() - startedAt
            )
            .summarizeIndexResults()
            .build();
    }

    @Override
    public int removeDocument(
        Path projectRoot,
        Path relativePath
    ) throws RagProjectIndexException {

        Path normalizedRoot =
            requireSupportedProject(projectRoot);

        Objects.requireNonNull(
            relativePath,
            "relativePath must not be null"
        );

        List<Path> validated;

        try {
            validated =
                validateRelativePaths(
                    normalizedRoot,
                    List.of(relativePath)
                );

        } catch (RuntimeException exception) {
            throw new RagProjectIndexException(
                "Invalid document path",
                RagProjectIndexOperation
                    .REMOVE_DOCUMENT,
                normalizePath(normalizedRoot),
                normalizePath(relativePath),
                exception
            );
        }

        if (validated.isEmpty()) {
            return 0;
        }

        String sourcePath =
            normalizePath(
                validated.get(0)
            );

        String model =
            resolveEmbeddingModel(
                normalizedRoot
            );

        return removeDocumentBySourcePath(
            normalizedRoot,
            sourcePath,
            model
        );
    }

    @Override
    public int removeProject(
        Path projectRoot
    ) throws RagProjectIndexException {

        Path normalizedRoot =
            requireSupportedProject(projectRoot);

        String model =
            resolveEmbeddingModel(
                normalizedRoot
            );

        Set<String> sourcePaths =
            loadIndexedSourcePaths(
                model,
                normalizedRoot
            );

        int deletedCount = 0;

        for (String sourcePath
            : sourcePaths) {

            deletedCount +=
                removeDocumentBySourcePath(
                    normalizedRoot,
                    sourcePath,
                    model
                );
        }

        return deletedCount;
    }

    @Override
    public boolean supportsProject(
        Path projectRoot
    ) {
        if (projectRoot == null) {
            return false;
        }

        try {
            Path normalized =
                projectRoot
                    .toAbsolutePath()
                    .normalize();

            if (!Files.isDirectory(
                normalized,
                LinkOption.NOFOLLOW_LINKS
            )) {
                return false;
            }

            return Files.isDirectory(
                normalized.resolve("OEBPS"),
                LinkOption.NOFOLLOW_LINKS
            )
                || Files.isDirectory(
                    normalized.resolve("META-INF"),
                    LinkOption.NOFOLLOW_LINKS
                )
                || Files.exists(
                    normalized.resolve("mimetype"),
                    LinkOption.NOFOLLOW_LINKS
                )
                || Files.isDirectory(
                    normalized.resolve("Text"),
                    LinkOption.NOFOLLOW_LINKS
                );

        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public boolean supportsDocument(
        Path relativePath
    ) {
        if (relativePath == null) {
            return false;
        }

        String extension =
            extensionOf(relativePath);

        return "xhtml".equals(extension)
            || "html".equals(extension)
            || "htm".equals(extension);
    }

    @Override
    public boolean isAvailable() {
        try {
            String model =
                embeddingModelProvider.getModel();

            return model != null
                && !model.isBlank()
                && ragIndexer.isAvailable()
                && vectorStore.isAvailable();

        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public RagIndexRequest
        getDefaultIndexRequest() {

        return defaultIndexRequest;
    }

    public RagProjectIndexRequest
        getDefaultProjectRequest() {

        return defaultProjectRequest;
    }

    public DocumentLoader getDocumentLoader() {
        return documentLoader;
    }

    public RagIndexer getRagIndexer() {
        return ragIndexer;
    }

    public VectorStore getVectorStore() {
        return vectorStore;
    }

    public EmbeddingModelProvider
        getEmbeddingModelProvider() {

        return embeddingModelProvider;
    }

    /**
     * 현재 파일 목록에 존재하지 않는 저장소 레코드를 제거합니다.
     */
    private MissingDocumentCleanup
        removeMissingDocuments(
            Path projectRoot,
            List<Path> currentDocuments
        ) throws RagProjectIndexException {

        String model =
            resolveEmbeddingModel(
                projectRoot
            );

        Set<String> currentSourcePaths =
            toNormalizedPathSet(
                currentDocuments
            );

        Set<String> indexedSourcePaths =
            loadIndexedSourcePaths(
                model,
                projectRoot
            );

        indexedSourcePaths.removeAll(
            currentSourcePaths
        );

        List<String> removed =
            new ArrayList<>();

        int deletedRecordCount = 0;

        for (String missingSourcePath
            : indexedSourcePaths) {

            int deleted =
                removeDocumentBySourcePath(
                    projectRoot,
                    missingSourcePath,
                    model
                );

            if (deleted > 0) {
                deletedRecordCount += deleted;
                removed.add(missingSourcePath);
            }
        }

        return new MissingDocumentCleanup(
            List.copyOf(removed),
            deletedRecordCount
        );
    }

    private DocumentSource loadDocument(
        Path projectRoot,
        Path relativePath
    ) throws RagProjectIndexException {

        try {
            DocumentSource source =
                documentLoader.load(
                    projectRoot,
                    relativePath
                );

            if (source == null) {
                throw new RagProjectIndexException(
                    "DocumentLoader returned null source",
                    RagProjectIndexOperation
                        .LOAD_DOCUMENT,
                    normalizePath(projectRoot),
                    normalizePath(relativePath),
                    null
                );
            }

            return source;

        } catch (
            RagProjectIndexException exception
        ) {
            throw exception;

        } catch (Exception exception) {
            throw new RagProjectIndexException(
                "Failed to load document: "
                    + normalizePath(relativePath),
                RagProjectIndexOperation
                    .LOAD_DOCUMENT,
                normalizePath(projectRoot),
                normalizePath(relativePath),
                exception
            );
        }
    }

    /**
     * VectorStore에서 현재 모델의 원본 경로를 추출합니다.
     */
    private Set<String> loadIndexedSourcePaths(
        String model,
        Path projectRoot
    ) throws RagProjectIndexException {

        try {
            List<VectorRecord> records =
                vectorStore.findByModel(
                    model
                );

            Set<String> sourcePaths =
                new LinkedHashSet<>();

            for (VectorRecord record
                : records) {

                if (record == null
                    || record.getChunk() == null) {

                    continue;
                }

                String sourcePath =
                    normalizeStoredSourcePath(
                        projectRoot,
                        record.getChunk()
                            .getSourcePath()
                    );

                if (!sourcePath.isBlank()) {
                    sourcePaths.add(
                        sourcePath
                    );
                }
            }

            return sourcePaths;

        } catch (
            VectorStoreException exception
        ) {
            throw new RagProjectIndexException(
                "Failed to load indexed "
                    + "project documents",
                RagProjectIndexOperation.READ_INDEX,
                normalizePath(projectRoot),
                "",
                exception
            );
        }
    }

    private int removeDocumentBySourcePath(
        Path projectRoot,
        String sourcePath,
        String model
    ) throws RagProjectIndexException {

        try {
            return ragIndexer.remove(
                sourcePath,
                model
            );

        } catch (RagIndexException exception) {
            throw new RagProjectIndexException(
                "Failed to remove document index: "
                    + sourcePath,
                RagProjectIndexOperation
                    .REMOVE_DOCUMENT,
                normalizePath(projectRoot),
                sourcePath,
                exception
            );
        }
    }

    private String resolveEmbeddingModel(
        Path projectRoot
    ) throws RagProjectIndexException {

        String model;

        try {
            model =
                embeddingModelProvider.getModel();

        } catch (RuntimeException exception) {
            throw new RagProjectIndexException(
                "Failed to resolve embedding model",
                RagProjectIndexOperation.VALIDATE,
                normalizePath(projectRoot),
                "",
                exception
            );
        }

        if (model == null
            || model.isBlank()) {

            throw new RagProjectIndexException(
                "Embedding model must not be blank",
                RagProjectIndexOperation.VALIDATE,
                normalizePath(projectRoot),
                "",
                null
            );
        }

        return model.trim();
    }

    /**
     * 프로젝트 전체 인덱싱 결과를 생성합니다.
     */
    private RagProjectIndexResult
        createProjectResult(
            Path projectRoot,
            List<Path> discoveredDocuments,
            List<RagIndexResult> results,
            List<String> removedSourcePaths,
            int deletedRecordCount,
            long durationNanos
        ) {

        return RagProjectIndexResult.builder()
            .projectRoot(
                normalizePath(projectRoot)
            )
            .model(
                safeModelName()
            )
            .discoveredDocumentCount(
                discoveredDocuments.size()
            )
            .documentResults(results)
            .removedDocumentCount(
                removedSourcePaths.size()
            )
            .deletedRecordCount(
                deletedRecordCount
            )
            .removedSourcePaths(
                removedSourcePaths
            )
            .durationNanos(
                durationNanos
            )
            .summarizeDocumentResults()
            .build();
    }

    /**
     * 지원 대상이 아닌 파일의 건너뜀 결과를 생성합니다.
     */
    private RagIndexResult createSkippedResult(
        String sourcePath,
        String message
    ) {
        return RagIndexResult.builder()
            .sourcePath(sourcePath)
            .model(safeModelName())
            .totalChunkCount(0)
            .indexedCount(0)
            .reusedCount(0)
            .skippedCount(1)
            .deletedCount(0)
            .failedCount(0)
            .durationNanos(0L)
            .success(true)
            .issue(
                RagIndexIssue.warning(
                    message
                )
            )
            .build();
    }

    private String safeModelName() {
        try {
            String model =
                embeddingModelProvider.getModel();

            return model == null
                ? ""
                : model.trim();

        } catch (RuntimeException exception) {
            return "";
        }
    }

    /**
     * includeDirectory 설정을 실제 탐색 시작 경로로 변환합니다.
     */
    private List<Path> resolveSearchRoots(
        Path projectRoot,
        RagProjectIndexRequest request
    ) throws RagProjectIndexException {

        if (!request.hasIncludeDirectories()) {
            return List.of(projectRoot);
        }

        List<Path> roots =
            new ArrayList<>();

        for (String directory
            : request.getIncludeDirectories()) {

            Path relative =
                Path.of(directory)
                    .normalize();

            if (relative.isAbsolute()) {
                throw new RagProjectIndexException(
                    "Include directory must "
                        + "be relative: "
                        + directory,
                    RagProjectIndexOperation.VALIDATE,
                    normalizePath(projectRoot),
                    directory,
                    null
                );
            }

            Path resolved =
                projectRoot
                    .resolve(relative)
                    .normalize();

            if (!resolved.startsWith(
                projectRoot
            )) {
                throw new RagProjectIndexException(
                    "Include directory escapes "
                        + "project root: "
                        + directory,
                    RagProjectIndexOperation.VALIDATE,
                    normalizePath(projectRoot),
                    directory,
                    null
                );
            }

            roots.add(resolved);
        }

        return List.copyOf(roots);
    }

    private boolean isIncludedPath(
        Path relativePath,
        RagProjectIndexRequest request
    ) {
        if (!request.hasIncludeDirectories()) {
            return true;
        }

        String path =
            normalizePath(relativePath);

        for (String directory
            : request.getIncludeDirectories()) {

            if (isPathWithin(
                path,
                normalizeTextPath(directory)
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean isExcludedPath(
        Path relativePath,
        RagProjectIndexRequest request
    ) {
        if (!request.hasExcludeDirectories()) {
            return false;
        }

        String path =
            normalizePath(relativePath);

        for (String directory
            : request.getExcludeDirectories()) {

            if (isPathWithin(
                path,
                normalizeTextPath(directory)
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPathWithin(
        String path,
        String directory
    ) {
        if (directory.isBlank()) {
            return false;
        }

        return path.equals(directory)
            || path.startsWith(
                directory + "/"
            );
    }

    private static boolean isHiddenPath(
        Path projectRoot,
        Path candidate
    ) {
        Path relative =
            projectRoot
                .relativize(candidate)
                .normalize();

        Path current =
            projectRoot;

        for (Path segment : relative) {
            String name =
                segment.toString();

            if (name.startsWith(".")) {
                return true;
            }

            current =
                current.resolve(segment);

            try {
                if (Files.isHidden(current)) {
                    return true;
                }

            } catch (IOException ignored) {
                /*
                 * 숨김 여부 확인에 실패하면 파일명 규칙만
                 * 적용합니다.
                 */
            }
        }

        return false;
    }

    private Path requireSupportedProject(
        Path projectRoot
    ) throws RagProjectIndexException {

        Path normalized;

        try {
            normalized =
                validateProjectRoot(
                    projectRoot
                );

        } catch (RuntimeException exception) {
            throw new RagProjectIndexException(
                "Invalid project root",
                RagProjectIndexOperation.VALIDATE,
                normalizePath(projectRoot),
                "",
                exception
            );
        }

        if (!supportsProject(normalized)) {
            throw new RagProjectIndexException(
                "Unsupported or invalid EPUB project: "
                    + normalizePath(normalized),
                RagProjectIndexOperation.VALIDATE,
                normalizePath(normalized),
                "",
                null
            );
        }

        return normalized;
    }

    private static Set<String>
        toNormalizedPathSet(
            Collection<Path> paths
        ) {

        Set<String> result =
            new LinkedHashSet<>();

        if (paths == null) {
            return result;
        }

        for (Path path : paths) {
            if (path != null) {
                result.add(
                    normalizePath(path)
                );
            }
        }

        return result;
    }

    /**
     * 저장소 경로가 절대 경로인 경우 프로젝트 상대 경로로 변환합니다.
     */
    private static String
        normalizeStoredSourcePath(
            Path projectRoot,
            String sourcePath
        ) {

        if (sourcePath == null
            || sourcePath.isBlank()) {

            return "";
        }

        Path path;

        try {
            path =
                Path.of(sourcePath)
                    .normalize();

        } catch (RuntimeException exception) {
            return normalizeTextPath(
                sourcePath
            );
        }

        if (!path.isAbsolute()) {
            return normalizePath(path);
        }

        Path normalizedAbsolute =
            path.toAbsolutePath()
                .normalize();

        if (!normalizedAbsolute.startsWith(
            projectRoot
        )) {
            return "";
        }

        return normalizePath(
            projectRoot.relativize(
                normalizedAbsolute
            )
        );
    }

    private static String extensionOf(
        Path path
    ) {
        if (path == null
            || path.getFileName() == null) {

            return "";
        }

        String fileName =
            path.getFileName()
                .toString();

        int separator =
            fileName.lastIndexOf('.');

        if (separator < 0
            || separator
                == fileName.length() - 1) {

            return "";
        }

        return fileName
            .substring(separator + 1)
            .toLowerCase(
                Locale.ROOT
            );
    }

    private static String normalizeTextPath(
        String path
    ) {
        if (path == null) {
            return "";
        }

        String normalized =
            path.trim()
                .replace('\\', '/');

        while (normalized.startsWith("./")) {
            normalized =
                normalized.substring(2);
        }

        while (normalized.endsWith("/")
            && normalized.length() > 1) {

            normalized =
                normalized.substring(
                    0,
                    normalized.length() - 1
                );
        }

        return normalized;
    }

    private static String normalizePath(
        Path path
    ) {
        if (path == null) {
            return "";
        }

        return path
            .normalize()
            .toString()
            .replace('\\', '/');
    }

    private static String normalizeRelativePath(
        Path projectRoot,
        Path path
    ) {
        if (projectRoot == null
            || path == null) {

            return "";
        }

        try {
            Path normalizedRoot =
                projectRoot
                    .toAbsolutePath()
                    .normalize();

            Path normalizedPath =
                path
                    .toAbsolutePath()
                    .normalize();

            if (normalizedPath.startsWith(
                normalizedRoot
            )) {
                return normalizePath(
                    normalizedRoot.relativize(
                        normalizedPath
                    )
                );
            }

            return normalizePath(
                normalizedPath
            );

        } catch (RuntimeException exception) {
            return normalizePath(path);
        }
    }

    private static String safeMessage(
        Throwable throwable
    ) {
        if (throwable == null) {
            return "Unknown project indexing error";
        }

        String message =
            throwable.getMessage();

        if (message == null
            || message.isBlank()) {

            return throwable
                .getClass()
                .getSimpleName();
        }

        return message.trim();
    }

    @Override
    public String toString() {
        return "DefaultRagProjectIndexer{" +
            "documentLoader="
                + documentLoader
                    .getClass()
                    .getSimpleName() +
            ", ragIndexer="
                + ragIndexer
                    .getClass()
                    .getSimpleName() +
            ", vectorStore="
                + vectorStore
                    .getClass()
                    .getSimpleName() +
            ", embeddingModelProvider="
                + embeddingModelProvider
                    .getClass()
                    .getSimpleName() +
            ", defaultProjectRequest="
                + defaultProjectRequest +
            '}';
    }

    /**
     * 누락 문서 정리 결과입니다.
     */
    private static final class
        MissingDocumentCleanup {

        private final List<String>
            removedSourcePaths;

        private final int
            deletedRecordCount;

        private MissingDocumentCleanup(
            List<String> removedSourcePaths,
            int deletedRecordCount
        ) {
            this.removedSourcePaths =
                List.copyOf(
                    removedSourcePaths
                );

            this.deletedRecordCount =
                deletedRecordCount;
        }
    }
}