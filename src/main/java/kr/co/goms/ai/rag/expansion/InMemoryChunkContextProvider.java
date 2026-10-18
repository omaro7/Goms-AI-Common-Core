/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.expansion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import kr.co.goms.ai.rag.model.DocumentChunk;

/**
 * Context Expansion을 위해 DocumentChunk를 메모리에서 조회하는
 * ChunkContextProvider 구현체입니다.
 *
 * DocumentChunk에는 projectId가 없으므로 Provider가
 * projectId별로 Chunk를 분리하여 관리합니다.
 */
public final class InMemoryChunkContextProvider implements ChunkContextProvider {

    private final Map<String, List<DocumentChunk>> chunksByProject = new ConcurrentHashMap<>();

    @Override
    public List<DocumentChunk> findBySource(String projectId, String sourcePath) {

        String normalizedProjectId = requireText(projectId, "projectId");
        String normalizedSourcePath = requireText(sourcePath, "sourcePath");

        List<DocumentChunk> projectChunks = chunksByProject.get(normalizedProjectId);

        if (projectChunks == null || projectChunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<DocumentChunk> result = new ArrayList<>();

        for (DocumentChunk chunk : projectChunks) {

            if (chunk == null) {
                continue;
            }

            if (sameSource(chunk.getSourcePath(), normalizedSourcePath)) {
                result.add(chunk);
            }
        }

        result.sort(Comparator.comparingInt(DocumentChunk::getSequence));

        return Collections.unmodifiableList(result);
    }

    /**
     * 프로젝트의 전체 Chunk를 교체합니다.
     */
    public void replaceProjectChunks(String projectId, Collection<DocumentChunk> chunks) {

        String normalizedProjectId = requireText(projectId, "projectId");

        if (chunks == null || chunks.isEmpty()) {
            chunksByProject.remove(normalizedProjectId);
            return;
        }

        List<DocumentChunk> copy = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {

            if (chunk != null) {
                copy.add(chunk);
            }
        }

        copy.sort(
            Comparator.comparing(DocumentChunk::getSourcePath)
                .thenComparingInt(DocumentChunk::getSequence)
        );

        chunksByProject.put(normalizedProjectId, Collections.unmodifiableList(copy));
    }

    /**
     * 프로젝트에 Chunk를 추가합니다.
     */
    public void addChunks(String projectId, Collection<DocumentChunk> chunks) {

        String normalizedProjectId = requireText(projectId, "projectId");

        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        chunksByProject.compute(normalizedProjectId, (key, existing) -> {

            List<DocumentChunk> merged = new ArrayList<>();

            if (existing != null) {
                merged.addAll(existing);
            }

            for (DocumentChunk chunk : chunks) {

                if (chunk != null) {
                    merged.add(chunk);
                }
            }

            merged.sort(
                Comparator.comparing(DocumentChunk::getSourcePath)
                    .thenComparingInt(DocumentChunk::getSequence)
            );

            return Collections.unmodifiableList(merged);
        });
    }

    /**
     * 특정 sourcePath에 해당하는 모든 Chunk를 제거합니다.
     */
    public void removeBySource(String projectId, String sourcePath) {

        String normalizedProjectId = requireText(projectId, "projectId");
        String normalizedSourcePath = requireText(sourcePath, "sourcePath");

        chunksByProject.computeIfPresent(normalizedProjectId, (key, existing) -> {

            List<DocumentChunk> remaining = new ArrayList<>();

            for (DocumentChunk chunk : existing) {

                if (!sameSource(chunk.getSourcePath(), normalizedSourcePath)) {
                    remaining.add(chunk);
                }
            }

            if (remaining.isEmpty()) {
                return null;
            }

            return Collections.unmodifiableList(remaining);
        });
    }

    /**
     * 특정 프로젝트의 모든 Chunk를 제거합니다.
     */
    public void removeProject(String projectId) {

        String normalizedProjectId = requireText(projectId, "projectId");

        chunksByProject.remove(normalizedProjectId);
    }

    /**
     * 전체 Chunk를 제거합니다.
     */
    public void clear() {
        chunksByProject.clear();
    }

    /**
     * 프로젝트가 등록되어 있는지 확인합니다.
     */
    public boolean containsProject(String projectId) {

        String normalizedProjectId = requireText(projectId, "projectId");

        return chunksByProject.containsKey(normalizedProjectId);
    }

    /**
     * 프로젝트에 저장된 전체 Chunk 개수를 반환합니다.
     */
    public int size(String projectId) {

        String normalizedProjectId = requireText(projectId, "projectId");

        List<DocumentChunk> chunks = chunksByProject.get(normalizedProjectId);

        return chunks == null ? 0 : chunks.size();
    }

    private static boolean sameSource(String left, String right) {

        if (left == null || right == null) {
            return false;
        }

        return normalizePath(left).equals(normalizePath(right));
    }

    private static String normalizePath(String path) {
        return path.trim().replace('\\', '/');
    }

    private static String requireText(String value, String fieldName) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value.trim();
    }
}