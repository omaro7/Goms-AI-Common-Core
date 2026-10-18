/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.vector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 문서 임베딩 벡터를 저장하고 검색하는 저장소 인터페이스입니다.
 *
 * <pre>
 * DocumentChunk
 *      ↓ embedding
 * VectorRecord
 *      ↓ save
 * VectorStore
 *
 * query text
 *      ↓ embedding
 * VectorSearchRequest
 *      ↓ search
 * List&lt;VectorSearchResult&gt;
 * </pre>
 *
 * <p>projectId는 데이터베이스 UUID가 아니라 GomsBook 프로젝트의
 * 안정적인 논리 식별자를 사용합니다.</p>
 *
 * <p>예: {@code lunchwork_seoul}</p>
 */
public interface VectorStore extends AutoCloseable {

    void save(VectorRecord record) throws VectorStoreException;

    default void saveAll(List<VectorRecord> records) throws VectorStoreException {

        if (records == null || records.isEmpty()) return;

        for (VectorRecord record : records) {

            if (record == null) continue;

            save(record);
        }
    }

    List<VectorSearchResult> search(VectorSearchRequest request) throws VectorStoreException;

    Optional<VectorRecord> findById(String projectId, String id, String model) throws VectorStoreException;

    default Optional<VectorRecord> findById(String projectId, String id) throws VectorStoreException {

        validateText(projectId, "projectId");
        validateText(id, "id");

        List<VectorRecord> records = findByIds(projectId, List.of(id));

        if (records.isEmpty()) return Optional.empty();

        return Optional.of(records.get(0));
    }

    default List<VectorRecord> findByIds(String projectId, List<String> ids) throws VectorStoreException {

        String normalizedProjectId = validateText(projectId, "projectId");

        if (ids == null || ids.isEmpty()) return List.of();

        Set<String> normalizedIds = new LinkedHashSet<>();

        for (String id : ids) {

            if (id != null && !id.isBlank()) normalizedIds.add(id.trim());
        }

        if (normalizedIds.isEmpty()) return List.of();

        List<VectorRecord> result = new ArrayList<>();

        for (VectorRecord record : findAll()) {

            if (record == null) continue;
            if (!record.isProject(normalizedProjectId)) continue;
            if (!normalizedIds.contains(record.getId())) continue;

            result.add(record);
        }

        return List.copyOf(result);
    }

    List<VectorRecord> findAll() throws VectorStoreException;

    default List<VectorRecord> findByModel(String model) throws VectorStoreException {

        String normalizedModel = validateText(model, "model");

        return findAll().stream()
            .filter(record -> record != null && record.isModel(normalizedModel))
            .toList();
    }

    default List<VectorRecord> findByProjectAndModel(String projectId, String model) throws VectorStoreException {

        String normalizedProjectId = validateText(projectId, "projectId");
        String normalizedModel = validateText(model, "model");

        return findAll().stream()
            .filter(record -> record != null && record.isProject(normalizedProjectId))
            .filter(record -> record.isModel(normalizedModel))
            .toList();
    }

    default List<VectorRecord> findBySourcePath(String sourcePath) throws VectorStoreException {

        String normalizedPath = normalizePath(validateText(sourcePath, "sourcePath"));

        return findAll().stream()
            .filter(record -> record != null && record.getChunk() != null)
            .filter(record -> normalizePath(record.getChunk().getSourcePath()).equals(normalizedPath))
            .toList();
    }

    default List<VectorRecord> findByProjectAndSourcePath(String projectId, String sourcePath) throws VectorStoreException {

        String normalizedProjectId = validateText(projectId, "projectId");
        String normalizedPath = normalizePath(validateText(sourcePath, "sourcePath"));

        return findAll().stream()
            .filter(record -> record != null && record.isProject(normalizedProjectId))
            .filter(record -> record.getChunk() != null)
            .filter(record -> normalizePath(record.getChunk().getSourcePath()).equals(normalizedPath))
            .toList();
    }

    default boolean contains(String projectId, String id, String model) throws VectorStoreException {

        return findById(projectId, id, model).isPresent();
    }

    boolean delete(String projectId, String id, String model) throws VectorStoreException;

    default int deleteAll(String projectId, List<String> ids, String model) throws VectorStoreException {

        String normalizedProjectId = validateText(projectId, "projectId");
        String normalizedModel = validateText(model, "model");

        if (ids == null || ids.isEmpty()) return 0;

        int deletedCount = 0;

        for (String id : ids) {

            if (id == null || id.isBlank()) continue;

            if (delete(normalizedProjectId, id.trim(), normalizedModel)) deletedCount++;
        }

        return deletedCount;
    }

    /**
     * 모든 프로젝트에서 sourcePath에 해당하는 벡터를 삭제합니다.
     *
     * <p>일반 프로젝트 인덱싱에서는 사용하지 않고 전체 관리 작업에만 사용합니다.</p>
     */
    int deleteBySourcePath(String sourcePath) throws VectorStoreException;

    /**
     * 기존 호출부 호환용 API입니다.
     *
     * <p>실제 처리는 projectId가 명확한
     * {@link #deleteByProjectAndSourcePath(String, String, String)}에 위임합니다.</p>
     */
    default int deleteBySourcePath(String projectId, String sourcePath, String model) throws VectorStoreException {

        return deleteByProjectAndSourcePath(projectId, sourcePath, model);
    }

    default int deleteByProjectAndSourcePath(String projectId, String sourcePath, String model) throws VectorStoreException {

        String normalizedProjectId = validateText(projectId, "projectId");
        String normalizedPath = normalizePath(validateText(sourcePath, "sourcePath"));
        String normalizedModel = validateText(model, "model");

        List<VectorRecord> records = findByProjectAndSourcePath(normalizedProjectId, normalizedPath);

        int deletedCount = 0;

        for (VectorRecord record : records) {

            if (record == null) continue;
            if (!record.isModel(normalizedModel)) continue;

            if (delete(normalizedProjectId, record.getId(), normalizedModel)) deletedCount++;
        }

        return deletedCount;
    }

    /**
     * 모든 프로젝트에서 특정 모델의 벡터를 삭제합니다.
     *
     * <p>임베딩 모델 전체 초기화와 같은 관리 작업에 사용합니다.</p>
     */
    int deleteByModel(String model) throws VectorStoreException;

    void clear() throws VectorStoreException;

    long count() throws VectorStoreException;

    default long countByModel(String model) throws VectorStoreException {

        return findByModel(model).size();
    }

    default long countBySourcePath(String sourcePath) throws VectorStoreException {

        return findBySourcePath(sourcePath).size();
    }

    default boolean isEmpty() throws VectorStoreException {

        return count() == 0;
    }

    default boolean isAvailable() {

        return true;
    }

    @Override
    default void close() throws VectorStoreException {

    }

    private static String validateText(String value, String fieldName) {

        String normalized = value == null ? "" : value.trim();

        if (normalized.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");

        return normalized;
    }

    private static String normalizePath(String value) {

        return value == null ? "" : value.trim().replace('\\', '/');
    }
}