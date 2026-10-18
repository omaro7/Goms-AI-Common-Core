/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 메모리 기반 {@link VectorStore} 구현체입니다.
 *
 * <p>모든 {@link VectorRecord}를 애플리케이션 메모리에 저장하고,
 * 검색 시 저장된 레코드를 순차적으로 비교하여 Vector Similarity를 계산합니다.</p>
 *
 * <p>영속 저장소가 아니므로 애플리케이션 종료 시 저장된 Vector 데이터는
 * 모두 사라집니다.</p>
 *
 * <h2>주요 용도</h2>
 *
 * <ul>
 *     <li>Golden Dataset Vector Baseline</li>
 *     <li>RAG Retrieval 기능 검증</li>
 *     <li>Vector Search 단위 테스트</li>
 *     <li>QdrantVectorStore 비교 실험</li>
 * </ul>
 *
 * <pre>
 * DocumentChunk
 *      ↓
 * EmbeddingClient
 *      ↓
 * VectorRecord
 *      ↓
 * InMemoryVectorStore
 *      ↓
 * VectorSearchRequest
 *      ↓
 * VectorSearchResult
 * </pre>
 *
 * <h2>Project ID 정책</h2>
 *
 * <p>{@code projectId}는 데이터베이스 UUID가 아니라
 * GomsBook 프로젝트의 안정적인 논리 식별자를 사용합니다.</p>
 *
 * <pre>
 * projectId = lunchwork_seoul
 * projectId = epub-ai-agent
 * projectId = season
 * </pre>
 *
 * <p>서로 다른 프로젝트에서 동일한 Chunk ID가 존재할 수 있으므로
 * 내부 저장 Key는 다음 조합을 사용합니다.</p>
 *
 * <pre>
 * projectId
 *      +
 * recordId
 *      +
 * embeddingModel
 *      ↓
 * RecordKey
 * </pre>
 *
 * <h2>곰스북 1줄 원칙</h2>
 *
 * <p>메서드 선언, 단순 조건문, 단순 변수 선언, 단순 return 및
 * 단순 메서드 호출은 가능한 한 한 줄로 작성합니다.</p>
 *
 * <p>Stream, Builder, 복합 검색 로직처럼 여러 단계의 의미를 표현해야 하는 경우에는
 * 가독성을 유지하기 위해 의미 단위로 줄을 나눕니다.</p>
 */
public final class InMemoryVectorStore implements VectorStore {

    /**
     * Vector Record 저장소입니다.
     *
     * <p>Thread-safe한 {@link ConcurrentHashMap}을 사용하며
     * {@link RecordKey}를 기준으로 VectorRecord를 관리합니다.</p>
     */
    private final Map<RecordKey, VectorRecord> records = new ConcurrentHashMap<>();

    /**
     * Vector Store 종료 상태입니다.
     *
     * <p>true가 되면 저장, 검색, 조회, 삭제 등의 모든 작업을 허용하지 않습니다.</p>
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * VectorRecord 하나를 저장합니다.
     *
     * <p>동일한 projectId + recordId + model 조합의 데이터가 이미 존재하면
     * 기존 레코드를 새로운 레코드로 대체합니다.</p>
     *
     * @param record 저장할 벡터 레코드
     * @throws VectorStoreException 저장 실패 시
     */
    @Override
    public void save(VectorRecord record) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.SAVE);

        if (record == null) throw new IllegalArgumentException("record must not be null");

        try {

            RecordKey key = RecordKey.of(record.getProjectId(), record.getId(), record.getModel());

            records.put(key, record);

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to save vector record", VectorStoreOperation.SAVE, record.getId(), record.getModel(), exception);
        }
    }

    /**
     * 여러 VectorRecord를 한 번에 저장합니다.
     *
     * <p>각 레코드의 projectId + recordId + model 조합을 기준으로 저장하며,
     * 동일 Key가 이미 존재하면 기존 값을 대체합니다.</p>
     *
     * @param records 저장할 벡터 레코드 목록
     * @throws VectorStoreException 저장 실패 시
     */
    @Override
    public void saveAll(List<VectorRecord> records) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.SAVE_ALL);

        if (records == null || records.isEmpty()) return;

        try {

            for (VectorRecord record : records) {

                if (record == null) continue;

                RecordKey key = RecordKey.of(record.getProjectId(), record.getId(), record.getModel());

                this.records.put(key, record);
            }

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to save vector records", VectorStoreOperation.SAVE_ALL, exception);
        }
    }

    /**
     * Query Vector를 이용해 유사 Vector를 검색합니다.
     *
     * <p>{@link VectorSearchRequest#matches(VectorRecord)}를 통해 projectId,
     * embedding model, sourcePath, chunkType 등의 검색 조건을 먼저 적용한 후
     * Vector Similarity를 계산합니다.</p>
     *
     * <p>검색 결과는 score가 높은 순서로 정렬하고 rank는 1부터 부여합니다.</p>
     *
     * <pre>
     * queryVector
     *      ↓
     * request.matches()
     *      ↓
     * similarity score
     *      ↓
     * minScore
     *      ↓
     * Top-K
     *      ↓
     * rank
     * </pre>
     *
     * @param request 벡터 검색 요청
     * @return 검색 결과 목록
     * @throws VectorStoreException 검색 실패 시
     */
    @Override
    public List<VectorSearchResult> search(VectorSearchRequest request) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.SEARCH);

        if (request == null) throw new IllegalArgumentException("request must not be null");

        try {

            float[] queryVector = request.getQueryVector();

            List<VectorSearchResult> candidates = new ArrayList<>();

            for (VectorRecord record : records.values()) {

                if (!request.matches(record)) continue;

                double score = calculateSearchScore(queryVector, record, request.getSimilarityType());

                boolean accepted = request.accepts(score);

                if (!accepted && !request.isIncludeRejected()) continue;

                candidates.add(
                    VectorSearchResult.builder()
                        .record(record)
                        .score(score)
                        .similarityType(request.getSimilarityType())
                        .accepted(accepted)
                        .build()
                );
            }

            candidates.sort(
                Comparator
                    .comparingDouble(VectorSearchResult::getScore)
                    .reversed()
                    .thenComparing(VectorSearchResult::getId)
            );

            int resultCount = Math.min(request.getTopK(), candidates.size());

            List<VectorSearchResult> results = new ArrayList<>(resultCount);

            for (int index = 0; index < resultCount; index++) results.add(candidates.get(index).withRank(index + 1));

            return List.copyOf(results);

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to search vector records", VectorStoreOperation.SEARCH, exception);
        }
    }

    /**
     * Query Vector와 저장된 VectorRecord의 검색 점수를 계산합니다.
     *
     * <p>Cosine 검색이고 Query Vector와 Record Vector가 모두 L2 정규화되어 있으면
     * 빠른 normalized cosine 계산을 사용합니다.</p>
     *
     * <p>그 외에는 {@link VectorSimilarity#score(float[], float[], VectorSimilarityType)}
     * 를 통해 일반 Vector Similarity를 계산합니다.</p>
     *
     * @param queryVector 검색 Query Vector
     * @param record 검색 대상 VectorRecord
     * @param similarityType Vector Similarity 방식
     * @return 계산된 검색 점수
     */
    private double calculateSearchScore(float[] queryVector, VectorRecord record, VectorSimilarityType similarityType) {

        float[] recordVector = record.getVector();

        VectorSimilarityType resolvedType = Objects.requireNonNullElse(similarityType, VectorSimilarityType.COSINE);

        if (resolvedType == VectorSimilarityType.COSINE && record.isNormalized() && VectorSimilarity.isNormalized(queryVector, 1.0e-4)) {

            return VectorSimilarity.cosineNormalized(queryVector, recordVector);
        }

        return VectorSimilarity.score(queryVector, recordVector, resolvedType);
    }

    /**
     * 특정 프로젝트의 ID와 Embedding Model로 VectorRecord를 조회합니다.
     *
     * <p>RecordKey가 projectId + id + model로 구성되어 있으므로
     * Map에서 직접 조회할 수 있습니다.</p>
     *
     * <pre>
     * lunchwork_seoul
     * +
     * OEBPS/Text/chapter10_4.xhtml#p_05
     * +
     * nomic-embed-text
     * </pre>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param id 벡터 레코드 ID
     * @param model 임베딩 모델명
     * @return 조회된 VectorRecord
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public Optional<VectorRecord> findById(String projectId, String id, String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = requireText(projectId, "projectId");
        String normalizedId = requireText(id, "id");
        String normalizedModel = requireText(model, "model");

        try {

            RecordKey key = RecordKey.of(normalizedProjectId, normalizedId, normalizedModel);

            return Optional.ofNullable(records.get(key));

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to find vector record", VectorStoreOperation.FIND, normalizedId, normalizedModel, exception);
        }
    }

    /**
     * 특정 프로젝트에서 ID만으로 VectorRecord 하나를 조회합니다.
     *
     * <p>동일한 ID에 여러 Embedding Model이 존재하면 indexedAt이 가장 최근인
     * VectorRecord를 반환합니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param id 벡터 레코드 ID
     * @return 조회된 VectorRecord
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public Optional<VectorRecord> findById(String projectId, String id) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = requireText(projectId, "projectId");
        String normalizedId = requireText(id, "id");

        try {

            return records.values().stream()
                .filter(record -> record.isProject(normalizedProjectId))
                .filter(record -> record.getId().equals(normalizedId))
                .max(Comparator.comparingLong(VectorRecord::getIndexedAt));

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to find vector record by id", VectorStoreOperation.FIND, normalizedId, "", exception);
        }
    }

    /**
     * 특정 프로젝트에서 여러 ID에 해당하는 VectorRecord를 조회합니다.
     *
     * <p>projectId를 반드시 검색 조건으로 사용하여 다른 프로젝트의
     * 동일한 Chunk ID가 결과에 포함되지 않도록 합니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param ids 조회할 벡터 레코드 ID 목록
     * @return 조회된 VectorRecord 목록
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public List<VectorRecord> findByIds(String projectId, List<String> ids) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = requireText(projectId, "projectId");

        if (ids == null || ids.isEmpty()) return List.of();

        try {

            Set<String> normalizedIds = new LinkedHashSet<>();

            for (String id : ids) {

                if (id != null && !id.isBlank()) normalizedIds.add(id.trim());
            }

            if (normalizedIds.isEmpty()) return List.of();

            return records.values().stream()
                .filter(record -> record.isProject(normalizedProjectId))
                .filter(record -> normalizedIds.contains(record.getId()))
                .sorted(Comparator.comparing(VectorRecord::getId).thenComparing(VectorRecord::getModel))
                .toList();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to find vector records by ids", VectorStoreOperation.FIND, exception);
        }
    }

    /**
     * 저장된 모든 VectorRecord를 조회합니다.
     *
     * <p>projectId, recordId, embedding model 순서로 정렬합니다.</p>
     *
     * <p>InMemory 저장소의 테스트 및 상태 점검에 사용하며,
     * QdrantVectorStore에서는 대량 데이터 조회 비용을 고려해야 합니다.</p>
     *
     * @return 전체 VectorRecord 목록
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public List<VectorRecord> findAll() throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        try {

            return records.values().stream()
                .sorted(
                    Comparator
                        .comparing(VectorRecord::getProjectId)
                        .thenComparing(VectorRecord::getId)
                        .thenComparing(VectorRecord::getModel)
                )
                .toList();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to find all vector records", VectorStoreOperation.FIND, exception);
        }
    }

    /**
     * 특정 Embedding Model로 생성된 모든 프로젝트의 VectorRecord를 조회합니다.
     *
     * <p>projectId 범위가 없는 관리용 조회 API입니다.</p>
     *
     * @param model 임베딩 모델명
     * @return 모델별 VectorRecord 목록
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public List<VectorRecord> findByModel(String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedModel = requireText(model, "model");

        try {

            return records.values().stream()
                .filter(record -> record.isModel(normalizedModel))
                .sorted(Comparator.comparing(VectorRecord::getProjectId).thenComparing(VectorRecord::getId))
                .toList();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to find vector records by model", VectorStoreOperation.FIND, "", normalizedModel, exception);
        }
    }

    /**
     * 특정 프로젝트와 Embedding Model에 해당하는 VectorRecord를 조회합니다.
     *
     * <p>프로젝트 재인덱싱 또는 기존 Vector 인덱스 상태 확인에 사용합니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param model 임베딩 모델명
     * @return 프로젝트/모델별 VectorRecord 목록
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public List<VectorRecord> findByProjectAndModel(String projectId, String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = requireText(projectId, "projectId");
        String normalizedModel = requireText(model, "model");

        try {

            return records.values().stream()
                .filter(record -> record.isProject(normalizedProjectId))
                .filter(record -> record.isModel(normalizedModel))
                .sorted(Comparator.comparing(VectorRecord::getId))
                .toList();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to find vector records by project and model", VectorStoreOperation.FIND, "", normalizedModel, exception);
        }
    }

    /**
     * 특정 sourcePath에서 생성된 모든 프로젝트의 VectorRecord를 조회합니다.
     *
     * <p>projectId 범위가 없으므로 관리용 조회에 사용합니다.</p>
     *
     * @param sourcePath 원본 문서 상대 경로
     * @return 원본 문서별 VectorRecord 목록
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public List<VectorRecord> findBySourcePath(String sourcePath) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedPath = normalizePath(requireText(sourcePath, "sourcePath"));

        try {

            return records.values().stream()
                .filter(record -> normalizePath(record.getChunk().getSourcePath()).equals(normalizedPath))
                .sorted(
                    Comparator
                        .comparing(VectorRecord::getProjectId)
                        .thenComparingInt(record -> record.getChunk().getSequence())
                        .thenComparing(VectorRecord::getId)
                )
                .toList();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to find vector records by source path", VectorStoreOperation.FIND, exception);
        }
    }

    /**
     * 특정 프로젝트와 sourcePath에 해당하는 VectorRecord를 조회합니다.
     *
     * <p>파일 변경 감지, 재인덱싱, 기존 Vector 제거 등에 사용하는
     * 프로젝트 기준 표준 조회 API입니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param sourcePath 원본 문서 상대 경로
     * @return 프로젝트/문서별 VectorRecord 목록
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public List<VectorRecord> findByProjectAndSourcePath(String projectId, String sourcePath) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.FIND);

        String normalizedProjectId = requireText(projectId, "projectId");
        String normalizedPath = normalizePath(requireText(sourcePath, "sourcePath"));

        try {

            return records.values().stream()
                .filter(record -> record.isProject(normalizedProjectId))
                .filter(record -> normalizePath(record.getChunk().getSourcePath()).equals(normalizedPath))
                .sorted(
                    Comparator
                        .comparingInt((VectorRecord record) -> record.getChunk().getSequence())
                        .thenComparing(VectorRecord::getId)
                )
                .toList();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to find vector records by project and source path", VectorStoreOperation.FIND, exception);
        }
    }

    /**
     * 특정 프로젝트의 VectorRecord 하나를 삭제합니다.
     *
     * <p>projectId + recordId + model 조합으로 RecordKey를 생성하여
     * 정확히 하나의 VectorRecord를 삭제합니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param id 벡터 레코드 ID
     * @param model 임베딩 모델명
     * @return 실제 삭제된 경우 true
     * @throws VectorStoreException 삭제 실패 시
     */
    @Override
    public boolean delete(String projectId, String id, String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.DELETE);

        String normalizedProjectId = requireText(projectId, "projectId");
        String normalizedId = requireText(id, "id");
        String normalizedModel = requireText(model, "model");

        try {

            RecordKey key = RecordKey.of(normalizedProjectId, normalizedId, normalizedModel);

            return records.remove(key) != null;

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to delete vector record", VectorStoreOperation.DELETE, normalizedId, normalizedModel, exception);
        }
    }

    /**
     * 모든 프로젝트에서 특정 sourcePath의 VectorRecord를 삭제합니다.
     *
     * <p>projectId 범위가 없는 전체 관리용 API입니다.</p>
     *
     * @param sourcePath 원본 문서 상대 경로
     * @return 삭제된 VectorRecord 수
     * @throws VectorStoreException 삭제 실패 시
     */
    @Override
    public int deleteBySourcePath(String sourcePath) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.DELETE);

        String normalizedPath = normalizePath(requireText(sourcePath, "sourcePath"));

        try {

            int deletedCount = 0;

            for (Map.Entry<RecordKey, VectorRecord> entry : records.entrySet()) {

                VectorRecord record = entry.getValue();

                if (!normalizePath(record.getChunk().getSourcePath()).equals(normalizedPath)) continue;

                if (records.remove(entry.getKey(), record)) deletedCount++;
            }

            return deletedCount;

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to delete vector records by source path", VectorStoreOperation.DELETE, exception);
        }
    }

    /**
     * 특정 프로젝트, sourcePath, Embedding Model의 VectorRecord를 삭제합니다.
     *
     * <p>기존 호출부 호환을 위해 유지하며 실제 처리는
     * {@link #deleteByProjectAndSourcePath(String, String, String)}에 위임합니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param sourcePath 원본 문서 상대 경로
     * @param model 임베딩 모델명
     * @return 삭제된 VectorRecord 수
     * @throws VectorStoreException 삭제 실패 시
     */
    @Override
    public int deleteBySourcePath(String projectId, String sourcePath, String model) throws VectorStoreException {

        return deleteByProjectAndSourcePath(projectId, sourcePath, model);
    }

    /**
     * 특정 프로젝트, sourcePath, Embedding Model의 모든 VectorRecord를 삭제합니다.
     *
     * <p>EPUB/XHTML 파일 변경 또는 재인덱싱 과정에서 사용하는
     * 프로젝트 기준 표준 삭제 API입니다.</p>
     *
     * <pre>
     * lunchwork_seoul
     * +
     * OEBPS/Text/chapter10_4.xhtml
     * +
     * nomic-embed-text
     *      ↓
     * 해당 문서 Vector 삭제
     * </pre>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param sourcePath 원본 문서 상대 경로
     * @param model 임베딩 모델명
     * @return 삭제된 VectorRecord 수
     * @throws VectorStoreException 삭제 실패 시
     */
    @Override
    public int deleteByProjectAndSourcePath(String projectId, String sourcePath, String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.DELETE);

        String normalizedProjectId = requireText(projectId, "projectId");
        String normalizedPath = normalizePath(requireText(sourcePath, "sourcePath"));
        String normalizedModel = requireText(model, "model");

        try {

            int deletedCount = 0;

            for (Map.Entry<RecordKey, VectorRecord> entry : records.entrySet()) {

                VectorRecord record = entry.getValue();

                if (!record.isProject(normalizedProjectId)) continue;
                if (!record.isModel(normalizedModel)) continue;
                if (!normalizePath(record.getChunk().getSourcePath()).equals(normalizedPath)) continue;

                if (records.remove(entry.getKey(), record)) deletedCount++;
            }

            return deletedCount;

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to delete vector records by project and source path", VectorStoreOperation.DELETE, "", normalizedModel, exception);
        }
    }

    /**
     * 모든 프로젝트에서 특정 Embedding Model의 VectorRecord를 삭제합니다.
     *
     * <p>Embedding Model 교체 또는 전체 모델 인덱스 초기화 시 사용하는
     * 관리용 API입니다.</p>
     *
     * @param model 임베딩 모델명
     * @return 삭제된 VectorRecord 수
     * @throws VectorStoreException 삭제 실패 시
     */
    @Override
    public int deleteByModel(String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.DELETE);

        String normalizedModel = requireText(model, "model");

        try {

            int deletedCount = 0;

            for (Map.Entry<RecordKey, VectorRecord> entry : records.entrySet()) {

                VectorRecord record = entry.getValue();

                if (!record.isModel(normalizedModel)) continue;

                if (records.remove(entry.getKey(), record)) deletedCount++;
            }

            return deletedCount;

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to delete vector records by model", VectorStoreOperation.DELETE, "", normalizedModel, exception);
        }
    }

    /**
     * 저장된 모든 VectorRecord를 삭제합니다.
     *
     * <p>모든 프로젝트와 모든 Embedding Model의 데이터가 삭제되므로
     * 테스트 초기화나 명시적인 관리 작업에서만 사용합니다.</p>
     *
     * @throws VectorStoreException 초기화 실패 시
     */
    @Override
    public void clear() throws VectorStoreException {

        ensureOpen(VectorStoreOperation.CLEAR);

        try {

            records.clear();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to clear vector store", VectorStoreOperation.CLEAR, exception);
        }
    }

    /**
     * 저장된 전체 VectorRecord 수를 반환합니다.
     *
     * @return 전체 VectorRecord 수
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public long count() throws VectorStoreException {

        ensureOpen(VectorStoreOperation.COUNT);

        return records.size();
    }

    /**
     * 특정 Embedding Model로 생성된 전체 VectorRecord 수를 반환합니다.
     *
     * <p>모든 프로젝트가 집계 대상입니다.</p>
     *
     * @param model 임베딩 모델명
     * @return 모델별 VectorRecord 수
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public long countByModel(String model) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.COUNT);

        String normalizedModel = requireText(model, "model");

        try {

            return records.values().stream().filter(record -> record.isModel(normalizedModel)).count();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to count vector records by model", VectorStoreOperation.COUNT, "", normalizedModel, exception);
        }
    }

    /**
     * 특정 sourcePath에서 생성된 전체 VectorRecord 수를 반환합니다.
     *
     * <p>projectId 범위가 없으므로 여러 프로젝트의 동일 sourcePath가
     * 모두 집계될 수 있습니다.</p>
     *
     * @param sourcePath 원본 문서 상대 경로
     * @return sourcePath별 VectorRecord 수
     * @throws VectorStoreException 조회 실패 시
     */
    @Override
    public long countBySourcePath(String sourcePath) throws VectorStoreException {

        ensureOpen(VectorStoreOperation.COUNT);

        String normalizedPath = normalizePath(requireText(sourcePath, "sourcePath"));

        try {

            return records.values().stream()
                .filter(record -> normalizePath(record.getChunk().getSourcePath()).equals(normalizedPath))
                .count();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to count vector records by source path", VectorStoreOperation.COUNT, exception);
        }
    }

    /**
     * 현재 InMemoryVectorStore를 사용할 수 있는지 확인합니다.
     *
     * <p>close()가 호출되지 않은 상태이면 true를 반환합니다.</p>
     *
     * @return 저장소 사용 가능 여부
     */
    @Override
    public boolean isAvailable() {

        return !closed.get();
    }

    /**
     * InMemoryVectorStore를 종료합니다.
     *
     * <p>최초 호출 시 저장된 모든 VectorRecord를 삭제하고 closed 상태로 변경합니다.</p>
     *
     * <p>이미 종료된 경우 추가 작업 없이 반환합니다.</p>
     *
     * @throws VectorStoreException 종료 처리 실패 시
     */
    @Override
    public void close() throws VectorStoreException {

        if (!closed.compareAndSet(false, true)) return;

        try {

            records.clear();

        } catch (RuntimeException exception) {

            throw new VectorStoreException("Failed to close in-memory vector store", VectorStoreOperation.CLOSE, exception);
        }
    }

    /**
     * Vector Store가 종료되지 않았는지 확인합니다.
     *
     * @param operation 현재 수행하려는 Vector Store 작업
     * @throws VectorStoreException 이미 종료된 저장소인 경우
     */
    private void ensureOpen(VectorStoreOperation operation) throws VectorStoreException {

        if (closed.get()) throw new VectorStoreException("Vector store is already closed", operation);
    }

    /**
     * 필수 문자열을 검증하고 trim 처리된 값을 반환합니다.
     *
     * @param value 검증할 문자열
     * @param fieldName 오류 메시지에 사용할 필드명
     * @return 정규화된 문자열
     */
    private static String requireText(String value, String fieldName) {

        String normalized = value == null ? "" : value.trim();

        if (normalized.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");

        return normalized;
    }

    /**
     * 문서 경로를 GomsBook RAG 내부 경로 형식으로 정규화합니다.
     *
     * <p>Windows 역슬래시를 EPUB/RAG 표준 슬래시 형식으로 변환합니다.</p>
     *
     * <pre>
     * OEBPS\Text\chapter10_4.xhtml
     *              ↓
     * OEBPS/Text/chapter10_4.xhtml
     * </pre>
     *
     * @param path 정규화할 문서 경로
     * @return 정규화된 문서 경로
     */
    private static String normalizePath(String path) {

        return path == null ? "" : path.trim().replace('\\', '/');
    }

    /**
     * InMemoryVectorStore 내부에서 VectorRecord를 식별하는 복합 Key입니다.
     *
     * <p>서로 다른 GomsBook 프로젝트에서 동일한 Chunk ID가 존재할 수 있기 때문에
     * projectId + recordId + embedding model 조합을 Key로 사용합니다.</p>
     *
     * <pre>
     * lunchwork_seoul
     * +
     * OEBPS/Text/chapter10_4.xhtml#p_05
     * +
     * nomic-embed-text
     *      ↓
     * RecordKey
     * </pre>
     *
     * <p>이 논리 Key 구조는 이후 QdrantVectorStore에서도 동일하게 유지하며,
     * Qdrant에서는 이 조합을 deterministic Point UUID 생성 기준으로 사용합니다.</p>
     */
    private static final class RecordKey {

        /**
         * GomsBook 논리 프로젝트 ID입니다.
         *
         * <p>DB UUID가 아니라 {@code lunchwork_seoul}과 같은 프로젝트 slug입니다.</p>
         */
        private final String projectId;

        /**
         * VectorRecord ID입니다.
         */
        private final String id;

        /**
         * Vector를 생성한 Embedding Model입니다.
         */
        private final String model;

        /**
         * RecordKey를 생성합니다.
         *
         * @param projectId GomsBook 논리 프로젝트 ID
         * @param id VectorRecord ID
         * @param model Embedding Model
         */
        private RecordKey(String projectId, String id, String model) {

            this.projectId = requireText(projectId, "projectId");
            this.id = requireText(id, "id");
            this.model = requireText(model, "model");
        }

        /**
         * RecordKey Factory Method입니다.
         *
         * @param projectId GomsBook 논리 프로젝트 ID
         * @param id VectorRecord ID
         * @param model Embedding Model
         * @return 생성된 RecordKey
         */
        private static RecordKey of(String projectId, String id, String model) {

            return new RecordKey(projectId, id, model);
        }

        /**
         * projectId + id + model 값이 모두 같은 경우 동일한 RecordKey로 판단합니다.
         *
         * @param object 비교 대상 객체
         * @return 동일한 RecordKey이면 true
         */
        @Override
        public boolean equals(Object object) {

            if (this == object) return true;
            if (!(object instanceof RecordKey)) return false;

            RecordKey other = (RecordKey) object;

            return projectId.equals(other.projectId) && id.equals(other.id) && model.equals(other.model);
        }

        /**
         * projectId + id + model을 이용해 Hash Code를 생성합니다.
         *
         * @return RecordKey Hash Code
         */
        @Override
        public int hashCode() {

            return Objects.hash(projectId, id, model);
        }

        /**
         * 디버깅용 RecordKey 문자열을 반환합니다.
         *
         * <pre>
         * lunchwork_seoul:OEBPS/Text/chapter10_4.xhtml#p_05@nomic-embed-text
         * </pre>
         *
         * @return RecordKey 문자열
         */
        @Override
        public String toString() {

            return projectId + ':' + id + '@' + model;
        }
    }
}