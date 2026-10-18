/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.context;

import java.util.Objects;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.model.DocumentChunkType;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * RagContext에 포함되는 검색 출처 정보입니다.
 */
public final class RagSource {

    private final int index;
    private final DocumentChunk chunk;
    private final double score;
    private final int rank;

    private RagSource(
        int index,
        DocumentChunk chunk,
        double score,
        int rank
    ) {
        if (index < 1) {
            throw new IllegalArgumentException(
                "index must be greater than zero"
            );
        }

        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException(
                "score must be finite"
            );
        }

        if (rank < 0) {
            throw new IllegalArgumentException(
                "rank must be greater than or equal to zero"
            );
        }

        this.index = index;

        this.chunk = Objects.requireNonNull(
            chunk,
            "chunk must not be null"
        );

        this.score = score;
        this.rank = rank;
    }

    public static RagSource from(
        int index,
        VectorSearchResult searchResult
    ) {
        Objects.requireNonNull(
            searchResult,
            "searchResult must not be null"
        );

        return new RagSource(
            index,
            searchResult.getChunk(),
            searchResult.getScore(),
            searchResult.getRank()
        );
    }

    public int getIndex() {
        return index;
    }

    public DocumentChunk getChunk() {
        return chunk;
    }

    public String getChunkId() {
        return chunk.getId();
    }

    public String getSourcePath() {
        return chunk.getSourcePath();
    }

    public String getTitle() {
        return chunk.getTitle();
    }

    public String getElementId() {
        return chunk.getElementId();
    }

    public DocumentChunkType getChunkType() {
        return chunk.getType();
    }

    public String getContent() {
        return chunk.getContent();
    }

    public double getScore() {
        return score;
    }

    public int getRank() {
        return rank;
    }

    public boolean hasRank() {
        return rank > 0;
    }

    @Override
    public String toString() {
        return "RagSource{" +
            "index=" + index +
            ", chunkId='" + chunk.getId() + '\'' +
            ", sourcePath='" + chunk.getSourcePath() + '\'' +
            ", chunkType=" + chunk.getType() +
            ", score=" + score +
            ", rank=" + rank +
            '}';
    }
}