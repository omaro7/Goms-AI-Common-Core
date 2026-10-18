/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RagRetrievalResult {

	private final String query;
	private final List<RagRetrievalHit> hits;

	public RagRetrievalResult(String query, List<RagRetrievalHit> hits) {
		this.query = Objects.requireNonNull(query, "query must not be null");
		this.hits = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(hits, "hits must not be null")));
	}

	public String getQuery() {
		return query;
	}

	public List<RagRetrievalHit> getHits() {
		return hits;
	}

	public int size() {
		return hits.size();
	}

	public boolean isEmpty() {
		return hits.isEmpty();
	}

	@Override
	public String toString() {
		return "RagRetrievalResult{query='" + query + "', hits=" + hits + "}";
	}
}