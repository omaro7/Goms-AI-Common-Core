/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.rag.eval.model.RagRetrievalHit;
import kr.co.goms.ai.rag.eval.model.RagRetrievalResult;
import kr.co.goms.ai.rag.retrieval.RetrievalResult;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * Retriever 검색 결과를 RAG 평가용 검색 결과로 변환합니다.
 */
public final class RagRetrievalResultMapper {

	public RagRetrievalResult map(RetrievalResult retrievalResult) {

		Objects.requireNonNull(retrievalResult, "retrievalResult must not be null");

		List<RagRetrievalHit> hits = new ArrayList<>();

		for (VectorSearchResult searchResult : retrievalResult.getSearchResults()) {
			hits.add(toHit(searchResult));
		}

		return new RagRetrievalResult(retrievalResult.getQuery(), hits);
	}

	private RagRetrievalHit toHit(VectorSearchResult searchResult) {

		Objects.requireNonNull(searchResult, "searchResult must not be null");

		String source = normalizeSource(searchResult.getChunk().getSourcePath());

		if (source.isBlank()) throw new IllegalStateException("RAG retrieval sourcePath must not be blank.");

		if (searchResult.getRank() <= 0) throw new IllegalStateException("RAG retrieval rank must be greater than zero.");

		return new RagRetrievalHit(source, searchResult.getRank(), searchResult.getScore());
	}

	private String normalizeSource(String source) {

		if (source == null) return "";

		return source.trim().replace('\\', '/');
	}
}