/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.vector.qdrant;

import static io.qdrant.client.ConditionFactory.match;
import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.ConditionFactory.matchKeywords;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.QueryFactory.nearest;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorFactory.vector;
import static io.qdrant.client.VectorsFactory.namedVectors;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.ListenableFuture;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.VectorOutputHelper;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.WithVectorsSelectorFactory;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.model.DocumentChunkType;
import kr.co.goms.ai.rag.util.RagUtil;
import kr.co.goms.ai.rag.vector.VectorRecord;
import kr.co.goms.ai.rag.vector.VectorSearchRequest;
import kr.co.goms.ai.rag.vector.VectorSearchResult;
import kr.co.goms.ai.rag.vector.VectorSimilarityType;
import kr.co.goms.ai.rag.vector.VectorStore;
import kr.co.goms.ai.rag.vector.VectorStoreException;
import kr.co.goms.ai.rag.vector.VectorStoreOperation;

/**
 * Qdrant 기반 {@link VectorStore} 구현체입니다.
 *
 * <p>GomsBook RAG에서 생성한 {@link VectorRecord}를 Qdrant에 영속 저장하고
 * Dense Vector Similarity Search를 수행합니다.</p>
 *
 * <pre>
 * DocumentChunk
 *      ↓
 * EmbeddingClient
 *      ↓
 * VectorRecord
 *      ↓
 * QdrantVectorStore
 *      ↓
 * Qdrant
 *      └─ gomsbook_rag
 *           └─ dense
 * </pre>
 *
 * <h2>Project ID 정책</h2>
 *
 * <p>{@code projectId}는 데이터베이스 UUID가 아니라
 * GomsBook의 안정적인 논리 프로젝트 ID를 사용합니다.</p>
 *
 * <pre>
 * projectId = lunchwork_seoul
 * </pre>
 *
 * <p>모든 프로젝트 단위 검색과 삭제는 projectId를 Qdrant Payload Filter에
 * 반드시 포함합니다.</p>
 *
 * <h2>Point ID 정책</h2>
 *
 * <p>Qdrant Point ID는 다음 논리 Key를 기반으로 deterministic UUID를 생성합니다.</p>
 *
 * <pre>
 * projectId
 *      +
 * recordId
 *      +
 * embeddingModel
 *      ↓
 * deterministic UUID
 * </pre>
 *
 * <p>Qdrant Point UUID는 저장 엔진 내부 식별자이며 GomsBook projectId 또는
 * 데이터베이스 UUID와 의미적으로 관계가 없습니다.</p>
 *
 * <h2>Vector 정책</h2>
 *
 * <p>현재 Vector 이름은 {@code dense}이며 Qdrant Collection은
 * Cosine Distance를 사용한다고 가정합니다.</p>
 *
 * <p>향후 Sparse Vector를 추가할 경우 동일 Collection에
 * {@code sparse} Named Vector를 추가할 수 있습니다.</p>
 *
 * <h2>곰스북 1줄 원칙</h2>
 *
 * <p>메서드 선언, 단순 조건문, 변수 선언, return 및 단순 메서드 호출은
 * 가능한 한 한 줄로 작성합니다.</p>
 *
 * <p>Builder, Stream, Filter 구성처럼 여러 단계의 의미를 가지는 코드는
 * 가독성을 유지하기 위해 의미 단위로 줄을 나눕니다.</p>
 */
public final class QdrantVectorStore implements VectorStore {

    public static final String DENSE_VECTOR_NAME = "dense";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private static final int UPSERT_BATCH_SIZE = 128;

    private static final int SCROLL_PAGE_SIZE = 256;

    private static final String PAYLOAD_PROJECT_ID = "projectId";
    private static final String PAYLOAD_RECORD_ID = "recordId";
    private static final String PAYLOAD_MODEL = "model";
    private static final String PAYLOAD_DIMENSIONS = "dimensions";
    private static final String PAYLOAD_SOURCE_PATH = "sourcePath";
    private static final String PAYLOAD_TITLE = "title";
    private static final String PAYLOAD_CHUNK_TYPE = "chunkType";
    private static final String PAYLOAD_CONTENT = "content";
    private static final String PAYLOAD_SEQUENCE = "sequence";
    private static final String PAYLOAD_ELEMENT_ID = "elementId";
    private static final String PAYLOAD_EPUB_TYPE = "epubType";
    private static final String PAYLOAD_LANGUAGE = "language";
    private static final String PAYLOAD_CONTENT_HASH = "contentHash";
    private static final String PAYLOAD_SOURCE_HASH = "sourceHash";
    private static final String PAYLOAD_NORMALIZED = "normalized";
    private static final String PAYLOAD_INDEXED_AT = "indexedAt";
    private static final String PAYLOAD_VERSION = "version";
    private static final String PAYLOAD_SOURCE_TYPE = "sourceType";

    private static final String METADATA_PREFIX = "metadata__";

    private final QdrantClient qdrantClient;

    private final String collectionName;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * QdrantVectorStore를 생성합니다.
     *
     * <p>QdrantClient는 Spring에서 생성한 공용 Bean을 전달받아 사용합니다.</p>
     *
     * @param qdrantClient 공용 Qdrant Java Client
     * @param configuration Qdrant 설정
     */
    public QdrantVectorStore(QdrantClient qdrantClient, QdrantConfiguration configuration) {

        this.qdrantClient = Objects.requireNonNull(qdrantClient, "qdrantClient must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");

        this.collectionName = RagUtil.requireText(configuration.getCollectionName(), "collectionName");
    }

    /**
     * VectorRecord 하나를 Qdrant에 Upsert합니다.
     *
     * <p>동일한 projectId + recordId + model은 동일한 deterministic Point UUID를
     * 생성하므로 기존 Point가 존재하면 갱신됩니다.</p>
     */
    @Override
    public void save(VectorRecord record) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.SAVE);

        if (record == null) throw new IllegalArgumentException("record must not be null");

        try {

            await(qdrantClient.upsertAsync(collectionName, List.of(toPoint(record)), REQUEST_TIMEOUT));

        } catch (Exception exception) {

            throw failure("Failed to save vector record to Qdrant", VectorStoreOperation.SAVE, record.getId(), record.getModel(), exception);
        }
    }

    /**
     * 여러 VectorRecord를 Qdrant Batch Upsert로 저장합니다.
     *
     * <p>한 번에 지나치게 많은 Point를 전송하지 않도록 128개 단위로 분할합니다.</p>
     */
    @Override
    public void saveAll(List<VectorRecord> records) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.SAVE_ALL);

        if (records == null || records.isEmpty()) return;

        try {

            List<Points.PointStruct> points = new ArrayList<>();

            for (VectorRecord record : records) {

                if (record == null) continue;

                points.add(toPoint(record));
            }

            for (int start = 0; start < points.size(); start += UPSERT_BATCH_SIZE) {

                int end = Math.min(start + UPSERT_BATCH_SIZE, points.size());

                await(qdrantClient.upsertAsync(collectionName, points.subList(start, end), REQUEST_TIMEOUT));
            }

        } catch (Exception exception) {

            throw new VectorStoreException("Failed to save vector records to Qdrant", VectorStoreOperation.SAVE_ALL, exception);
        }
    }

    /**
     * Qdrant Dense Vector Search를 수행합니다.
     *
     * <p>projectId, model, dimensions를 기본 Filter로 사용하고
     * VectorSearchRequest에 지정된 추가 검색 조건을 Qdrant Payload Filter로 변환합니다.</p>
     *
     * <p>현재 gomsbook_rag Collection은 Cosine Distance를 사용하므로
     * COSINE 검색만 허용합니다.</p>
     */
    @Override
    public List<VectorSearchResult> search(VectorSearchRequest request) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.SEARCH);
        validateSearchRequest(request);

        try {

            Points.QueryPoints.Builder builder = Points.QueryPoints.newBuilder()
                .setCollectionName(collectionName)
                .setUsing(DENSE_VECTOR_NAME)
                .setQuery(nearest(request.getQueryVector()))
                .setFilter(createSearchFilter(request))
                .setLimit(request.getTopK())
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                .setWithVectors(WithVectorsSelectorFactory.include(List.of(DENSE_VECTOR_NAME)));

            if (!request.isIncludeRejected()) builder.setScoreThreshold((float) request.getMinimumScore());

            List<Points.ScoredPoint> points = await(qdrantClient.queryAsync(builder.build(), REQUEST_TIMEOUT));

            List<VectorSearchResult> results = new ArrayList<>(points.size());

            for (int index = 0; index < points.size(); index++) {

                Points.ScoredPoint point = points.get(index);

                VectorRecord record = toVectorRecord(point.getPayloadMap(), point.getVectors());

                double score = point.getScore();

                results.add(
                    VectorSearchResult.builder()
                        .record(record)
                        .score(score)
                        .rank(index + 1)
                        .similarityType(VectorSimilarityType.COSINE)
                        .accepted(request.accepts(score))
                        .build()
                );
            }

            return List.copyOf(results);

        } catch (Exception exception) {

            throw new VectorStoreException("Failed to search Qdrant vector records", VectorStoreOperation.SEARCH, exception);
        }
    }

    /**
     * projectId + recordId + model로 정확한 Point 하나를 조회합니다.
     */
    @Override
    public Optional<VectorRecord> findById(String projectId, String id, String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = RagUtil.requireProjectId(projectId);
        String normalizedId = RagUtil.requireText(id, "id");
        String normalizedModel = RagUtil.requireText(model, "model");

        try {

            Common.PointId pointId = createPointId(normalizedProjectId, normalizedId, normalizedModel);

            List<Points.RetrievedPoint> points = await(
                qdrantClient.retrieveAsync(
                    collectionName,
                    List.of(pointId),
                    WithPayloadSelectorFactory.enable(true),
                    WithVectorsSelectorFactory.include(List.of(DENSE_VECTOR_NAME)),
                    null,
                    REQUEST_TIMEOUT
                )
            );

            if (points.isEmpty()) return Optional.empty();

            VectorRecord record = toVectorRecord(points.get(0).getPayloadMap(), points.get(0).getVectors());

            if (!record.isProject(normalizedProjectId)) return Optional.empty();
            if (!record.getId().equals(normalizedId)) return Optional.empty();
            if (!record.isModel(normalizedModel)) return Optional.empty();

            return Optional.of(record);

        } catch (Exception exception) {

            throw failure("Failed to find Qdrant vector record", VectorStoreOperation.FIND, normalizedId, normalizedModel, exception);
        }
    }

    /**
     * 특정 프로젝트에서 recordId가 동일한 레코드를 조회합니다.
     *
     * <p>여러 Embedding Model의 데이터가 존재하는 경우 indexedAt이 가장 최근인
     * 레코드를 반환합니다.</p>
     */
    @Override
    public Optional<VectorRecord> findById(String projectId, String id) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = RagUtil.requireProjectId(projectId);
        String normalizedId = RagUtil.requireText(id, "id");

        Common.Filter filter = filter(
            matchKeyword(PAYLOAD_PROJECT_ID, normalizedProjectId),
            matchKeyword(PAYLOAD_RECORD_ID, normalizedId)
        );

        List<VectorRecord> records = scrollRecords(filter);

        return records.stream().max(Comparator.comparingLong(VectorRecord::getIndexedAt));
    }

    /**
     * 특정 프로젝트에서 여러 recordId에 해당하는 레코드를 조회합니다.
     */
    @Override
    public List<VectorRecord> findByIds(String projectId, List<String> ids) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = RagUtil.requireProjectId(projectId);

        if (ids == null || ids.isEmpty()) return List.of();

        List<String> normalizedIds = ids.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();

        if (normalizedIds.isEmpty()) return List.of();

        Common.Filter filter = filter(
            matchKeyword(PAYLOAD_PROJECT_ID, normalizedProjectId),
            matchKeywords(PAYLOAD_RECORD_ID, normalizedIds)
        );

        return scrollRecords(filter).stream()
            .sorted(Comparator.comparing(VectorRecord::getId).thenComparing(VectorRecord::getModel))
            .toList();
    }

    /**
     * Collection에 저장된 모든 VectorRecord를 조회합니다.
     *
     * <p>대규모 데이터에서는 비용이 큰 관리용 API입니다.</p>
     */
    @Override
    public List<VectorRecord> findAll() throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        return scrollRecords(null).stream()
            .sorted(Comparator.comparing(VectorRecord::getProjectId).thenComparing(VectorRecord::getId).thenComparing(VectorRecord::getModel))
            .toList();
    }

    /**
     * 모든 프로젝트에서 특정 Embedding Model의 레코드를 조회합니다.
     */
    @Override
    public List<VectorRecord> findByModel(String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedModel = RagUtil.requireText(model, "model");

        return scrollRecords(filter(matchKeyword(PAYLOAD_MODEL, normalizedModel))).stream()
            .sorted(Comparator.comparing(VectorRecord::getProjectId).thenComparing(VectorRecord::getId))
            .toList();
    }

    /**
     * 특정 프로젝트와 Embedding Model의 레코드를 조회합니다.
     */
    @Override
    public List<VectorRecord> findByProjectAndModel(String projectId, String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = RagUtil.requireProjectId(projectId);
        String normalizedModel = RagUtil.requireText(model, "model");

        Common.Filter filter = filter(
            matchKeyword(PAYLOAD_PROJECT_ID, normalizedProjectId),
            matchKeyword(PAYLOAD_MODEL, normalizedModel)
        );

        return scrollRecords(filter).stream()
            .sorted(Comparator.comparing(VectorRecord::getId))
            .toList();
    }

    /**
     * 모든 프로젝트에서 특정 sourcePath의 레코드를 조회합니다.
     */
    @Override
    public List<VectorRecord> findBySourcePath(String sourcePath) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedPath = requireSourcePath(sourcePath);

        return scrollRecords(filter(matchKeyword(PAYLOAD_SOURCE_PATH, normalizedPath))).stream()
            .sorted(
                Comparator
                    .comparing(VectorRecord::getProjectId)
                    .thenComparingInt(record -> record.getChunk().getSequence())
                    .thenComparing(VectorRecord::getId)
            )
            .toList();
    }

    /**
     * 특정 프로젝트의 특정 sourcePath 레코드를 조회합니다.
     */
    @Override
    public List<VectorRecord> findByProjectAndSourcePath(String projectId, String sourcePath) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = RagUtil.requireProjectId(projectId);
        String normalizedPath = requireSourcePath(sourcePath);

        Common.Filter filter = filter(
            matchKeyword(PAYLOAD_PROJECT_ID, normalizedProjectId),
            matchKeyword(PAYLOAD_SOURCE_PATH, normalizedPath)
        );

        return scrollRecords(filter).stream()
            .sorted(Comparator.comparingInt((VectorRecord record) -> record.getChunk().getSequence()).thenComparing(VectorRecord::getId))
            .toList();
    }

    /**
     * projectId + recordId + model로 정확한 Point 하나를 삭제합니다.
     */
    @Override
    public boolean delete(String projectId, String id, String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.DELETE);

        String normalizedProjectId = RagUtil.requireProjectId(projectId);
        String normalizedId = RagUtil.requireText(id, "id");
        String normalizedModel = RagUtil.requireText(model, "model");

        try {

            Common.PointId pointId = createPointId(normalizedProjectId, normalizedId, normalizedModel);

            if (findById(normalizedProjectId, normalizedId, normalizedModel).isEmpty()) return false;

            await(qdrantClient.deleteAsync(collectionName, List.of(pointId), REQUEST_TIMEOUT));

            return true;

        } catch (VectorStoreException exception) {

            throw exception;

        } catch (Exception exception) {

            throw failure("Failed to delete Qdrant vector record", VectorStoreOperation.DELETE, normalizedId, normalizedModel, exception);
        }
    }

    /**
     * 모든 프로젝트에서 특정 sourcePath의 Point를 삭제합니다.
     *
     * <p>프로젝트 범위가 없는 관리용 API입니다.</p>
     */
    @Override
    public int deleteBySourcePath(String sourcePath) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.DELETE);

        String normalizedPath = requireSourcePath(sourcePath);

        Common.Filter filter = filter(matchKeyword(PAYLOAD_SOURCE_PATH, normalizedPath));

        return deleteByFilter(filter);
    }

    /**
     * 기존 호출부 호환용 API입니다.
     */
    @Override
    public int deleteBySourcePath(String projectId, String sourcePath, String model) throws VectorStoreException {

        return deleteByProjectAndSourcePath(projectId, sourcePath, model);
    }

    /**
     * 특정 프로젝트, sourcePath, Embedding Model의 Point를 삭제합니다.
     */
    @Override
    public int deleteByProjectAndSourcePath(String projectId, String sourcePath, String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.DELETE);

        String normalizedProjectId = RagUtil.requireProjectId(projectId);
        String normalizedPath = requireSourcePath(sourcePath);
        String normalizedModel = RagUtil.requireText(model, "model");

        Common.Filter filter = filter(
            matchKeyword(PAYLOAD_PROJECT_ID, normalizedProjectId),
            matchKeyword(PAYLOAD_SOURCE_PATH, normalizedPath),
            matchKeyword(PAYLOAD_MODEL, normalizedModel)
        );

        return deleteByFilter(filter);
    }

    /**
     * 모든 프로젝트에서 특정 Embedding Model의 Point를 삭제합니다.
     */
    @Override
    public int deleteByModel(String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.DELETE);

        String normalizedModel = RagUtil.requireText(model, "model");

        return deleteByFilter(filter(matchKeyword(PAYLOAD_MODEL, normalizedModel)));
    }

    /**
     * gomsbook_rag Collection의 모든 Point를 삭제합니다.
     *
     * <p>Collection 자체는 삭제하지 않습니다.</p>
     */
    @Override
    public void clear() throws VectorStoreException {

        ensureOpen(VectorStoreOperation.CLEAR);

        try {

            List<Common.PointId> pointIds = scrollPointIds(null);

            for (int start = 0; start < pointIds.size(); start += UPSERT_BATCH_SIZE) {

                int end = Math.min(start + UPSERT_BATCH_SIZE, pointIds.size());

                await(qdrantClient.deleteAsync(collectionName, pointIds.subList(start, end), REQUEST_TIMEOUT));
            }

        } catch (Exception exception) {

            throw new VectorStoreException("Failed to clear Qdrant vector store", VectorStoreOperation.CLEAR, exception);
        }
    }

    /**
     * Collection 전체 Point 수를 반환합니다.
     */
    @Override
    public long count() throws VectorStoreException {

        ensureOpen(VectorStoreOperation.COUNT);

        try {

            return await(qdrantClient.countAsync(collectionName, REQUEST_TIMEOUT));

        } catch (Exception exception) {

            throw new VectorStoreException("Failed to count Qdrant vector records", VectorStoreOperation.COUNT, exception);
        }
    }

    /**
     * 특정 Embedding Model의 Point 수를 반환합니다.
     */
    @Override
    public long countByModel(String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.COUNT);

        String normalizedModel = RagUtil.requireText(model, "model");

        return countByFilter(filter(matchKeyword(PAYLOAD_MODEL, normalizedModel)));
    }

    /**
     * 특정 sourcePath의 Point 수를 반환합니다.
     */
    @Override
    public long countBySourcePath(String sourcePath) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.COUNT);

        String normalizedPath = requireSourcePath(sourcePath);

        return countByFilter(filter(matchKeyword(PAYLOAD_SOURCE_PATH, normalizedPath)));
    }

    /**
     * Qdrant 연결과 Collection 존재 여부를 확인합니다.
     */
    @Override
    public boolean isAvailable() {

        if (closed.get()) return false;

        try {

            await(qdrantClient.healthCheckAsync(REQUEST_TIMEOUT));

            return await(qdrantClient.collectionExistsAsync(collectionName, REQUEST_TIMEOUT));

        } catch (Exception exception) {

            return false;
        }
    }

    /**
     * QdrantVectorStore 사용을 종료합니다.
     *
     * <p>QdrantClient는 Spring 공용 Bean이므로 여기서는 close하지 않습니다.
     * 실제 QdrantClient 종료는 Spring Bean lifecycle에서 처리합니다.</p>
     */
    @Override
    public void close() throws VectorStoreException {

        closed.set(true);
    }

    /**
     * VectorRecord를 Qdrant PointStruct로 변환합니다.
     */
    private Points.PointStruct toPoint(VectorRecord record) {

        validateRecord(record);

        return Points.PointStruct.newBuilder()
            .setId(createPointId(record.getProjectId(), record.getId(), record.getModel()))
            .setVectors(namedVectors(Map.of(DENSE_VECTOR_NAME, vector(record.getVector()))))
            .putAllPayload(toPayload(record))
            .build();
    }

    /**
     * VectorRecord를 Qdrant Payload로 변환합니다.
     */
    private Map<String, JsonWithInt.Value> toPayload(VectorRecord record) {

        DocumentChunk chunk = record.getChunk();

        Map<String, JsonWithInt.Value> payload = new LinkedHashMap<>();

        payload.put(PAYLOAD_PROJECT_ID, value(record.getProjectId()));
        payload.put(PAYLOAD_RECORD_ID, value(record.getId()));
        payload.put(PAYLOAD_MODEL, value(record.getModel()));
        payload.put(PAYLOAD_DIMENSIONS, value((long) record.getDimensions()));
        payload.put(PAYLOAD_SOURCE_PATH, value(requireSourcePath(chunk.getSourcePath())));
        payload.put(PAYLOAD_TITLE, value(normalize(chunk.getTitle())));
        payload.put(PAYLOAD_CHUNK_TYPE, value(chunk.getType().name()));
        payload.put(PAYLOAD_CONTENT, value(chunk.getContent()));
        payload.put(PAYLOAD_SEQUENCE, value((long) chunk.getSequence()));
        payload.put(PAYLOAD_ELEMENT_ID, value(normalize(chunk.getElementId())));
        payload.put(PAYLOAD_EPUB_TYPE, value(normalize(chunk.getEpubType())));
        payload.put(PAYLOAD_LANGUAGE, value(normalizeLanguage(chunk.getLanguage())));
        payload.put(PAYLOAD_CONTENT_HASH, value(normalize(record.getContentHash())));
        payload.put(PAYLOAD_SOURCE_HASH, value(normalize(record.getSourceHash())));
        payload.put(PAYLOAD_NORMALIZED, value(record.isNormalized()));
        payload.put(PAYLOAD_INDEXED_AT, value(record.getIndexedAt()));
        payload.put(PAYLOAD_VERSION, value(record.getVersion()));

        for (Map.Entry<String, String> entry : chunk.getMetadata().entrySet()) {

            if (entry.getKey() == null || entry.getKey().isBlank()) continue;

            payload.put(metadataField(entry.getKey()), value(normalize(entry.getValue())));
        }

        String sourceType = chunk.getMetadata("sourceType");

        if (sourceType != null && !sourceType.isBlank()) payload.put(PAYLOAD_SOURCE_TYPE, value(sourceType.trim()));

        return payload;
    }

    /**
     * Qdrant Payload와 Vector를 VectorRecord로 복원합니다.
     */
    private VectorRecord toVectorRecord(Map<String, JsonWithInt.Value> payload, Points.VectorsOutput vectors) {

        String projectId = requirePayloadText(payload, PAYLOAD_PROJECT_ID);
        String recordId = requirePayloadText(payload, PAYLOAD_RECORD_ID);
        String model = requirePayloadText(payload, PAYLOAD_MODEL);

        DocumentChunk chunk = DocumentChunk.builder()
            .id(recordId)
            .sourcePath(requirePayloadText(payload, PAYLOAD_SOURCE_PATH))
            .title(payloadText(payload, PAYLOAD_TITLE))
            .type(DocumentChunkType.valueOf(requirePayloadText(payload, PAYLOAD_CHUNK_TYPE)))
            .content(requirePayloadText(payload, PAYLOAD_CONTENT))
            .sequence((int) payloadLong(payload, PAYLOAD_SEQUENCE))
            .elementId(payloadText(payload, PAYLOAD_ELEMENT_ID))
            .epubType(payloadText(payload, PAYLOAD_EPUB_TYPE))
            .language(payloadText(payload, PAYLOAD_LANGUAGE))
            .metadata(readMetadata(payload))
            .build();

        float[] vector = readDenseVector(vectors);

        long expectedDimensions = payloadLong(payload, PAYLOAD_DIMENSIONS);

        if (vector.length != expectedDimensions) throw new IllegalStateException("Qdrant vector dimension mismatch. expected=" + expectedDimensions + ", actual=" + vector.length);

        return VectorRecord.builder()
            .projectId(projectId)
            .id(recordId)
            .chunk(chunk)
            .vector(vector)
            .model(model)
            .contentHash(payloadText(payload, PAYLOAD_CONTENT_HASH))
            .sourceHash(payloadText(payload, PAYLOAD_SOURCE_HASH))
            .normalized(payloadBoolean(payload, PAYLOAD_NORMALIZED))
            .indexedAt(payloadLong(payload, PAYLOAD_INDEXED_AT))
            .version(payloadLong(payload, PAYLOAD_VERSION))
            .build();
    }

    /**
     * Qdrant 검색용 Payload Filter를 생성합니다.
     */
    private Common.Filter createSearchFilter(VectorSearchRequest request) {

        Common.Filter.Builder builder = Common.Filter.newBuilder();

        builder.addMust(matchKeyword(PAYLOAD_PROJECT_ID, request.getProjectId()));
        builder.addMust(matchKeyword(PAYLOAD_MODEL, request.getModel()));
        builder.addMust(match(PAYLOAD_DIMENSIONS, (long) request.getDimensions()));

        if (request.hasChunkTypeFilters()) {

            List<String> values = request.getChunkTypes().stream().map(Enum::name).toList();

            builder.addMust(matchKeywords(PAYLOAD_CHUNK_TYPE, values));
        }

        if (request.hasSourcePathFilters()) {

            List<String> values = request.getSourcePaths().stream()
                .map(RagUtil::normalizeDocumentPath)
                .filter(Objects::nonNull)
                .toList();

            if (!values.isEmpty()) builder.addMust(matchKeywords(PAYLOAD_SOURCE_PATH, values));
        }

        if (request.hasEpubTypeFilters()) builder.addMust(matchKeywords(PAYLOAD_EPUB_TYPE, new ArrayList<>(request.getEpubTypes())));

        if (request.hasLanguageFilters()) {

            List<String> values = request.getLanguages().stream().map(QdrantVectorStore::normalizeLanguage).toList();

            builder.addMust(matchKeywords(PAYLOAD_LANGUAGE, values));
        }

        for (Map.Entry<String, String> entry : request.getMetadataFilters().entrySet()) {

            builder.addMust(matchKeyword(metadataField(entry.getKey()), normalize(entry.getValue())));
        }

        return builder.build();
    }

    /**
     * 지정된 조건의 VectorRecord를 Scroll 방식으로 모두 조회합니다.
     */
    private List<VectorRecord> scrollRecords(Common.Filter filter) throws VectorStoreException {

        try {

            List<VectorRecord> result = new ArrayList<>();

            Common.PointId offset = null;

            while (true) {

                Points.ScrollPoints.Builder builder = Points.ScrollPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .setLimit(SCROLL_PAGE_SIZE)
                    .setWithPayload(WithPayloadSelectorFactory.enable(true))
                    .setWithVectors(WithVectorsSelectorFactory.include(List.of(DENSE_VECTOR_NAME)));

                if (filter != null) builder.setFilter(filter);
                if (offset != null) builder.setOffset(offset);

                Points.ScrollResponse response = await(qdrantClient.scrollAsync(builder.build(), REQUEST_TIMEOUT));

                for (Points.RetrievedPoint point : response.getResultList()) {

                    result.add(toVectorRecord(point.getPayloadMap(), point.getVectors()));
                }

                if (!response.hasNextPageOffset()) break;

                offset = response.getNextPageOffset();
            }

            return List.copyOf(result);

        } catch (Exception exception) {

            throw new VectorStoreException("Failed to scroll Qdrant vector records", VectorStoreOperation.FIND, exception);
        }
    }

    /**
     * Point ID만 Scroll 조회합니다.
     *
     * <p>clear()에서 Vector/Payload를 불필요하게 읽지 않기 위해 사용합니다.</p>
     */
    private List<Common.PointId> scrollPointIds(Common.Filter filter) throws Exception {

        List<Common.PointId> result = new ArrayList<>();

        Common.PointId offset = null;

        while (true) {

            Points.ScrollPoints.Builder builder = Points.ScrollPoints.newBuilder()
                .setCollectionName(collectionName)
                .setLimit(SCROLL_PAGE_SIZE)
                .setWithPayload(WithPayloadSelectorFactory.enable(false))
                .setWithVectors(WithVectorsSelectorFactory.enable(false));

            if (filter != null) builder.setFilter(filter);
            if (offset != null) builder.setOffset(offset);

            Points.ScrollResponse response = await(qdrantClient.scrollAsync(builder.build(), REQUEST_TIMEOUT));

            for (Points.RetrievedPoint point : response.getResultList()) result.add(point.getId());

            if (!response.hasNextPageOffset()) break;

            offset = response.getNextPageOffset();
        }

        return List.copyOf(result);
    }

    /**
     * Qdrant Named Dense Vector를 float[]로 변환합니다.
     */
    private float[] readDenseVector(Points.VectorsOutput vectors) {

        if (vectors == null) throw new IllegalStateException("Qdrant vector output must not be null");

        Points.VectorOutput vectorOutput = null;

        if (vectors.hasVectors()) vectorOutput = vectors.getVectors().getVectorsMap().get(DENSE_VECTOR_NAME);
        if (vectorOutput == null && vectors.hasVector()) vectorOutput = vectors.getVector();

        if (vectorOutput == null) throw new IllegalStateException("Qdrant dense vector not found: " + DENSE_VECTOR_NAME);

        Points.DenseVector denseVector = VectorOutputHelper.getDenseVector(vectorOutput);

        if (denseVector == null || denseVector.getDataCount() == 0) throw new IllegalStateException("Qdrant dense vector is empty: " + DENSE_VECTOR_NAME);

        float[] result = new float[denseVector.getDataCount()];

        for (int index = 0; index < result.length; index++) result[index] = denseVector.getData(index);

        return result;
    }

    /**
     * 특정 Filter의 Point 수를 계산한 뒤 삭제합니다.
     */
    private int deleteByFilter(Common.Filter filter) throws VectorStoreException {

        try {

            long count = await(qdrantClient.countAsync(collectionName, filter, true, REQUEST_TIMEOUT));

            if (count == 0) return 0;

            await(qdrantClient.deleteAsync(collectionName, filter, REQUEST_TIMEOUT));

            return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;

        } catch (Exception exception) {

            throw new VectorStoreException("Failed to delete Qdrant vector records", VectorStoreOperation.DELETE, exception);
        }
    }

    /**
     * 특정 Filter의 Point 수를 반환합니다.
     */
    private long countByFilter(Common.Filter filter) throws VectorStoreException {

        try {

            return await(qdrantClient.countAsync(collectionName, filter, true, REQUEST_TIMEOUT));

        } catch (Exception exception) {

            throw new VectorStoreException("Failed to count Qdrant vector records", VectorStoreOperation.COUNT, exception);
        }
    }

    /**
     * 여러 Must 조건으로 Qdrant Filter를 생성합니다.
     */
    private static Common.Filter filter(Common.Condition... conditions) {

        Common.Filter.Builder builder = Common.Filter.newBuilder();

        for (Common.Condition condition : conditions) {

            if (condition != null) builder.addMust(condition);
        }

        return builder.build();
    }

    /**
     * projectId + recordId + model 기반 deterministic Point ID를 생성합니다.
     */
    private static Common.PointId createPointId(String projectId, String recordId, String model) {

        String identity = RagUtil.requireProjectId(projectId) + '\n' + RagUtil.requireText(recordId, "recordId") + '\n' + RagUtil.requireText(model, "model");

        UUID uuid = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));

        return id(uuid);
    }

    /**
     * DocumentChunk metadata key를 Qdrant Payload field로 변환합니다.
     *
     * <p>Metadata key 내부의 점, 공백 등과 Qdrant field path 문법이 충돌하지 않도록
     * URL-safe Base64로 encoding합니다.</p>
     */
    private static String metadataField(String key) {

        String normalizedKey = RagUtil.requireText(key, "metadataKey");

        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(normalizedKey.getBytes(StandardCharsets.UTF_8));

        return METADATA_PREFIX + encoded;
    }

    /**
     * Qdrant Payload에서 DocumentChunk metadata를 복원합니다.
     */
    private static Map<String, String> readMetadata(Map<String, JsonWithInt.Value> payload) {

        Map<String, String> metadata = new LinkedHashMap<>();

        for (Map.Entry<String, JsonWithInt.Value> entry : payload.entrySet()) {

            if (!entry.getKey().startsWith(METADATA_PREFIX)) continue;

            String encoded = entry.getKey().substring(METADATA_PREFIX.length());

            String key = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);

            metadata.put(key, entry.getValue().getStringValue());
        }

        return metadata;
    }

    /**
     * VectorRecord 저장 전 필수 값을 검증합니다.
     */
    private static void validateRecord(VectorRecord record) {

        if (record == null) throw new IllegalArgumentException("record must not be null");

        RagUtil.requireProjectId(record.getProjectId());
        RagUtil.requireText(record.getId(), "id");
        RagUtil.requireText(record.getModel(), "model");

        if (record.getChunk() == null) throw new IllegalArgumentException("record.chunk must not be null");
        if (record.getVector().length == 0) throw new IllegalArgumentException("record.vector must not be empty");
    }

    /**
     * Qdrant 검색 조건을 검증합니다.
     */
    private static void validateSearchRequest(VectorSearchRequest request) {

        if (request == null) throw new IllegalArgumentException("request must not be null");

        RagUtil.requireProjectId(request.getProjectId());

        if (request.getSimilarityType() != VectorSimilarityType.COSINE) throw new IllegalArgumentException("QdrantVectorStore V1 supports COSINE similarity only");
    }

    /**
     * sourcePath를 GomsBook 표준 형식으로 정규화합니다.
     */
    private static String requireSourcePath(String sourcePath) {

        String normalized = RagUtil.normalizeDocumentPath(sourcePath);

        if (normalized == null || normalized.isBlank()) throw new IllegalArgumentException("sourcePath must not be blank");

        return normalized;
    }

    /**
     * 언어 코드를 검색 가능한 공통 형식으로 변환합니다.
     */
    private static String normalizeLanguage(String value) {

        String normalized = normalize(value);

        return normalized.replace('_', '-').toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {

        return value == null ? "" : value.trim();
    }

    /**
     * 필수 문자열 Payload를 읽습니다.
     */
    private static String requirePayloadText(Map<String, JsonWithInt.Value> payload, String key) {

        String value = payloadText(payload, key);

        if (value.isBlank()) throw new IllegalStateException("Qdrant payload field must not be blank: " + key);

        return value;
    }

    /**
     * 문자열 Payload를 읽습니다.
     */
    private static String payloadText(Map<String, JsonWithInt.Value> payload, String key) {

        JsonWithInt.Value value = payload.get(key);

        return value == null ? "" : value.getStringValue();
    }

    /**
     * 정수 Payload를 읽습니다.
     */
    private static long payloadLong(Map<String, JsonWithInt.Value> payload, String key) {

        JsonWithInt.Value value = payload.get(key);

        if (value == null) throw new IllegalStateException("Qdrant payload field not found: " + key);

        return value.getIntegerValue();
    }

    /**
     * Boolean Payload를 읽습니다.
     */
    private static boolean payloadBoolean(Map<String, JsonWithInt.Value> payload, String key) {

        JsonWithInt.Value value = payload.get(key);

        if (value == null) throw new IllegalStateException("Qdrant payload field not found: " + key);

        return value.getBoolValue();
    }

    /**
     * Qdrant 비동기 요청 완료를 기다립니다.
     */
    private static <T> T await(ListenableFuture<T> future) throws Exception {

        try {

            return future.get(REQUEST_TIMEOUT.toMillis() + 1000L, TimeUnit.MILLISECONDS);

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw exception;
        }
    }

    /**
     * 저장소가 종료되지 않았는지 확인합니다.
     */
    private void ensureOpen(VectorStoreOperation operation) throws VectorStoreException {

        if (closed.get()) throw new VectorStoreException("Qdrant vector store is already closed", operation);
    }

    /**
     * VectorStoreException을 생성합니다.
     */
    private static VectorStoreException failure(String message, VectorStoreOperation operation, String id, String model, Exception exception) {

        return new VectorStoreException(message, operation, id, model, exception);
    }
}