/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.retrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import kr.co.goms.ai.rag.graph.GraphExpansionProvider;
import kr.co.goms.ai.rag.util.RagUtil;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * Vector 검색 결과를 Seed로 Graph를 확장한 후 관련 문서를 다시 Vector 검색하여 재정렬한다.
 *
 * <pre>
 * Question
 *    ↓
 * Vector Retriever
 *    ↓
 * Seed Documents
 *    ↓
 * Graph Expansion
 *    ↓
 * Related Documents
 *    ↓
 * Vector Search
 *    ↓
 * Graph Candidate Chunk Filter
 *    ↓
 * Graph Boost
 *    ↓
 * Final Top-K
 * </pre>
 */
public final class VectorGraphRetriever implements Retriever {

	private static final double DEFAULT_GRAPH_WEIGHT = 0.10;
	private static final int DEFAULT_GRAPH_SEED_LIMIT = Integer.MAX_VALUE;
	private static final boolean DEFAULT_GRAPH_CANDIDATE_CHUNK_FILTER_ENABLED = false;

	private static final String METADATA_RETRIEVAL_MODE = "retrievalMode";
	private static final String METADATA_RETRIEVAL_SOURCE = "retrievalSource";
	private static final String METADATA_VECTOR_SCORE = "vectorScore";
	private static final String METADATA_GRAPH_SCORE = "graphScore";
	private static final String METADATA_GRAPH_WEIGHT = "graphWeight";
	private static final String METADATA_FINAL_SCORE = "finalScore";

	private static final String RETRIEVAL_MODE_VECTOR_GRAPH = "VECTOR_GRAPH";
	private static final String RETRIEVAL_SOURCE_VECTOR = "VECTOR";
	private static final String RETRIEVAL_SOURCE_VECTOR_GRAPH = "VECTOR_GRAPH";

	private static final String CHUNK_TYPE_HEADING = "HEADING";
	private static final String CHUNK_TYPE_ALT_TEXT = "ALT_TEXT";

	private final Retriever vectorRetriever;
	private final GraphExpansionProvider graphExpansionProvider;
	private final double graphWeight;
	private final int graphSeedLimit;
	private final boolean graphCandidateChunkFilterEnabled;

	public VectorGraphRetriever(Retriever vectorRetriever, GraphExpansionProvider graphExpansionProvider) {
		this(vectorRetriever, graphExpansionProvider, DEFAULT_GRAPH_WEIGHT, DEFAULT_GRAPH_SEED_LIMIT, DEFAULT_GRAPH_CANDIDATE_CHUNK_FILTER_ENABLED);
	}

	public VectorGraphRetriever(Retriever vectorRetriever, GraphExpansionProvider graphExpansionProvider, double graphWeight) {
		this(vectorRetriever, graphExpansionProvider, graphWeight, DEFAULT_GRAPH_SEED_LIMIT, DEFAULT_GRAPH_CANDIDATE_CHUNK_FILTER_ENABLED);
	}

	public VectorGraphRetriever(Retriever vectorRetriever, GraphExpansionProvider graphExpansionProvider, double graphWeight, int graphSeedLimit) {
		this(vectorRetriever, graphExpansionProvider, graphWeight, graphSeedLimit, DEFAULT_GRAPH_CANDIDATE_CHUNK_FILTER_ENABLED);
	}

	public VectorGraphRetriever(Retriever vectorRetriever, GraphExpansionProvider graphExpansionProvider, double graphWeight, int graphSeedLimit, boolean graphCandidateChunkFilterEnabled) {
		this.vectorRetriever = Objects.requireNonNull(vectorRetriever, "vectorRetriever must not be null");
		this.graphExpansionProvider = Objects.requireNonNull(graphExpansionProvider, "graphExpansionProvider must not be null");
		this.graphWeight = requireGraphWeight(graphWeight);
		this.graphSeedLimit = requireGraphSeedLimit(graphSeedLimit);
		this.graphCandidateChunkFilterEnabled = graphCandidateChunkFilterEnabled;
	}

	@Override
	public RetrievalResult retrieve(String query) throws RetrievalException {

		String normalizedQuery = query == null ? "" : query.trim();

		throw new RetrievalException("Project-scoped Vector Graph retrieval requires RetrievalRequest with projectId", normalizedQuery, "", RetrievalOperation.VALIDATE, null);
	}

	@Override
	public RetrievalResult retrieve(RetrievalRequest request) throws RetrievalException {

		validateRequest(request);

		long startedAt = System.nanoTime();

		RetrievalResult vectorResult = vectorRetriever.retrieve(request);

		if (vectorResult == null) throw new RetrievalException("Vector Retriever returned null result", request.getQuery(), "", RetrievalOperation.RESULT_MAPPING, null);
		if (vectorResult.isEmpty()) return vectorResult;
		if (request.hasSourcePathFilters()) return vectorResult;
		if (!graphExpansionProvider.isAvailable()) return vectorResult;

		Set<String> seedSourcePaths = createGraphSeedSourcePaths(vectorResult.getSearchResults());

		if (seedSourcePaths.isEmpty()) return vectorResult;

		System.out.println("[RAG-GRAPH] graphWeight=" + graphWeight);
		System.out.println("[RAG-GRAPH] graphSeedLimit=" + graphSeedLimit);
		System.out.println("[RAG-GRAPH] graphCandidateChunkFilterEnabled=" + graphCandidateChunkFilterEnabled);
		System.out.println("[RAG-GRAPH] graphSeeds=" + seedSourcePaths);

		Map<String, Double> graphScores = graphExpansionProvider.expand(request.getProjectId(), request.getQuery(), seedSourcePaths);

		if (graphScores == null || graphScores.isEmpty()) return vectorResult;

		Set<String> graphSourcePaths = normalizeGraphSourcePaths(graphScores.keySet());

		if (graphSourcePaths.isEmpty()) return vectorResult;

		RetrievalRequest graphRequest = createGraphRetrievalRequest(request, graphSourcePaths);
		RetrievalResult graphResult = vectorRetriever.retrieve(graphRequest);

		if (graphResult == null || graphResult.isEmpty()) return vectorResult;

		List<VectorSearchResult> mergedResults = mergeAndRank(vectorResult.getSearchResults(), graphResult.getSearchResults(), graphScores, request.getTopK());

		return RetrievalResult.builder()
				.query(vectorResult.getQuery())
				.model(vectorResult.getModel())
				.searchResults(mergedResults)
				.durationNanos(System.nanoTime() - startedAt)
				.dimensions(vectorResult.getDimensions())
				.build();
	}

	private Set<String> createGraphSeedSourcePaths(List<VectorSearchResult> searchResults) {

		if (searchResults == null || searchResults.isEmpty()) return Set.of();

		Set<String> seedSourcePaths = new LinkedHashSet<>();

		for (VectorSearchResult result : searchResults) {

			if (result == null || result.getChunk() == null) continue;

			String sourcePath = RagUtil.normalizeDocumentPath(result.getChunk().getSourcePath());

			if (sourcePath == null || sourcePath.isBlank()) continue;
			if (RagUtil.isExcludedDocument(sourcePath)) continue;

			seedSourcePaths.add(sourcePath);

			if (seedSourcePaths.size() >= graphSeedLimit) break;
		}

		return Collections.unmodifiableSet(seedSourcePaths);
	}

	private Set<String> normalizeGraphSourcePaths(Set<String> sourcePaths) {

		if (sourcePaths == null || sourcePaths.isEmpty()) return Set.of();

		Set<String> normalized = new LinkedHashSet<>();

		for (String sourcePath : sourcePaths) {

			String normalizedSourcePath = RagUtil.normalizeDocumentPath(sourcePath);

			if (normalizedSourcePath == null || normalizedSourcePath.isBlank()) continue;
			if (RagUtil.isExcludedDocument(normalizedSourcePath)) continue;

			normalized.add(normalizedSourcePath);
		}

		return Collections.unmodifiableSet(normalized);
	}

	private RetrievalRequest createGraphRetrievalRequest(RetrievalRequest request, Set<String> graphSourcePaths) {

		int candidateTopK = Math.max(request.getTopK(), request.getTopK() * 3);

		return RetrievalRequest.builder()
				.projectId(request.getProjectId())
				.query(request.getQuery())
				.topK(candidateTopK)
				.minimumScore(request.getMinimumScore())
				.similarityType(request.getSimilarityType())
				.chunkTypes(request.getChunkTypes())
				.sourcePaths(graphSourcePaths)
				.epubTypes(request.getEpubTypes())
				.languages(request.getLanguages())
				.metadataFilters(request.getMetadataFilters())
				.includeRejected(request.isIncludeRejected())
				.preserveDocumentOrder(false)
				.build();
	}

	private List<VectorSearchResult> mergeAndRank(List<VectorSearchResult> vectorResults, List<VectorSearchResult> graphResults, Map<String, Double> graphScores, int topK) {

		Map<String, VectorSearchResult> mergedById = new LinkedHashMap<>();

		addVectorResults(mergedById, vectorResults);
		addGraphResults(mergedById, graphResults, graphScores);

		List<VectorSearchResult> merged = new ArrayList<>(mergedById.values());

		merged.sort(Comparator.comparingDouble(VectorSearchResult::getScore).reversed().thenComparing(VectorSearchResult::getId));

		int resultCount = Math.min(topK, merged.size());
		List<VectorSearchResult> ranked = new ArrayList<>(resultCount);

		for (int index = 0; index < resultCount; index++) ranked.add(merged.get(index).withRank(index + 1));

		return List.copyOf(ranked);
	}

	private void addVectorResults(Map<String, VectorSearchResult> mergedById, List<VectorSearchResult> vectorResults) {

		if (vectorResults == null || vectorResults.isEmpty()) return;

		for (VectorSearchResult result : vectorResults) {

			if (result == null || result.getChunk() == null) continue;

			VectorSearchResult decorated = createVectorResult(result);

			mergedById.put(decorated.getId(), decorated);
		}
	}

	private void addGraphResults(Map<String, VectorSearchResult> mergedById, List<VectorSearchResult> graphResults, Map<String, Double> graphScores) {

		if (graphResults == null || graphResults.isEmpty()) return;
		if (graphScores == null || graphScores.isEmpty()) return;

		for (VectorSearchResult result : graphResults) {

			if (result == null || result.getChunk() == null) continue;
			if (!isEligibleGraphCandidate(result)) continue;

			String sourcePath = RagUtil.normalizeDocumentPath(result.getChunk().getSourcePath());

			if (sourcePath == null || sourcePath.isBlank()) continue;

			Double graphScore = graphScores.get(sourcePath);

			if (graphScore == null) continue;

			VectorSearchResult graphResult = createVectorGraphResult(result, graphScore);

			mergedById.merge(graphResult.getId(), graphResult, this::selectHigherScore);
		}
	}

	private boolean isEligibleGraphCandidate(VectorSearchResult result) {

		if (!graphCandidateChunkFilterEnabled) return true;
		if (result == null || result.getChunk() == null) return false;
		if (result.getChunk().getType() == null) return true;

		String chunkType = String.valueOf(result.getChunk().getType()).trim().toUpperCase(Locale.ROOT);

		if (CHUNK_TYPE_HEADING.equals(chunkType)) {
			System.out.println("[RAG-GRAPH] candidate skipped - HEADING: " + result.getId());
			return false;
		}

		if (CHUNK_TYPE_ALT_TEXT.equals(chunkType)) {
			System.out.println("[RAG-GRAPH] candidate skipped - ALT_TEXT: " + result.getId());
			return false;
		}

		return true;
	}

	private VectorSearchResult createVectorResult(VectorSearchResult result) {

		double vectorScore = result.getScore();

		Map<String, String> metadata = new LinkedHashMap<>(result.getMetadata());

		metadata.put(METADATA_RETRIEVAL_MODE, RETRIEVAL_MODE_VECTOR_GRAPH);
		metadata.put(METADATA_RETRIEVAL_SOURCE, RETRIEVAL_SOURCE_VECTOR);
		metadata.put(METADATA_VECTOR_SCORE, Double.toString(vectorScore));
		metadata.put(METADATA_FINAL_SCORE, Double.toString(vectorScore));

		return VectorSearchResult.builder()
				.record(result.getRecord())
				.score(vectorScore)
				.rank(result.getRank())
				.similarityType(result.getSimilarityType())
				.accepted(result.isAccepted())
				.metadata(metadata)
				.build();
	}

	private VectorSearchResult createVectorGraphResult(VectorSearchResult result, double graphScore) {

		double vectorScore = result.getScore();
		double finalScore = vectorScore + graphScore * graphWeight;

		Map<String, String> metadata = new LinkedHashMap<>(result.getMetadata());

		metadata.put(METADATA_RETRIEVAL_MODE, RETRIEVAL_MODE_VECTOR_GRAPH);
		metadata.put(METADATA_RETRIEVAL_SOURCE, RETRIEVAL_SOURCE_VECTOR_GRAPH);
		metadata.put(METADATA_VECTOR_SCORE, Double.toString(vectorScore));
		metadata.put(METADATA_GRAPH_SCORE, Double.toString(graphScore));
		metadata.put(METADATA_GRAPH_WEIGHT, Double.toString(graphWeight));
		metadata.put(METADATA_FINAL_SCORE, Double.toString(finalScore));

		return VectorSearchResult.builder()
				.record(result.getRecord())
				.score(finalScore)
				.rank(result.getRank())
				.similarityType(result.getSimilarityType())
				.accepted(result.isAccepted())
				.metadata(metadata)
				.build();
	}

	private VectorSearchResult selectHigherScore(VectorSearchResult current, VectorSearchResult candidate) {

		if (current == null) return candidate;
		if (candidate == null) return current;
		if (candidate.getScore() > current.getScore()) return candidate;

		return current;
	}

	private void validateRequest(RetrievalRequest request) throws RetrievalException {

		if (request == null) throw new RetrievalException("Retrieval request must not be null", "", "", RetrievalOperation.VALIDATE, null);

		String projectId = request.getProjectId();
		String query = request.getQuery();

		if (projectId == null || projectId.isBlank()) throw new RetrievalException("Retrieval projectId must not be blank", query == null ? "" : query, "", RetrievalOperation.VALIDATE, null);
		if (query == null || query.isBlank()) throw new RetrievalException("Retrieval query must not be blank", "", "", RetrievalOperation.VALIDATE, null);
		if (request.getTopK() < 1) throw new RetrievalException("Retrieval topK must be greater than zero", query, "", RetrievalOperation.VALIDATE, null);
		if (!Double.isFinite(request.getMinimumScore())) throw new RetrievalException("Retrieval minimumScore must be finite", query, "", RetrievalOperation.VALIDATE, null);
	}

	private static double requireGraphWeight(double graphWeight) {

		if (!Double.isFinite(graphWeight)) throw new IllegalArgumentException("graphWeight must be finite");
		if (graphWeight < 0.0 || graphWeight > 1.0) throw new IllegalArgumentException("graphWeight must be between 0.0 and 1.0");

		return graphWeight;
	}

	private static int requireGraphSeedLimit(int graphSeedLimit) {

		if (graphSeedLimit <= 0) throw new IllegalArgumentException("graphSeedLimit must be greater than zero");

		return graphSeedLimit;
	}

	@Override
	public boolean isAvailable() {
		return vectorRetriever.isAvailable() && graphExpansionProvider.isAvailable();
	}
}