/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.retrieval;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import kr.co.goms.ai.rag.eval.model.RagRetrievalEvaluationResult;
import kr.co.goms.ai.rag.eval.model.RagRetrievalHit;
import kr.co.goms.ai.rag.eval.model.RagRetrievalResult;

/**
 * RAG Retrieval 기본 평가기.
	 expectedDocuments
	 ├─ OEBPS/Text/chapter10_4.xhtml
	 └─ OEBPS/Text/chapter10_5.xhtml
	
	Retriever Top-K
	 rank 1 → chapter10_1.xhtml
	 rank 2 → chapter10_4.xhtml  ← 정답
	 rank 3 → chapter20_1.xhtml
	 rank 4 → chapter10_5.xhtml  ← 정답
	 rank 5 → chapter30_1.xhtml
	
	Hit@K    = true
	Recall@K = 2 / 2 = 1.0
	MRR      = 1 / 2 = 0.5
 */
public final class DefaultRagRetrievalEvaluator implements RagRetrievalEvaluator {

	@Override
	public RagRetrievalEvaluationResult evaluate(List<String> expectedDocuments, RagRetrievalResult retrievalResult) {
		Objects.requireNonNull(retrievalResult, "retrievalResult must not be null");

		Set<String> expectedSources = normalizeExpectedDocuments(expectedDocuments);

		if (expectedSources.isEmpty()) return RagRetrievalEvaluationResult.notApplicable();

		Set<String> matchedSources = new LinkedHashSet<>();
		int firstRelevantRank = Integer.MAX_VALUE;

		for (RagRetrievalHit hit : retrievalResult.getHits()) {
			if (hit == null) continue;

			String source = normalizeSource(hit.getSource());

			if (!expectedSources.contains(source)) continue;

			matchedSources.add(source);
			firstRelevantRank = Math.min(firstRelevantRank, hit.getRank());
		}

		boolean hitAtK = !matchedSources.isEmpty();
		double recallAtK = (double) matchedSources.size() / expectedSources.size();
		double mrr = firstRelevantRank == Integer.MAX_VALUE ? 0.0 : 1.0 / firstRelevantRank;

		return RagRetrievalEvaluationResult.of(hitAtK, recallAtK, mrr);
	}

	private Set<String> normalizeExpectedDocuments(List<String> expectedDocuments) {
		Set<String> normalized = new LinkedHashSet<>();

		if (expectedDocuments == null || expectedDocuments.isEmpty()) return normalized;

		for (String expectedDocument : expectedDocuments) {
			String source = normalizeSource(expectedDocument);

			if (!source.isBlank()) normalized.add(source);
		}

		return normalized;
	}

	private String normalizeSource(String source) {
		if (source == null) return "";
		return source.trim().replace('\\', '/');
	}
}