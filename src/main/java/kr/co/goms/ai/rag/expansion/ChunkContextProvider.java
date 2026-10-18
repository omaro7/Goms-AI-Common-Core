/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.expansion;

import java.util.Collection;
import java.util.List;

import kr.co.goms.ai.rag.model.DocumentChunk;

/**
 * Context Expansion에 필요한 원본 DocumentChunk를 제공합니다.
 *
 * 동일 프로젝트와 동일 sourcePath에 속하는 chunk들을
 * 원본 문서 순서(sequence)대로 조회하는 역할을 담당합니다.
 */

public interface ChunkContextProvider {

    List<DocumentChunk> findBySource(String projectId, String sourcePath);

    void replaceProjectChunks(String projectId, Collection<DocumentChunk> chunks);

    void addChunks(String projectId, Collection<DocumentChunk> chunks);

    void removeBySource(String projectId, String sourcePath);

    void removeProject(String projectId);
}