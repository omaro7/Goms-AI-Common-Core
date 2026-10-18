/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.expansion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.retrieval.RetrievedDocument;

/**
 * 검색된 Chunk의 앞뒤 문맥을 확장하는 기본 ContextExpander 구현체입니다.
 *
 * 동일 sourcePath에 속하는 DocumentChunk를 sequence 순으로 조회한 뒤
 * 직접 검색된 Chunk를 기준으로 beforeChunks / afterChunks 범위만큼
 * 인접 Chunk를 추가합니다.
 *
 * 직접 검색된 Chunk의 retrievalScore는 유지하고,
 * 확장된 Chunk에는 parentRetrievalScore와 parentChunkId를 기록합니다.
 */
public final class DefaultContextExpander implements ContextExpander {

    private final ChunkContextProvider chunkContextProvider;

    public DefaultContextExpander(ChunkContextProvider chunkContextProvider) {
        this.chunkContextProvider = Objects.requireNonNull(chunkContextProvider, "chunkContextProvider must not be null");
    }

    @Override
    public List<RetrievedDocument> expand(ContextExpansionRequest request) {

        Objects.requireNonNull(request, "request must not be null");

        List<RetrievedDocument> retrievedDocuments = request.getRetrievedDocuments();

        if (retrievedDocuments == null || retrievedDocuments.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, RetrievedDocument> expandedDocuments = new LinkedHashMap<>();

        for (RetrievedDocument retrievedDocument : retrievedDocuments) {

            if (retrievedDocument == null) {
                continue;
            }

            addExpandedContext(
                request.getProjectId(),
                retrievedDocument,
                request.getBeforeChunks(),
                request.getAfterChunks(),
                expandedDocuments
            );
        }

        return Collections.unmodifiableList(new ArrayList<>(expandedDocuments.values()));
    }

    private void addExpandedContext(
        String projectId,
        RetrievedDocument parent,
        int beforeChunks,
        int afterChunks,
        Map<String, RetrievedDocument> result
    ) {

        List<DocumentChunk> sourceChunks = chunkContextProvider.findBySource(projectId, parent.getSourcePath());

        if (sourceChunks == null || sourceChunks.isEmpty()) {
            addDirectDocument(parent, result);
            return;
        }

        int parentIndex = findParentIndex(sourceChunks, parent);

        if (parentIndex < 0) {
            addDirectDocument(parent, result);
            return;
        }

        int startIndex = Math.max(0, parentIndex - Math.max(0, beforeChunks));
        int endIndex = Math.min(sourceChunks.size() - 1, parentIndex + Math.max(0, afterChunks));

        for (int index = startIndex; index <= endIndex; index++) {

            DocumentChunk chunk = sourceChunks.get(index);

            if (chunk == null) {
                continue;
            }

            if (chunk.getId().equals(parent.getChunkId())) {
                addDirectDocument(parent, result);
                continue;
            }

            addExpandedDocument(parent, chunk, result);
        }
    }

    private int findParentIndex(List<DocumentChunk> chunks, RetrievedDocument parent) {

        for (int index = 0; index < chunks.size(); index++) {

            DocumentChunk chunk = chunks.get(index);

            if (chunk == null) {
                continue;
            }

            if (chunk.getId().equals(parent.getChunkId())) {
                return index;
            }
        }

        /*
         * chunkId가 일치하지 않는 경우 sequence를 보조 기준으로 사용합니다.
         */
        for (int index = 0; index < chunks.size(); index++) {

            DocumentChunk chunk = chunks.get(index);

            if (chunk == null) {
                continue;
            }

            if (chunk.getSequence() == parent.getSequence()) {
                return index;
            }
        }

        return -1;
    }

    private void addDirectDocument(RetrievedDocument document, Map<String, RetrievedDocument> result) {
        result.putIfAbsent(createKey(document.getSourcePath(), document.getChunkId()), document);
    }

    private void addExpandedDocument(
        RetrievedDocument parent,
        DocumentChunk chunk,
        Map<String, RetrievedDocument> result
    ) {

        String key = createKey(chunk.getSourcePath(), chunk.getId());

        if (result.containsKey(key)) {
            return;
        }

        RetrievedDocument expanded = parent.createExpandedDocument(
            chunk.getId(),
            chunk.getSourcePath(),
            chunk.getSequence(),
            chunk.getTitle(),
            chunk.getContent()
        );

        result.put(key, expanded);
    }

    private static String createKey(String sourcePath, String chunkId) {
        return normalizePath(sourcePath) + "|" + chunkId;
    }

    private static String normalizePath(String path) {

        if (path == null) {
            return "";
        }

        return path.trim().replace('\\', '/');
    }
}