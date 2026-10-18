/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.index;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import kr.co.goms.ai.project.EpubProjectContext;
import kr.co.goms.ai.rag.document.DocumentLoader;
import kr.co.goms.ai.rag.embedding.EmbeddingModelProvider;
import kr.co.goms.ai.rag.expansion.ChunkContextProvider;
import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.model.DocumentSource;
import kr.co.goms.ai.rag.util.RagUtil;
import kr.co.goms.ai.rag.vector.VectorRecord;
import kr.co.goms.ai.rag.vector.VectorStore;
import kr.co.goms.ai.rag.vector.VectorStoreException;

/**
 * 기본 {@link ProjectRagIndexer} 구현체입니다.
 *
 * <p>현재 EPUB 프로젝트의 TEXT 디렉터리를 검사하고 VectorStore 상태를
 * 실제 프로젝트 파일 상태와 증분 방식으로 동기화합니다.</p>
 *
 * <pre>
 * EpubProjectContext
 *        ↓
 * DefaultProjectRagIndexer
 *        │
 *        ├─ XHTML 검색
 *        ├─ 제외 파일 처리
 *        ├─ NEW / CHANGED / UNCHANGED / DELETED 판단
 *        ├─ Progress 이벤트
 *        └─ ChunkContext 동기화
 *                │
 *                ↓
 *             RagIndexer
 *                │
 *                ↓
 *        DefaultRagIndexer
 *        ├─ Chunk
 *        ├─ Hash
 *        ├─ Embedding
 *        ├─ VectorRecord
 *        └─ VectorStore
 * </pre>
 *
 * <h2>책임 분리</h2>
 *
 * <p>이 클래스는 프로젝트 수준의 Orchestration만 담당합니다.</p>
 *
 * <p>Chunk 생성, Hash 비교, Embedding 생성, VectorRecord 생성,
 * Chunk 증분 재사용 및 Vector 저장은 {@link RagIndexer}에 위임합니다.</p>
 *
 * <h2>Project ID 정책</h2>
 *
 * <p>projectId는 프로젝트 경로의 SHA-256 값이 아니라 프로젝트 디렉터리명을
 * 사용하는 GomsBook 논리 프로젝트 ID입니다.</p>
 *
 * <pre>
 * C:/1004.GomsBook/03.Project/lunchwork_seoul
 *                              ↓
 * projectId = lunchwork_seoul
 * </pre>
 *
 * <p>RagRuntime, RagIndexer, Retriever, VectorStore 및 Qdrant Payload가
 * 모두 동일한 projectId를 사용해야 합니다.</p>
 *
 * <h2>곰스북 1줄 원칙</h2>
 *
 * <p>메서드 선언, 단순 조건문, 변수 선언, return 및 단순 호출은 가능한 한
 * 한 줄로 작성합니다. Stream, Builder 및 복합 처리만 의미 단위로 나눕니다.</p>
 */
public final class DefaultProjectRagIndexer implements ProjectRagIndexer {

    /**
     * 기본적으로 RAG 인덱싱에서 제외하는 XHTML 파일입니다.
     */
    private static final Set<String> DEFAULT_EXCLUDED_FILES = Set.of("quiz.xhtml");

    /**
     * 프로젝트 파일을 DocumentSource로 로드합니다.
     */
    private final DocumentLoader documentLoader;

    /**
     * 실제 문서 Chunk/Hash/Embedding/Vector 인덱싱을 담당합니다.
     */
    private final RagIndexer ragIndexer;

    /**
     * 기존 프로젝트 Vector 상태 확인 및 Context 복원에 사용합니다.
     */
    private final VectorStore vectorStore;

    /**
     * Context Expansion용 Chunk 상태를 관리합니다.
     */
    private final ChunkContextProvider chunkContextProvider;

    /**
     * 현재 사용할 Embedding Model을 제공합니다.
     */
    private final EmbeddingModelProvider embeddingModelProvider;

    /**
     * 프로젝트 동기화 시 사용할 기본 문서 인덱싱 설정입니다.
     */
    private final RagIndexRequest defaultIndexRequest;

    /**
     * 프로젝트 RAG 인덱서를 생성합니다.
     *
     * @param documentLoader 프로젝트 문서 로더
     * @param ragIndexer 실제 문서 RAG 인덱서
     * @param vectorStore Vector 저장소
     * @param chunkContextProvider Context Expansion Chunk 저장소
     * @param embeddingModelProvider Embedding Model 제공자
     * @param defaultIndexRequest 기본 문서 인덱싱 설정
     */
    public DefaultProjectRagIndexer(DocumentLoader documentLoader, RagIndexer ragIndexer, VectorStore vectorStore, ChunkContextProvider chunkContextProvider, EmbeddingModelProvider embeddingModelProvider, RagIndexRequest defaultIndexRequest) {

        this.documentLoader = Objects.requireNonNull(documentLoader, "documentLoader must not be null");
        this.ragIndexer = Objects.requireNonNull(ragIndexer, "ragIndexer must not be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
        this.chunkContextProvider = Objects.requireNonNull(chunkContextProvider, "chunkContextProvider must not be null");
        this.embeddingModelProvider = Objects.requireNonNull(embeddingModelProvider, "embeddingModelProvider must not be null");
        this.defaultIndexRequest = Objects.requireNonNull(defaultIndexRequest, "defaultIndexRequest must not be null");
    }

    /**
     * 기본 RagIndexRequest를 사용하는 편의 생성자입니다.
     */
    public DefaultProjectRagIndexer(DocumentLoader documentLoader, RagIndexer ragIndexer, VectorStore vectorStore, ChunkContextProvider chunkContextProvider, EmbeddingModelProvider embeddingModelProvider) {

        this(documentLoader, ragIndexer, vectorStore, chunkContextProvider, embeddingModelProvider, RagIndexRequest.defaults());
    }

    /**
     * 기본 옵션으로 프로젝트 RAG 인덱스를 동기화합니다.
     */
    @Override
    public ProjectIndexResult synchronize(EpubProjectContext project) throws ProjectIndexException {

        return synchronize(project, ProjectIndexOptions.defaults(), ProjectIndexProgressListener.noop());
    }

    /**
     * 지정된 프로젝트 옵션으로 RAG 인덱스를 동기화합니다.
     */
    @Override
    public ProjectIndexResult synchronize(EpubProjectContext project, ProjectIndexOptions options) throws ProjectIndexException {

        return synchronize(project, options, ProjectIndexProgressListener.noop());
    }

    /**
     * 프로젝트 옵션과 Progress Listener를 적용하여 RAG 인덱스를 동기화합니다.
     *
     * <p>실제 구현은 이 메서드 하나에만 존재합니다.</p>
     */
    @Override
    public ProjectIndexResult synchronize(EpubProjectContext project, ProjectIndexOptions options, ProjectIndexProgressListener progressListener) throws ProjectIndexException {

        validateProject(project);

        ProjectIndexOptions resolvedOptions = options == null ? ProjectIndexOptions.defaults() : options;
        ProjectIndexProgressListener listener = progressListener == null ? ProjectIndexProgressListener.noop() : progressListener;

        String projectId = null;
        int totalFiles = 0;
        int currentFile = 0;

        try {

            Path projectRoot = requireProjectRoot(project);
            Path textDirectory = requireTextDirectory(project);
            String embeddingModel = resolveEmbeddingModel();
            projectId = resolveProjectId(projectRoot);

            validateTextDirectory(textDirectory);

            notifyProgress(listener, projectId, ProjectIndexProgressStage.STARTED, 0, 0, 0, null, "RAG 인덱싱을 시작합니다.");

            /*
             * 애플리케이션 재기동 등으로 ChunkContextProvider가 비어 있을 수 있으므로
             * 기존 VectorStore의 실제 프로젝트 Chunk를 먼저 복원합니다.
             */
            synchronizeChunkContext(projectId, embeddingModel);

            notifyProgress(listener, projectId, ProjectIndexProgressStage.SCANNING, 0, 0, 0, null, "EPUB XHTML 문서를 검색하고 있습니다.");

            Set<String> excludedFiles = createExcludedFiles(resolvedOptions);
            List<Path> xhtmlFiles = findXhtmlFiles(textDirectory, projectRoot, excludedFiles);

            totalFiles = xhtmlFiles.size();

            notifyProgress(listener, projectId, ProjectIndexProgressStage.SCANNING, 0, totalFiles, 0, null, totalFiles + "개의 XHTML 문서를 확인했습니다.");

            ProjectIndexState state = new ProjectIndexState();

            String textSourcePrefix = createTextSourcePrefix(projectRoot, textDirectory);
            Set<String> currentSourcePaths = createCurrentSourcePaths(projectRoot, xhtmlFiles);

            /*
             * 1. DELETED
             *
             * 실제 파일이 삭제되었거나 제외 정책 때문에 현재 프로젝트 대상에서
             * 사라진 문서를 projectId + model 범위에서 제거합니다.
             */
            cleanupDeletedDocuments(projectId, embeddingModel, textSourcePrefix, currentSourcePaths, totalFiles, listener, state);

            /*
             * 2. NEW / CHANGED / UNCHANGED
             */
            for (Path xhtmlFile : xhtmlFiles) {

                currentFile++;

                Path relativePath = projectRoot.relativize(xhtmlFile);
                String sourcePath = normalizePath(relativePath);
                int percent = calculatePercent(currentFile, totalFiles);

                notifyProgress(listener, projectId, ProjectIndexProgressStage.INDEXING, currentFile, totalFiles, percent, sourcePath, sourcePath + " 문서를 처리하고 있습니다.");

                DocumentSource source = loadDocument(projectRoot, relativePath, sourcePath);

                if (!ragIndexer.supports(source)) {

                    System.out.println("[RAG][SKIP] Unsupported source: " + sourcePath);

                    continue;
                }

                List<VectorRecord> existingRecords = loadSourceRecords(projectId, sourcePath, embeddingModel);
                boolean existedBefore = !existingRecords.isEmpty();

                notifyProgress(listener, projectId, ProjectIndexProgressStage.EMBEDDING, currentFile, totalFiles, percent, sourcePath, sourcePath + " 문서의 Vector 인덱스를 확인하고 있습니다.");

                RagIndexResult indexResult = indexDocument(projectId, source, sourcePath);

                state.processedFiles++;
                state.createdEmbeddings += indexResult.getIndexedCount();
                state.storedVectors += indexResult.getIndexedCount();
                state.deletedVectors += indexResult.getDeletedCount();

                /*
                 * 실제 Chunk 생성이 발생한 문서만 createdChunks에 반영합니다.
                 *
                 * 기존 Vector가 전부 재사용된 UNCHANGED 문서는
                 * totalChunkCount가 존재하더라도 새 Chunk 생성 통계에서는 제외합니다.
                 */
                if (indexResult.getIndexedCount() > 0 || indexResult.getDeletedCount() > 0) state.createdChunks += indexResult.getTotalChunkCount();

                if (isUnchangedResult(indexResult, existedBefore)) {

                    state.skippedFiles++;
                    state.skippedFilePaths.add(sourcePath);

                    System.out.println("[RAG][SKIP] " + sourcePath + " - unchanged, reusedVectors=" + indexResult.getReusedCount());

                } else if (existedBefore) {

                    state.reindexedFiles++;
                    state.indexedFiles.add(sourcePath);

                    System.out.println("[RAG][REINDEX] " + sourcePath + ", indexedVectors=" + indexResult.getIndexedCount() + ", reusedVectors=" + indexResult.getReusedCount() + ", deletedVectors=" + indexResult.getDeletedCount());

                } else {

                    state.newFiles++;
                    state.indexedFiles.add(sourcePath);

                    System.out.println("[RAG][NEW] " + sourcePath + ", indexedVectors=" + indexResult.getIndexedCount());
                }

                /*
                 * VectorStore에 실제 저장되어 있는 Chunk를 기준으로
                 * ContextProvider를 다시 맞춥니다.
                 *
                 * Embedding 대상에서 제외된 빈 Chunk 등이 ContextProvider에만
                 * 남는 문제를 방지합니다.
                 */
                synchronizeSourceChunkContext(projectId, sourcePath, embeddingModel);
            }

            long vectorStoreSize = countProjectVectors(projectId, embeddingModel);

            ProjectIndexResult result = ProjectIndexResult.builder()
                .projectId(projectId)
                .projectName(project.getProjectName())
                .textDirectory(textDirectory.toString())
                .embeddingModel(embeddingModel)
                .discoveredFiles(xhtmlFiles.size())
                .processedFiles(state.processedFiles)
                .newFiles(state.newFiles)
                .reindexedFiles(state.reindexedFiles)
                .skippedFiles(state.skippedFiles)
                .deletedFiles(state.deletedFiles)
                .createdChunks(state.createdChunks)
                .createdEmbeddings(state.createdEmbeddings)
                .storedVectors(state.storedVectors)
                .deletedVectors(state.deletedVectors)
                .vectorStoreSize(vectorStoreSize)
                .indexedFiles(state.indexedFiles)
                .skippedFilePaths(state.skippedFilePaths)
                .deletedFilePaths(state.deletedFilePaths)
                .build();

            notifyProgress(listener, projectId, ProjectIndexProgressStage.COMPLETED, totalFiles, totalFiles, 100, null, "RAG 인덱싱이 완료되었습니다.");

            return result;

        } catch (Exception exception) {

            notifyProgress(listener, projectId, ProjectIndexProgressStage.FAILED, currentFile, totalFiles, calculatePercent(currentFile, totalFiles), null, "RAG 인덱싱에 실패했습니다: " + safeMessage(exception));

            if (exception instanceof ProjectIndexException projectIndexException) throw projectIndexException;

            throw new ProjectIndexException("Failed to synchronize EPUB project RAG index: " + safeMessage(exception), exception);
        }
    }

    /**
     * 프로젝트 파일 목록에서 제거된 기존 Vector 문서를 정리합니다.
     */
    private void cleanupDeletedDocuments(String projectId, String embeddingModel, String textSourcePrefix, Set<String> currentSourcePaths, int totalFiles, ProjectIndexProgressListener listener, ProjectIndexState state) throws RagIndexException, VectorStoreException {

        List<VectorRecord> projectRecords = vectorStore.findByProjectAndModel(projectId, embeddingModel);
        Set<String> storedSourcePaths = new LinkedHashSet<>();

        for (VectorRecord record : projectRecords) {

            if (record == null || record.getChunk() == null) continue;

            String sourcePath = RagUtil.normalizeDocumentPath(record.getChunk().getSourcePath());

            if (sourcePath == null || sourcePath.isBlank()) continue;
            if (!sourcePath.startsWith(textSourcePrefix)) continue;

            storedSourcePaths.add(sourcePath);
        }

        for (String storedSourcePath : storedSourcePaths) {

            if (currentSourcePaths.contains(storedSourcePath)) continue;

            notifyProgress(listener, projectId, ProjectIndexProgressStage.DELETING, 0, totalFiles, 0, storedSourcePath, storedSourcePath + " 기존 인덱스를 제거하고 있습니다.");

            int deleted = ragIndexer.remove(projectId, storedSourcePath, embeddingModel);

            /*
             * Vector가 실제로 존재하지 않는 경우에도 Context의 stale Chunk는
             * 제거하는 편이 안전합니다.
             */
            chunkContextProvider.removeBySource(projectId, storedSourcePath);

            if (deleted <= 0) continue;

            state.deletedFiles++;
            state.deletedVectors += deleted;
            state.deletedFilePaths.add(storedSourcePath);

            System.out.println("[RAG][DELETE] " + storedSourcePath + ", deletedVectors=" + deleted);
        }
    }

    /**
     * 실제 문서 인덱싱을 RagIndexer에 위임합니다.
     */
    private RagIndexResult indexDocument(String projectId, DocumentSource source, String sourcePath) throws ProjectIndexException {

        try {

            return ragIndexer.index(projectId, source, defaultIndexRequest);

        } catch (RagIndexException exception) {

            throw new ProjectIndexException("Failed to index document: " + sourcePath, exception);
        }
    }

    /**
     * 프로젝트 문서를 로드합니다.
     */
    private DocumentSource loadDocument(Path projectRoot, Path relativePath, String sourcePath) throws ProjectIndexException {

        try {

            return documentLoader.load(projectRoot, relativePath);

        } catch (Exception exception) {

            throw new ProjectIndexException("Failed to load project document: " + sourcePath, exception);
        }
    }

    /**
     * 특정 프로젝트 문서의 기존 VectorRecord를 조회합니다.
     */
    private List<VectorRecord> loadSourceRecords(String projectId, String sourcePath, String embeddingModel) throws VectorStoreException {

        return vectorStore.findByProjectAndSourcePath(projectId, sourcePath).stream()
            .filter(Objects::nonNull)
            .filter(record -> record.isModel(embeddingModel))
            .toList();
    }

    /**
     * RagIndexer 결과가 전체 재사용 상태인지 확인합니다.
     */
    private boolean isUnchangedResult(RagIndexResult result, boolean existedBefore) {

        if (!existedBefore) return false;
        if (result == null) return false;
        if (result.getFailedCount() > 0) return false;
        if (result.getIndexedCount() > 0) return false;
        if (result.getDeletedCount() > 0) return false;

        return result.getReusedCount() > 0;
    }

    /**
     * 기존 VectorStore 전체 프로젝트 Chunk를 ContextProvider에 복원합니다.
     */
    private void synchronizeChunkContext(String projectId, String embeddingModel) throws VectorStoreException {

        List<VectorRecord> records = vectorStore.findByProjectAndModel(projectId, embeddingModel);

        if (records == null || records.isEmpty()) {

            chunkContextProvider.removeProject(projectId);

            return;
        }

        List<DocumentChunk> chunks = records.stream()
            .filter(Objects::nonNull)
            .map(VectorRecord::getChunk)
            .filter(Objects::nonNull)
            .toList();

        chunkContextProvider.replaceProjectChunks(projectId, chunks);

        System.out.println("[RAG][CONTEXT] projectId=" + projectId + ", chunks=" + chunks.size());
    }

    /**
     * 특정 Source의 실제 VectorStore Chunk 상태를 ContextProvider에 반영합니다.
     */
    private void synchronizeSourceChunkContext(String projectId, String sourcePath, String embeddingModel) throws VectorStoreException {

        List<VectorRecord> records = loadSourceRecords(projectId, sourcePath, embeddingModel);

        chunkContextProvider.removeBySource(projectId, sourcePath);

        if (records.isEmpty()) return;

        List<DocumentChunk> chunks = records.stream()
            .map(VectorRecord::getChunk)
            .filter(Objects::nonNull)
            .toList();

        if (!chunks.isEmpty()) chunkContextProvider.addChunks(projectId, chunks);
    }

    /**
     * 현재 프로젝트 + Embedding Model 범위의 Vector 수를 반환합니다.
     *
     * <p>{@link VectorStore#count()}를 사용하지 않는 이유는 Qdrant Collection에
     * 다른 프로젝트 Vector가 함께 저장될 수 있기 때문입니다.</p>
     */
    private long countProjectVectors(String projectId, String embeddingModel) throws VectorStoreException {

        return vectorStore.findByProjectAndModel(projectId, embeddingModel).size();
    }

    /**
     * 프로젝트에서 사용할 Embedding Model을 반환합니다.
     */
    private String resolveEmbeddingModel() {

        String model = embeddingModelProvider.getModel();

        if (model == null || model.isBlank()) throw new IllegalStateException("Embedding model must not be blank");

        return model.trim();
    }

    /**
     * 프로젝트 디렉터리명으로 GomsBook 논리 projectId를 생성합니다.
     */
    private String resolveProjectId(Path projectRoot) {

        Path normalizedRoot = Objects.requireNonNull(projectRoot, "projectRoot must not be null").toAbsolutePath().normalize();
        Path fileName = normalizedRoot.getFileName();

        if (fileName == null) throw new IllegalArgumentException("Unable to resolve projectId from projectRoot: " + normalizedRoot);

        return RagUtil.requireProjectId(fileName.toString());
    }

    /**
     * 기본 제외 파일과 ProjectIndexOptions의 제외 파일을 통합합니다.
     */
    private Set<String> createExcludedFiles(ProjectIndexOptions options) {

        Set<String> excludedFiles = new LinkedHashSet<>();

        for (String file : DEFAULT_EXCLUDED_FILES) addExcludedFile(excludedFiles, file);

        if (options != null) {

            for (String file : options.getExcludeFiles()) addExcludedFile(excludedFiles, file);
        }

        return Set.copyOf(excludedFiles);
    }

    /**
     * 제외 파일을 정규화하여 Set에 추가합니다.
     */
    private void addExcludedFile(Set<String> excludedFiles, String file) {

        if (excludedFiles == null || file == null || file.isBlank()) return;

        String normalized = RagUtil.normalizeDocumentPath(file.trim());

        if (normalized == null || normalized.isBlank()) return;

        excludedFiles.add(normalized.toLowerCase(Locale.ROOT));
    }

    /**
     * TEXT 디렉터리 아래의 인덱싱 대상 XHTML 문서를 검색합니다.
     */
    private List<Path> findXhtmlFiles(Path textDirectory, Path projectRoot, Set<String> excludedFiles) {

        try (Stream<Path> stream = Files.walk(textDirectory)) {

            return stream
                .filter(Files::isRegularFile)
                .filter(this::isXhtmlFile)
                .filter(path -> !isExcluded(path, projectRoot, excludedFiles))
                .sorted(Comparator.comparing(path -> path.toAbsolutePath().normalize().toString()))
                .toList();

        } catch (Exception exception) {

            throw new IllegalStateException("Failed to scan TEXT directory: " + textDirectory, exception);
        }
    }

    /**
     * 문서가 제외 대상인지 확인합니다.
     */
    private boolean isExcluded(Path path, Path projectRoot, Set<String> excludedFiles) {

        if (path == null || path.getFileName() == null) return false;
        if (excludedFiles == null || excludedFiles.isEmpty()) return false;

        String fileName = path.getFileName().toString().trim().toLowerCase(Locale.ROOT);
        String relativePath = normalizePath(projectRoot.relativize(path.toAbsolutePath().normalize())).toLowerCase(Locale.ROOT);

        return excludedFiles.contains(fileName) || excludedFiles.contains(relativePath);
    }

    /**
     * TEXT 디렉터리의 프로젝트 상대 Source Prefix를 생성합니다.
     */
    private String createTextSourcePrefix(Path projectRoot, Path textDirectory) {

        String prefix = normalizePath(projectRoot.relativize(textDirectory));

        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    /**
     * 현재 인덱싱 대상 Source 경로 목록을 생성합니다.
     */
    private Set<String> createCurrentSourcePaths(Path projectRoot, List<Path> xhtmlFiles) {

        Set<String> result = new LinkedHashSet<>();

        for (Path xhtmlFile : xhtmlFiles) {

            if (xhtmlFile == null) continue;

            result.add(normalizePath(projectRoot.relativize(xhtmlFile)));
        }

        return Set.copyOf(result);
    }

    /**
     * XHTML 파일인지 확인합니다.
     */
    private boolean isXhtmlFile(Path path) {

        if (path == null || path.getFileName() == null) return false;

        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);

        return fileName.endsWith(".xhtml");
    }

    /**
     * 프로젝트 객체를 검증합니다.
     */
    private void validateProject(EpubProjectContext project) throws ProjectIndexException {

        if (project == null) throw new ProjectIndexException("EPUB project must not be null.");
    }

    /**
     * 프로젝트 Root를 반환합니다.
     */
    private Path requireProjectRoot(EpubProjectContext project) {

        return Objects.requireNonNull(project.getProjectRoot(), "projectRoot must not be null")
            .toAbsolutePath()
            .normalize();
    }

    /**
     * 프로젝트 TEXT 디렉터리를 반환합니다.
     */
    private Path requireTextDirectory(EpubProjectContext project) {

        return Objects.requireNonNull(project.getTextDirectory(), "textDirectory must not be null")
            .toAbsolutePath()
            .normalize();
    }

    /**
     * TEXT 디렉터리를 검증합니다.
     */
    private void validateTextDirectory(Path textDirectory) {

        if (!Files.exists(textDirectory)) throw new IllegalStateException("TEXT directory does not exist: " + textDirectory);
        if (!Files.isDirectory(textDirectory)) throw new IllegalStateException("TEXT path is not a directory: " + textDirectory);
    }

    /**
     * 프로젝트 상대 Path를 RAG 공통 Source 경로로 변환합니다.
     */
    private String normalizePath(Path path) {

        if (path == null) return "";

        String normalized = RagUtil.normalizeDocumentPath(path.normalize().toString());

        return normalized == null ? "" : normalized;
    }

    /**
     * Progress Listener에 진행 상태를 전달합니다.
     *
     * <p>Listener 실패가 실제 인덱싱을 실패시키지 않도록 예외는 격리합니다.</p>
     */
    private void notifyProgress(ProjectIndexProgressListener listener, String projectId, ProjectIndexProgressStage stage, int current, int total, int percent, String sourcePath, String message) {

        if (listener == null) return;

        try {

            listener.onProgress(
                ProjectIndexProgress.builder()
                    .projectId(projectId)
                    .stage(stage)
                    .current(current)
                    .total(total)
                    .percent(percent)
                    .sourcePath(sourcePath)
                    .message(message)
                    .build()
            );

        } catch (RuntimeException exception) {

            System.err.println("[RAG][PROGRESS][WARN] Failed to notify progress: " + safeMessage(exception));
        }
    }

    /**
     * 진행률을 계산합니다.
     */
    private int calculatePercent(int current, int total) {

        if (total <= 0) return 0;
        if (current <= 0) return 0;
        if (current >= total) return 100;

        return (int) Math.round(current * 100.0 / total);
    }

    /**
     * 예외 메시지를 안전하게 반환합니다.
     */
    private String safeMessage(Throwable throwable) {

        if (throwable == null) return "Unknown error";

        String message = throwable.getMessage();

        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message.trim();
    }

    /**
     * 프로젝트 RAG 인덱서 사용 가능 여부를 반환합니다.
     */
    @Override
    public boolean isAvailable() {

        try {

            String model = embeddingModelProvider.getModel();

            return model != null
                && !model.isBlank()
                && ragIndexer.isAvailable()
                && vectorStore.isAvailable();

        } catch (RuntimeException exception) {

            return false;
        }
    }

    @Override
    public String toString() {

        return "DefaultProjectRagIndexer{"
            + "documentLoader=" + documentLoader.getClass().getSimpleName()
            + ", ragIndexer=" + ragIndexer.getClass().getSimpleName()
            + ", vectorStore=" + vectorStore.getClass().getSimpleName()
            + ", chunkContextProvider=" + chunkContextProvider.getClass().getSimpleName()
            + ", embeddingModelProvider=" + embeddingModelProvider.getClass().getSimpleName()
            + '}';
    }

    /**
     * 한 번의 프로젝트 동기화 집계 상태입니다.
     */
    private static final class ProjectIndexState {

        private int processedFiles;
        private int newFiles;
        private int reindexedFiles;
        private int skippedFiles;
        private int deletedFiles;

        private int createdChunks;
        private int createdEmbeddings;
        private int storedVectors;
        private int deletedVectors;

        private final List<String> indexedFiles = new ArrayList<>();
        private final List<String> skippedFilePaths = new ArrayList<>();
        private final List<String> deletedFilePaths = new ArrayList<>();
    }
}