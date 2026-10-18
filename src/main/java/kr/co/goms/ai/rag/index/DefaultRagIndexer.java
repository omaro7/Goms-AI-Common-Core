/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.index;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import kr.co.goms.ai.rag.embedding.DocumentEmbeddingTextBuilder;
import kr.co.goms.ai.rag.embedding.EmbeddingClient;
import kr.co.goms.ai.rag.embedding.EmbeddingException;
import kr.co.goms.ai.rag.embedding.EmbeddingModelProvider;
import kr.co.goms.ai.rag.embedding.EmbeddingPurpose;
import kr.co.goms.ai.rag.embedding.EmbeddingRequest;
import kr.co.goms.ai.rag.embedding.EmbeddingResponse;
import kr.co.goms.ai.rag.hash.HashException;
import kr.co.goms.ai.rag.hash.HashService;
import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.model.DocumentSource;
import kr.co.goms.ai.rag.util.RagUtil;
import kr.co.goms.ai.rag.vector.VectorRecord;
import kr.co.goms.ai.rag.vector.VectorStore;
import kr.co.goms.ai.rag.vector.VectorStoreException;

/**
 * EPUB 문서를 Chunk로 분할하고 임베딩한 뒤 VectorStore에 저장하는
 * 기본 {@link RagIndexer} 구현체입니다.
 *
 * <pre>
 * projectId
 *      +
 * DocumentSource
 *      ↓
 * DocumentIndexer
 *      ↓
 * DocumentChunk
 *      ↓
 * DocumentEmbeddingTextBuilder
 *      ↓
 * HashService
 *      ↓
 * EmbeddingClient
 *      ↓
 * VectorRecord
 *      ↓
 * VectorStore
 * </pre>
 *
 * <h2>Project ID 정책</h2>
 *
 * <p>{@code projectId}는 데이터베이스 UUID가 아니라
 * {@code lunchwork_seoul}과 같은 GomsBook 논리 프로젝트 ID입니다.</p>
 *
 * <p>DefaultRagIndexer 생성자에는 projectId를 저장하지 않습니다.
 * 동일 인덱서 인스턴스를 여러 프로젝트에서 재사용할 수 있도록
 * index/remove 등의 실행 메서드에서 projectId를 전달합니다.</p>
 *
 * <h2>증분 인덱싱 정책</h2>
 *
 * <p>Chunk 콘텐츠 해시가 기존 VectorRecord와 동일하면 기존 Vector를
 * 재사용하고 변경된 Chunk만 다시 임베딩합니다.</p>
 *
 * <p>{@link RagIndexRequest#isForceReindex()}가 활성화된 경우 기존 Vector를
 * 재사용하지 않으며, {@link RagIndexRequest#isReplaceSource()}가 활성화된 경우
 * 해당 프로젝트/문서/모델의 기존 Vector를 제거한 뒤 다시 생성합니다.</p>
 *
 * <h2>Contextual Embedding 정책</h2>
 *
 * <p>실제 Embedding 입력 문자열은 {@link DocumentEmbeddingTextBuilder}가 생성합니다.
 * 현재 Chunk만 사용하는 대신 필요한 경우 같은 문서의 이전 Paragraph 문맥을 포함하여
 * Chunk Boundary로 인한 의미 손실을 줄입니다.</p>
 *
 * <p>증분 재사용 판단에 사용하는 contentHash도 원본 Chunk 자체가 아니라
 * 실제 Embedding 입력 문자열을 기준으로 생성합니다. 따라서 이전 Paragraph가 변경되어
 * 현재 Chunk의 Contextual Embedding 입력이 달라지면 현재 Chunk도 자동 재임베딩됩니다.</p>
 *
 * <h2>곰스북 1줄 원칙</h2>
 *
 * <p>메서드 선언, 단순 조건문, 변수 선언, return 및 단순 메서드 호출은
 * 가능한 한 한 줄로 작성합니다.</p>
 *
 * <p>Builder, Stream 및 복합 처리처럼 여러 단계의 의미를 가지는 코드는
 * 가독성을 위해 의미 단위로 줄을 나눕니다.</p>
 */
public final class DefaultRagIndexer implements RagIndexer {

    /**
     * 원본 문서를 Chunk로 분할합니다.
     */
    private final DocumentIndexer documentIndexer;

    /**
     * Chunk의 실제 Embedding 입력 문자열을 생성합니다.
     */
    private final DocumentEmbeddingTextBuilder documentEmbeddingTextBuilder;

    /**
     * Chunk Embedding Vector를 생성합니다.
     */
    private final EmbeddingClient embeddingClient;

    /**
     * 현재 Embedding Model을 제공합니다.
     */
    private final EmbeddingModelProvider embeddingModelProvider;

    /**
     * VectorRecord 저장소입니다.
     */
    private final VectorStore vectorStore;

    /**
     * Source 및 Chunk 변경 감지용 Hash 서비스입니다.
     */
    private final HashService hashService;

    /**
     * 기본 RAG 인덱서를 생성합니다.
     *
     * <p>projectId는 생성자 Dependency가 아니라 실행 Context이므로
     * 생성자에 포함하지 않습니다.</p>
     *
     * @param documentIndexer 문서 Chunk 인덱서
     * @param documentEmbeddingTextBuilder Embedding 입력 문자열 생성기
     * @param embeddingClient Embedding 클라이언트
     * @param embeddingModelProvider Embedding Model 제공자
     * @param vectorStore Vector 저장소
     * @param hashService Hash 생성 서비스
     */
    public DefaultRagIndexer(DocumentIndexer documentIndexer, DocumentEmbeddingTextBuilder documentEmbeddingTextBuilder, EmbeddingClient embeddingClient, EmbeddingModelProvider embeddingModelProvider, VectorStore vectorStore, HashService hashService) {

        this.documentIndexer = Objects.requireNonNull(documentIndexer, "documentIndexer must not be null");
        this.documentEmbeddingTextBuilder = Objects.requireNonNull(documentEmbeddingTextBuilder, "documentEmbeddingTextBuilder must not be null");
        this.embeddingClient = Objects.requireNonNull(embeddingClient, "embeddingClient must not be null");
        this.embeddingModelProvider = Objects.requireNonNull(embeddingModelProvider, "embeddingModelProvider must not be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");
        this.hashService = Objects.requireNonNull(hashService, "hashService must not be null");
    }

    /**
     * 기본 {@link DocumentEmbeddingTextBuilder}를 사용하는 편의 생성자입니다.
     *
     * @param documentIndexer 문서 Chunk 인덱서
     * @param embeddingClient Embedding 클라이언트
     * @param embeddingModelProvider Embedding Model 제공자
     * @param vectorStore Vector 저장소
     * @param hashService Hash 생성 서비스
     */
    public DefaultRagIndexer(DocumentIndexer documentIndexer, EmbeddingClient embeddingClient, EmbeddingModelProvider embeddingModelProvider, VectorStore vectorStore, HashService hashService) {

        this(documentIndexer, new DocumentEmbeddingTextBuilder(), embeddingClient, embeddingModelProvider, vectorStore, hashService);
    }

    /**
     * 문서 하나를 기본 설정으로 인덱싱합니다.
     */
    @Override
    public RagIndexResult index(String projectId, DocumentSource source) throws RagIndexException {

        return index(projectId, source, RagIndexRequest.defaults());
    }

    /**
     * 문서 하나를 지정된 설정으로 인덱싱합니다.
     */
    @Override
    public RagIndexResult index(String projectId, DocumentSource source, RagIndexRequest request) throws RagIndexException {

        String normalizedProjectId = requireProjectId(projectId);

        validateSource(source);
        validateRequest(request);

        String sourcePath = requireSourcePath(source.getRelativePath());
        String model = resolveEmbeddingModel(sourcePath);

        List<DocumentChunk> chunks;

        try {

            chunks = documentIndexer.index(source);

        } catch (DocumentIndexException exception) {

            throw failure("Failed to split document into chunks", RagIndexOperation.CHUNK_DOCUMENT, sourcePath, "", model, exception);

        } catch (RuntimeException exception) {

            throw failure("Unexpected error while splitting document", RagIndexOperation.CHUNK_DOCUMENT, sourcePath, "", model, exception);
        }

        return indexChunksInternal(normalizedProjectId, source, chunks, request, model);
    }

    /**
     * 이미 생성된 Chunk 목록을 직접 인덱싱합니다.
     */
    @Override
    public RagIndexResult indexChunks(String projectId, DocumentSource source, List<DocumentChunk> chunks, RagIndexRequest request) throws RagIndexException {

        String normalizedProjectId = requireProjectId(projectId);

        validateSource(source);
        validateRequest(request);
        validateChunks(source, chunks);

        String sourcePath = requireSourcePath(source.getRelativePath());
        String model = resolveEmbeddingModel(sourcePath);

        return indexChunksInternal(normalizedProjectId, source, chunks, request, model);
    }

    /**
     * 실제 Chunk 증분 인덱싱을 수행합니다.
     */
    private RagIndexResult indexChunksInternal(String projectId, DocumentSource source, List<DocumentChunk> chunks, RagIndexRequest request, String model) throws RagIndexException {

        long startedAt = System.nanoTime();

        String sourcePath = requireSourcePath(source.getRelativePath());
        List<DocumentChunk> safeChunks = sanitizeChunks(source, chunks);

        RagIndexState state = new RagIndexState(sourcePath, model, safeChunks.size());

        try {

            /*
             * replaceSource=true이면 현재 프로젝트/문서/모델의 기존 Vector만 삭제합니다.
             *
             * 다른 projectId의 동일 sourcePath에는 영향을 주지 않습니다.
             */
            if (request.isReplaceSource()) {

                state.deletedCount = deleteExistingSource(projectId, sourcePath, model);
            }

            ExistingRecordIndex existingIndex = request.isReplaceSource()
                ? ExistingRecordIndex.empty()
                : loadExistingRecords(projectId, sourcePath, model);

            IndexPlan plan = createIndexPlan(safeChunks, request, model, existingIndex, state);

            embedAndSave(projectId, source, plan.chunksToEmbed, plan.embeddingTexts, plan.contentHashes, request, model, state);

            /*
             * replaceSource=false이면 현재 문서에서 사라진 stale Chunk만 제거합니다.
             */
            if (!request.isReplaceSource()) {

                state.deletedCount += deleteStaleRecords(projectId, existingIndex, plan.currentChunkIds, model);
            }

            state.durationNanos = System.nanoTime() - startedAt;
            state.success = state.failedCount == 0;

            return state.toResult();

        } catch (RagIndexException exception) {

            throw exception;

        } catch (RuntimeException exception) {

            throw failure("Unexpected error while indexing document", RagIndexOperation.SAVE, sourcePath, "", model, exception);
        }
    }

    /**
     * 기존 VectorRecord 재사용 여부와 새 임베딩 대상 Chunk를 계산합니다.
     *
     * <p>contentHash는 원본 Chunk 문자열이 아니라
     * {@link DocumentEmbeddingTextBuilder}가 생성한 실제 Embedding 입력 문자열을
     * 기준으로 계산합니다.</p>
     *
     * <p>이전 Paragraph가 변경되어 현재 Chunk의 Embedding Context가 달라지는 경우에도
     * contentHash가 변경되므로 현재 Chunk를 자동으로 재임베딩할 수 있습니다.</p>
     */
    private IndexPlan createIndexPlan(List<DocumentChunk> chunks, RagIndexRequest request, String model, ExistingRecordIndex existingIndex, RagIndexState state) throws RagIndexException {

        List<DocumentChunk> chunksToEmbed = new ArrayList<>();
        Map<String, String> embeddingTexts = new LinkedHashMap<>();
        Map<String, String> contentHashes = new LinkedHashMap<>();
        Set<String> currentChunkIds = new LinkedHashSet<>();

        for (int index = 0; index < chunks.size(); index++) {

            DocumentChunk chunk = chunks.get(index);

            currentChunkIds.add(chunk.getId());

            String embeddingText;

            try {

                embeddingText = documentEmbeddingTextBuilder.build(chunks, index);

            } catch (RuntimeException exception) {

                handleChunkFailure(request, state, chunk, "Failed to create chunk embedding text", RagIndexOperation.CREATE_HASH, model, exception);

                continue;
            }

            if (embeddingText == null || embeddingText.isBlank()) {

                handleChunkFailure(request, state, chunk, "Chunk embedding text is empty", RagIndexOperation.CREATE_HASH, model, null);

                continue;
            }

            String contentHash;

            try {

                contentHash = hashService.hash(embeddingText, StandardCharsets.UTF_8);

            } catch (HashException exception) {

                handleChunkFailure(request, state, chunk, "Failed to create embedding content hash", RagIndexOperation.CREATE_HASH, model, exception);

                continue;
            }

            Optional<VectorRecord> existing = existingIndex.find(chunk.getId());

            if (canReuse(request, chunk, contentHash, existing, model)) {

                state.reusedCount++;

                continue;
            }

            chunksToEmbed.add(chunk);
            embeddingTexts.put(chunk.getId(), embeddingText);
            contentHashes.put(chunk.getId(), contentHash);
        }

        return new IndexPlan(List.copyOf(chunksToEmbed), Map.copyOf(embeddingTexts), Map.copyOf(contentHashes), Set.copyOf(currentChunkIds));
    }

    /**
     * 기존 VectorRecord를 재사용할 수 있는지 판단합니다.
     */
    private boolean canReuse(RagIndexRequest request, DocumentChunk chunk, String contentHash, Optional<VectorRecord> existing, String model) {

        if (request.isForceReindex()) return false;
        if (!request.isReuseUnchanged()) return false;
        if (existing.isEmpty()) return false;

        VectorRecord record = existing.get();

        if (!record.isModel(model)) return false;
        if (!record.hasContentHash()) return false;
        if (!hashService.matches(contentHash, record.getContentHash())) return false;

        return chunk.getId().equals(record.getChunk().getId());
    }

    /**
     * 임베딩이 필요한 Chunk를 Batch 단위로 처리합니다.
     */
    private void embedAndSave(String projectId, DocumentSource source, List<DocumentChunk> chunks, Map<String, String> embeddingTexts, Map<String, String> contentHashes, RagIndexRequest request, String model, RagIndexState state) throws RagIndexException {

        if (chunks.isEmpty()) return;

        int batchSize = request.getBatchSize();

        for (int start = 0; start < chunks.size(); start += batchSize) {

            int end = Math.min(start + batchSize, chunks.size());

            List<DocumentChunk> batch = chunks.subList(start, end);

            try {

                processBatch(projectId, source, batch, embeddingTexts, contentHashes, request, model, state);

            } catch (RagIndexException exception) {

                if (!request.isContinueOnError()) throw exception;

                processBatchIndividually(projectId, source, batch, embeddingTexts, contentHashes, request, model, state, exception);
            }
        }
    }

    /**
     * 하나의 Chunk Batch를 임베딩하고 저장합니다.
     *
     * <p>Embedding 입력 문자열은 createIndexPlan 단계에서 전체 Chunk 문맥을 기준으로
     * 미리 생성한 값을 사용합니다. Batch 분할 또는 개별 재시도 과정에서 Context가
     * 달라지는 것을 방지합니다.</p>
     */
    private void processBatch(String projectId, DocumentSource source, List<DocumentChunk> batch, Map<String, String> embeddingTexts, Map<String, String> contentHashes, RagIndexRequest request, String model, RagIndexState state) throws RagIndexException {

        List<String> inputs = new ArrayList<>(batch.size());

        for (DocumentChunk chunk : batch) {

            String embeddingText = embeddingTexts.get(chunk.getId());

            if (embeddingText == null || embeddingText.isBlank()) {

                throw failure("Chunk embedding text is missing", RagIndexOperation.EMBED, source.getRelativePath(), chunk.getId(), model, null);
            }

            inputs.add(embeddingText);
        }

        EmbeddingResponse embeddingResponse = requestEmbeddings(source.getRelativePath(), batch, inputs, request, model);
        List<float[]> vectors = embeddingResponse.getEmbeddings();

        if (vectors.size() != batch.size()) {

            throw failure("Embedding count mismatch. expected=" + batch.size() + ", actual=" + vectors.size(), RagIndexOperation.EMBED, source.getRelativePath(), "", model, null);
        }

        List<VectorRecord> records = new ArrayList<>(batch.size());

        for (int index = 0; index < batch.size(); index++) {

            DocumentChunk chunk = batch.get(index);
            float[] vector = vectors.get(index);
            String contentHash = contentHashes.get(chunk.getId());

            if (contentHash == null || contentHash.isBlank()) {

                throw failure("Chunk content hash is missing", RagIndexOperation.CREATE_RECORD, source.getRelativePath(), chunk.getId(), model, null);
            }

            try {

                records.add(createVectorRecord(projectId, source, chunk, vector, contentHash, embeddingResponse, request, model));

            } catch (RuntimeException exception) {

                throw failure("Failed to create vector record", RagIndexOperation.CREATE_RECORD, source.getRelativePath(), chunk.getId(), model, exception);
            }
        }

        saveRecords(source.getRelativePath(), records, model);
        
        

        for (VectorRecord record : records) {

            state.indexedCount++;
            state.indexedRecordIds.add(record.getId());
        }
    }

    /**
     * Batch 임베딩이 실패한 경우 Chunk 단위로 재시도합니다.
     *
     * <p>개별 재시도에서도 최초 IndexPlan에서 생성한 Embedding 입력 문자열을
     * 그대로 사용하여 Contextual Embedding 입력의 일관성을 유지합니다.</p>
     */
    private void processBatchIndividually(String projectId, DocumentSource source, List<DocumentChunk> batch, Map<String, String> embeddingTexts, Map<String, String> contentHashes, RagIndexRequest request, String model, RagIndexState state, RagIndexException batchException) throws RagIndexException {

        state.issues.add(RagIndexIssue.warning("Batch embedding failed; retrying chunks individually: " + safeMessage(batchException)));

        for (DocumentChunk chunk : batch) {

            try {

                processBatch(projectId, source, List.of(chunk), embeddingTexts, contentHashes, request, model, state);

            } catch (RagIndexException exception) {

                handleChunkFailure(request, state, chunk, safeMessage(exception), exception.getOperation(), model, exception);
            }
        }
    }

    /**
     * EmbeddingClient에 Batch 요청을 전달합니다.
     */
    private EmbeddingResponse requestEmbeddings(String sourcePath, List<DocumentChunk> batch, List<String> inputs, RagIndexRequest request, String model) throws RagIndexException {

        String requestId = createEmbeddingRequestId(sourcePath, batch);

        EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
            .model(model)
            .inputs(inputs)
            .purpose(EmbeddingPurpose.DOCUMENT)
            .normalize(request.isNormalize())
            .truncate(request.isTruncate())
            .requestId(requestId)
            .build();

        try {

            EmbeddingResponse response = embeddingClient.embed(embeddingRequest);

            embeddingClient.validateResponse(embeddingRequest, response);

            return response;

        } catch (EmbeddingException exception) {

            throw failure(
                "Failed to create document embeddings",
                RagIndexOperation.EMBED,
                sourcePath,
                batch.size() == 1 ? batch.get(0).getId() : "",
                model,
                exception
            );

        } catch (RuntimeException exception) {

            throw failure(
                "Unexpected error while creating embeddings",
                RagIndexOperation.EMBED,
                sourcePath,
                batch.size() == 1 ? batch.get(0).getId() : "",
                model,
                exception
            );
        }
    }

    /**
     * Embedding 결과를 VectorRecord로 변환합니다.
     */
    private VectorRecord createVectorRecord(String projectId, DocumentSource source, DocumentChunk chunk, float[] vector, String contentHash, EmbeddingResponse embeddingResponse, RagIndexRequest request, String model) {

        return VectorRecord.builder()
            .projectId(projectId)
            .chunk(chunk)
            .vector(vector)
            .model(model)
            .contentHash(contentHash)
            .sourceHash(resolveSourceHash(source))
            .normalized(embeddingResponse.isNormalized())
            .indexedAt(System.currentTimeMillis())
            .version(request.getVersion())
            .build();
    }

    /**
     * VectorRecord 목록을 VectorStore에 저장합니다.
     */
    private void saveRecords(String sourcePath, List<VectorRecord> records, String model) throws RagIndexException {

        try {

            vectorStore.saveAll(records);

        } catch (VectorStoreException exception) {

            throw failure(
                "Failed to save vector records",
                RagIndexOperation.SAVE,
                sourcePath,
                "",
                model,
                exception
            );

        } catch (RuntimeException exception) {

            throw failure(
                "Unexpected error while saving vector records",
                RagIndexOperation.SAVE,
                sourcePath,
                "",
                model,
                exception
            );
        }
    }

    /**
     * 동일 프로젝트/문서에 저장된 기존 VectorRecord를 조회합니다.
     */
    private ExistingRecordIndex loadExistingRecords(String projectId, String sourcePath, String model) throws RagIndexException {

        try {

            List<VectorRecord> sourceRecords = vectorStore.findByProjectAndSourcePath(projectId, sourcePath);

            Map<String, VectorRecord> indexed = new HashMap<>();

            for (VectorRecord record : sourceRecords) {

                if (record == null) continue;
                if (!record.isProject(projectId)) continue;
                if (!record.isModel(model)) continue;

                indexed.put(record.getId(), record);
            }

            return new ExistingRecordIndex(indexed);

        } catch (VectorStoreException exception) {

            throw failure(
                "Failed to load existing vector records",
                RagIndexOperation.CHECK_EXISTING,
                sourcePath,
                "",
                model,
                exception
            );
        }
    }

    /**
     * 현재 문서에 더 이상 존재하지 않는 stale VectorRecord를 삭제합니다.
     */
    private int deleteStaleRecords(String projectId, ExistingRecordIndex existingIndex, Set<String> currentChunkIds, String model) throws RagIndexException {

        int deletedCount = 0;

        for (VectorRecord existing : existingIndex.records.values()) {

            if (currentChunkIds.contains(existing.getId())) continue;

            try {

                if (vectorStore.delete(projectId, existing.getId(), model)) deletedCount++;

            } catch (VectorStoreException exception) {

                throw failure(
                    "Failed to delete stale vector record",
                    RagIndexOperation.DELETE,
                    existing.getChunk().getSourcePath(),
                    existing.getId(),
                    model,
                    exception
                );
            }
        }

        return deletedCount;
    }

    /**
     * 현재 프로젝트의 기존 문서 Vector를 삭제합니다.
     */
    private int deleteExistingSource(String projectId, String sourcePath, String model) throws RagIndexException {

        try {

            return vectorStore.deleteByProjectAndSourcePath(projectId, sourcePath, model);

        } catch (VectorStoreException exception) {

            throw failure(
                "Failed to remove existing source index",
                RagIndexOperation.DELETE,
                sourcePath,
                "",
                model,
                exception
            );
        }
    }

    /**
     * 현재 Embedding Model을 기준으로 프로젝트 문서 Vector를 삭제합니다.
     */
    @Override
    public int remove(String projectId, String sourcePath) throws RagIndexException {

        String normalizedProjectId = requireProjectId(projectId);
        String normalizedPath = requireSourcePath(sourcePath);
        String model = resolveEmbeddingModel(normalizedPath);

        return remove(normalizedProjectId, normalizedPath, model);
    }

    /**
     * 특정 프로젝트/문서/Embedding Model의 Vector를 삭제합니다.
     */
    @Override
    public int remove(String projectId, String sourcePath, String model) throws RagIndexException {

        String normalizedProjectId = requireProjectId(projectId);
        String normalizedPath = requireSourcePath(sourcePath);
        String normalizedModel = RagUtil.requireText(model, "model");

        try {

            return vectorStore.deleteByProjectAndSourcePath(
                normalizedProjectId,
                normalizedPath,
                normalizedModel
            );

        } catch (VectorStoreException exception) {

            throw failure(
                "Failed to remove source index for model",
                RagIndexOperation.DELETE,
                normalizedPath,
                "",
                normalizedModel,
                exception
            );
        }
    }

    /**
     * 특정 프로젝트에서 현재 Embedding Model로 생성된 모든 Vector를 삭제합니다.
     *
     * <p>deleteByModel()은 모든 프로젝트를 삭제할 수 있으므로 사용하지 않습니다.</p>
     */
    @Override
    public int removeCurrentModel(String projectId) throws RagIndexException {

        String normalizedProjectId = requireProjectId(projectId);
        String model = resolveEmbeddingModel("");

        try {

            List<VectorRecord> records = vectorStore.findByProjectAndModel(normalizedProjectId, model);

            if (records == null || records.isEmpty()) return 0;

            List<String> ids = records.stream()
                .filter(Objects::nonNull)
                .map(VectorRecord::getId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

            if (ids.isEmpty()) return 0;

            return vectorStore.deleteAll(normalizedProjectId, ids, model);

        } catch (VectorStoreException exception) {

            throw failure(
                "Failed to remove current project model index",
                RagIndexOperation.DELETE,
                "",
                "",
                model,
                exception
            );
        }
    }

    /**
     * VectorStore 전체를 초기화합니다.
     *
     * <p>모든 projectId의 데이터가 삭제되는 관리용 API입니다.</p>
     */
    @Override
    public void clear() throws RagIndexException {

        try {

            vectorStore.clear();

        } catch (VectorStoreException exception) {

            throw failure(
                "Failed to clear vector store",
                RagIndexOperation.CLEAR,
                "",
                "",
                "",
                exception
            );
        }
    }

    /**
     * DocumentSource 지원 여부를 확인합니다.
     */
    @Override
    public boolean supports(DocumentSource source) {

        return source != null && documentIndexer.supports(source);
    }

    /**
     * 인덱싱 구성요소 사용 가능 여부를 확인합니다.
     */
    @Override
    public boolean isAvailable() {

        try {

            String model = embeddingModelProvider.getModel();

            return model != null
                && !model.isBlank()
                && hashService.isAvailable()
                && embeddingClient.isAvailable(model)
                && vectorStore.isAvailable();

        } catch (RuntimeException exception) {

            return false;
        }
    }

    /**
     * DocumentSource를 검증합니다.
     */
    private void validateSource(DocumentSource source) throws RagIndexException {

        if (source == null) {

            throw failure(
                "DocumentSource must not be null",
                RagIndexOperation.VALIDATE,
                "",
                "",
                "",
                null
            );
        }

        if (!supports(source)) {

            throw failure(
                "Unsupported document source type: " + source.getType(),
                RagIndexOperation.VALIDATE,
                source.getRelativePath(),
                "",
                "",
                null
            );
        }

        if (!source.hasContent()) {

            throw failure(
                "Document source content is empty",
                RagIndexOperation.VALIDATE,
                source.getRelativePath(),
                "",
                "",
                null
            );
        }
    }

    /**
     * 인덱싱 요청을 검증합니다.
     */
    private void validateRequest(RagIndexRequest request) throws RagIndexException {

        if (request == null) {

            throw failure(
                "RagIndexRequest must not be null",
                RagIndexOperation.VALIDATE,
                "",
                "",
                "",
                null
            );
        }

        if (request.getBatchSize() < 1) {

            throw failure(
                "batchSize must be greater than zero",
                RagIndexOperation.VALIDATE,
                "",
                "",
                "",
                null
            );
        }

        if (request.getVersion() < 1L) {

            throw failure(
                "version must be greater than zero",
                RagIndexOperation.VALIDATE,
                "",
                "",
                "",
                null
            );
        }
    }

    /**
     * 직접 전달된 Chunk 목록을 검증합니다.
     */
    private void validateChunks(DocumentSource source, List<DocumentChunk> chunks) throws RagIndexException {

        if (chunks != null) return;

        throw failure(
            "chunks must not be null",
            RagIndexOperation.VALIDATE,
            source.getRelativePath(),
            "",
            "",
            null
        );
    }

    /**
     * null Chunk를 제거하고 sourcePath와 Chunk ID 정합성을 확인합니다.
     */
    private List<DocumentChunk> sanitizeChunks(DocumentSource source, List<DocumentChunk> chunks) throws RagIndexException {

        if (chunks == null || chunks.isEmpty()) return List.of();

        String sourcePath = requireSourcePath(source.getRelativePath());

        List<DocumentChunk> sanitized = new ArrayList<>(chunks.size());
        Set<String> ids = new LinkedHashSet<>();

        for (DocumentChunk chunk : chunks) {

            if (chunk == null) continue;

            String chunkSourcePath = requireSourcePath(chunk.getSourcePath());

            if (!sourcePath.equals(chunkSourcePath)) {

                throw failure(
                    "Chunk sourcePath does not match DocumentSource. expected=" + sourcePath + ", actual=" + chunkSourcePath,
                    RagIndexOperation.VALIDATE,
                    sourcePath,
                    chunk.getId(),
                    "",
                    null
                );
            }

            if (!ids.add(chunk.getId())) {

                throw failure(
                    "Duplicate chunk id: " + chunk.getId(),
                    RagIndexOperation.VALIDATE,
                    sourcePath,
                    chunk.getId(),
                    "",
                    null
                );
            }

            sanitized.add(chunk);
        }

        return List.copyOf(sanitized);
    }

    /**
     * Chunk 단위 실패를 continueOnError 정책에 따라 처리합니다.
     */
    private void handleChunkFailure(RagIndexRequest request, RagIndexState state, DocumentChunk chunk, String message, RagIndexOperation operation, String model, Throwable cause) throws RagIndexException {

        if (!request.isContinueOnError()) {

            throw failure(
                message,
                operation,
                state.sourcePath,
                chunk.getId(),
                model,
                cause
            );
        }

        state.failedCount++;
        state.success = false;

        state.issues.add(
            RagIndexIssue.error(
                message,
                chunk.getId()
            )
        );
    }

    /**
     * 현재 Embedding Model을 조회합니다.
     */
    private String resolveEmbeddingModel(String sourcePath) throws RagIndexException {

        String model;

        try {

            model = embeddingModelProvider.getModel();

        } catch (RuntimeException exception) {

            throw failure(
                "Failed to resolve embedding model",
                RagIndexOperation.VALIDATE,
                sourcePath,
                "",
                "",
                exception
            );
        }

        if (model == null || model.isBlank()) {

            throw failure(
                "Embedding model must not be blank",
                RagIndexOperation.VALIDATE,
                sourcePath,
                "",
                "",
                null
            );
        }

        return model.trim();
    }

    /**
     * 원본 문서 전체의 Hash를 반환합니다.
     *
     * <p>DocumentSource에 기존 contentHash가 존재하면 재사용하고,
     * 없으면 원본 charset을 이용해 HashService로 계산합니다.</p>
     */
    private String resolveSourceHash(DocumentSource source) {

        if (source.hasContentHash()) return source.getContentHash();

        try {

            return hashService.hash(source.getContent(), source.getCharset());

        } catch (HashException exception) {

            return "";
        }
    }

    /**
     * Embedding 요청 추적 ID를 생성합니다.
     */
    private String createEmbeddingRequestId(String sourcePath, List<DocumentChunk> batch) {

        String firstChunkId = batch.isEmpty() ? "empty" : batch.get(0).getId();

        return "rag-index:" + sourcePath + ":" + firstChunkId + ":" + batch.size() + ":" + System.nanoTime();
    }

    /**
     * GomsBook 논리 projectId를 검증합니다.
     */
    private static String requireProjectId(String projectId) {

        return RagUtil.requireText(projectId, "projectId");
    }

    /**
     * 원본 문서 경로를 정규화하고 검증합니다.
     */
    private static String requireSourcePath(String sourcePath) {

        String normalized = RagUtil.normalizeDocumentPath(sourcePath);

        if (normalized == null || normalized.isBlank()) throw new IllegalArgumentException("sourcePath must not be blank");

        return normalized;
    }

    /**
     * 예외 메시지를 안전한 문자열로 변환합니다.
     */
    private static String safeMessage(Throwable throwable) {

        if (throwable == null) return "Unknown indexing error";

        String message = throwable.getMessage();

        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message.trim();
    }

    /**
     * RagIndexException 생성을 한 곳으로 통일합니다.
     *
     * <p>실제 RagIndexException의
     * message + operation + sourcePath + chunkId + model + cause
     * 생성자와 정확하게 일치합니다.</p>
     */
    private static RagIndexException failure(String message, RagIndexOperation operation, String sourcePath, String chunkId, String model, Throwable cause) {

        return new RagIndexException(message, operation, sourcePath, chunkId, model, cause);
    }

    public DocumentIndexer getDocumentIndexer() {

        return documentIndexer;
    }

    public DocumentEmbeddingTextBuilder getDocumentEmbeddingTextBuilder() {

        return documentEmbeddingTextBuilder;
    }

    public EmbeddingClient getEmbeddingClient() {

        return embeddingClient;
    }

    public EmbeddingModelProvider getEmbeddingModelProvider() {

        return embeddingModelProvider;
    }

    public VectorStore getVectorStore() {

        return vectorStore;
    }

    public HashService getHashService() {

        return hashService;
    }

    @Override
    public String toString() {

        return "DefaultRagIndexer{"
            + "documentIndexer=" + documentIndexer.getClass().getSimpleName()
            + ", documentEmbeddingTextBuilder=" + documentEmbeddingTextBuilder.getClass().getSimpleName()
            + ", embeddingClient=" + embeddingClient.getClass().getSimpleName()
            + ", embeddingModelProvider=" + embeddingModelProvider.getClass().getSimpleName()
            + ", vectorStore=" + vectorStore.getClass().getSimpleName()
            + ", hashService=" + hashService.getClass().getSimpleName()
            + '}';
    }

    /**
     * 한 문서의 인덱싱 처리 상태입니다.
     */
    private static final class RagIndexState {

        private final String sourcePath;
        private final String model;
        private final int totalChunkCount;

        private int indexedCount;
        private int reusedCount;
        private int skippedCount;
        private int deletedCount;
        private int failedCount;

        private long durationNanos;

        private boolean success = true;

        private final List<String> indexedRecordIds = new ArrayList<>();
        private final List<RagIndexIssue> issues = new ArrayList<>();

        private RagIndexState(String sourcePath, String model, int totalChunkCount) {

            this.sourcePath = sourcePath;
            this.model = model;
            this.totalChunkCount = totalChunkCount;
        }

        private RagIndexResult toResult() {

            return RagIndexResult.builder()
                .sourcePath(sourcePath)
                .model(model)
                .totalChunkCount(totalChunkCount)
                .indexedCount(indexedCount)
                .reusedCount(reusedCount)
                .skippedCount(skippedCount)
                .deletedCount(deletedCount)
                .failedCount(failedCount)
                .durationNanos(durationNanos)
                .success(success)
                .indexedRecordIds(indexedRecordIds)
                .issues(issues)
                .build();
        }
    }

    /**
     * 기존 VectorRecord를 Chunk ID로 조회하기 위한 내부 Index입니다.
     */
    private static final class ExistingRecordIndex {

        private final Map<String, VectorRecord> records;

        private ExistingRecordIndex(Map<String, VectorRecord> records) {

            this.records = Map.copyOf(records);
        }

        private static ExistingRecordIndex empty() {

            return new ExistingRecordIndex(Map.of());
        }

        private Optional<VectorRecord> find(String id) {

            if (id == null || id.isBlank()) return Optional.empty();

            return Optional.ofNullable(records.get(id));
        }
    }

    /**
     * 현재 인덱싱 실행 계획입니다.
     *
     * <p>Embedding 입력 문자열은 전체 Chunk 문맥을 기준으로 미리 생성하여
     * Batch 처리와 개별 재시도에서 동일한 값을 사용합니다.</p>
     */
    private static final class IndexPlan {

        private final List<DocumentChunk> chunksToEmbed;
        private final Map<String, String> embeddingTexts;
        private final Map<String, String> contentHashes;
        private final Set<String> currentChunkIds;

        private IndexPlan(List<DocumentChunk> chunksToEmbed, Map<String, String> embeddingTexts, Map<String, String> contentHashes, Set<String> currentChunkIds) {

            this.chunksToEmbed = List.copyOf(chunksToEmbed);
            this.embeddingTexts = Map.copyOf(embeddingTexts);
            this.contentHashes = Map.copyOf(contentHashes);
            this.currentChunkIds = Set.copyOf(currentChunkIds);
        }
    }
}