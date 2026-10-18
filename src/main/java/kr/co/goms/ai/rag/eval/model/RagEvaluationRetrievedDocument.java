/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.eval.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.util.RagUtil;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * RAG Evaluation Report에 저장할 개별 Retrieval Trace.
 */
public final class RagEvaluationRetrievedDocument {

	private static final String METADATA_RETRIEVAL_SOURCE = "retrievalSource";
	private static final String METADATA_VECTOR_SCORE = "vectorScore";
	private static final String METADATA_GRAPH_SCORE = "graphScore";
	private static final String METADATA_GRAPH_WEIGHT = "graphWeight";
	private static final String METADATA_FINAL_SCORE = "finalScore";

	private final String chunkId;
	private final String sourcePath;
	private final String title;
	private final int rank;
	private final double score;
	private final String retrievalSource;
	private final Double vectorScore;
	private final Double graphScore;
	private final Double graphWeight;
	private final double finalScore;
	private final Map<String, String> metadata;

	public RagEvaluationRetrievedDocument(String chunkId, String sourcePath, String title, int rank, double score, String retrievalSource, Double vectorScore, Double graphScore, Double graphWeight, double finalScore, Map<String, String> metadata) {
		this.chunkId = RagUtil.requireText(chunkId, "chunkId");
		this.sourcePath = RagUtil.requireText(RagUtil.normalizeDocumentPath(sourcePath), "sourcePath");
		this.title = normalize(title);
		this.rank = requireRank(rank);
		this.score = requireFinite(score, "score");
		this.retrievalSource = normalize(retrievalSource);
		this.vectorScore = requireNullableFinite(vectorScore, "vectorScore");
		this.graphScore = requireNullableFinite(graphScore, "graphScore");
		this.graphWeight = requireNullableFinite(graphWeight, "graphWeight");
		this.finalScore = requireFinite(finalScore, "finalScore");
		this.metadata = normalizeMetadata(metadata);
	}

	public static RagEvaluationRetrievedDocument from(VectorSearchResult result) {

		if (result == null) throw new NullPointerException("result must not be null");

		DocumentChunk chunk = result.getChunk();

		if (chunk == null) throw new IllegalStateException("VectorSearchResult chunk must not be null");

		Map<String, String> metadata = result.getMetadata();

		String retrievalSource = getMetadata(metadata, METADATA_RETRIEVAL_SOURCE);
		if (retrievalSource == null || retrievalSource.isBlank()) retrievalSource = "VECTOR";

		Double vectorScore = parseDouble(getMetadata(metadata, METADATA_VECTOR_SCORE));
		if (vectorScore == null) vectorScore = result.getScore();

		Double graphScore = parseDouble(getMetadata(metadata, METADATA_GRAPH_SCORE));
		Double graphWeight = parseDouble(getMetadata(metadata, METADATA_GRAPH_WEIGHT));
		Double metadataFinalScore = parseDouble(getMetadata(metadata, METADATA_FINAL_SCORE));
		double finalScore = metadataFinalScore != null ? metadataFinalScore : result.getScore();

		return new RagEvaluationRetrievedDocument(chunk.getId(), chunk.getSourcePath(), chunk.getTitle(), result.getRank(), result.getScore(), retrievalSource, vectorScore, graphScore, graphWeight, finalScore, metadata);
	}

	public String getChunkId() {
		return chunkId;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public String getTitle() {
		return title;
	}

	public int getRank() {
		return rank;
	}

	public double getScore() {
		return score;
	}

	public String getRetrievalSource() {
		return retrievalSource;
	}

	public Double getVectorScore() {
		return vectorScore;
	}

	public Double getGraphScore() {
		return graphScore;
	}

	public Double getGraphWeight() {
		return graphWeight;
	}

	public double getFinalScore() {
		return finalScore;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public boolean hasRetrievalSource() {
		return !retrievalSource.isBlank();
	}

	public boolean isGraphResult() {
		return retrievalSource.toUpperCase(java.util.Locale.ROOT).contains("GRAPH") || graphScore != null;
	}

	private static String getMetadata(Map<String, String> metadata, String key) {
		return metadata == null ? null : metadata.get(key);
	}

	private static Double parseDouble(String value) {

		if (value == null || value.isBlank()) return null;

		try {
			double parsed = Double.parseDouble(value.trim());
			return Double.isFinite(parsed) ? parsed : null;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private static int requireRank(int rank) {
		if (rank < 0) throw new IllegalArgumentException("rank must be greater than or equal to zero");
		return rank;
	}

	private static double requireFinite(double value, String fieldName) {
		if (!Double.isFinite(value)) throw new IllegalArgumentException(fieldName + " must be finite");
		return value;
	}

	private static Double requireNullableFinite(Double value, String fieldName) {
		if (value != null && !Double.isFinite(value)) throw new IllegalArgumentException(fieldName + " must be finite");
		return value;
	}

	private static Map<String, String> normalizeMetadata(Map<String, String> metadata) {

		if (metadata == null || metadata.isEmpty()) return Collections.emptyMap();

		Map<String, String> normalized = new LinkedHashMap<>();

		for (Map.Entry<String, String> entry : metadata.entrySet()) {

			String key = normalize(entry.getKey());

			if (key.isBlank()) continue;

			normalized.put(key, normalize(entry.getValue()));
		}

		return Collections.unmodifiableMap(normalized);
	}

	@Override
	public String toString() {
		return "RagEvaluationRetrievedDocument{" + "sourcePath='" + sourcePath + '\'' + ", rank=" + rank + ", score=" + score + ", retrievalSource='" + retrievalSource + '\'' + ", finalScore=" + finalScore + '}';
	}
}