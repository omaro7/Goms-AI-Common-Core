/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * Vector Retrieval과 Vector Graph Retrieval 결과를
 * Weighted Reciprocal Rank Fusion 방식으로 결합한다.
 *
 * <pre>
 * Question
 *    │
 *    ├──────────────────────────┐
 *    ▼                          ▼
 * Vector Retriever         Vector Graph Retriever
 *    │                          │
 *    │                          │
 *    └────────────┬─────────────┘
 *                 ▼
 *          Weighted RRF
 *                 ▼
 *             Final Top-K
 * </pre>
 *
 * HYBRID_V1:
 *
 * <pre>
 * branchCandidateMultiplier = 1
 *
 * Final Top-5
 * ├─ Vector Top-5
 * └─ VectorGraph Top-5
 * </pre>
 *
 * HYBRID_V2:
 *
 * <pre>
 * branchCandidateMultiplier = 2
 *
 * Final Top-5
 * ├─ Vector Top-10
 * └─ VectorGraph Top-10
 *          ↓
 *     Weighted RRF
 *          ↓
 *      Final Top-5
 * </pre>
 *
 * Raw Score를 직접 합산하지 않고 각 Retriever의 Rank를 사용한다.
 */
public final class HybridRetriever implements Retriever {

	private static final double DEFAULT_VECTOR_WEIGHT = 0.70;
	private static final double DEFAULT_VECTOR_GRAPH_WEIGHT = 0.30;
	private static final int DEFAULT_RRF_K = 60;
	private static final int DEFAULT_BRANCH_CANDIDATE_MULTIPLIER = 1;

	private static final String METADATA_RETRIEVAL_MODE = "retrievalMode";
	private static final String METADATA_RETRIEVAL_SOURCE = "retrievalSource";
	private static final String METADATA_VECTOR_MATCHED = "vectorMatched";
	private static final String METADATA_VECTOR_GRAPH_MATCHED = "vectorGraphMatched";
	private static final String METADATA_VECTOR_RANK = "vectorRank";
	private static final String METADATA_VECTOR_GRAPH_RANK = "vectorGraphRank";
	private static final String METADATA_VECTOR_WEIGHT = "vectorWeight";
	private static final String METADATA_VECTOR_GRAPH_WEIGHT = "vectorGraphWeight";
	private static final String METADATA_RRF_K = "rrfK";
	private static final String METADATA_VECTOR_RRF_SCORE = "vectorRrfScore";
	private static final String METADATA_VECTOR_GRAPH_RRF_SCORE = "vectorGraphRrfScore";
	private static final String METADATA_VECTOR_FINAL_SCORE = "vectorFinalScore";
	private static final String METADATA_VECTOR_GRAPH_FINAL_SCORE = "vectorGraphFinalScore";
	private static final String METADATA_VECTOR_GRAPH_RETRIEVAL_SOURCE = "vectorGraphRetrievalSource";
	private static final String METADATA_BRANCH_CANDIDATE_MULTIPLIER = "branchCandidateMultiplier";
	private static final String METADATA_BRANCH_TOP_K = "branchTopK";
	private static final String METADATA_FINAL_TOP_K = "finalTopK";
	private static final String METADATA_HYBRID_SCORE = "hybridScore";
	private static final String METADATA_FINAL_SCORE = "finalScore";

	private static final String RETRIEVAL_MODE_HYBRID = "HYBRID";
	private static final String RETRIEVAL_SOURCE_VECTOR = "VECTOR";
	private static final String RETRIEVAL_SOURCE_VECTOR_GRAPH = "VECTOR_GRAPH";
	private static final String RETRIEVAL_SOURCE_HYBRID = "HYBRID";

	private final Retriever vectorRetriever;
	private final Retriever vectorGraphRetriever;
	private final double vectorWeight;
	private final double vectorGraphWeight;
	private final int rrfK;
	private final int branchCandidateMultiplier;

	public HybridRetriever(Retriever vectorRetriever, Retriever vectorGraphRetriever) {
		this(vectorRetriever, vectorGraphRetriever, DEFAULT_VECTOR_WEIGHT, DEFAULT_VECTOR_GRAPH_WEIGHT, DEFAULT_RRF_K, DEFAULT_BRANCH_CANDIDATE_MULTIPLIER);
	}

	public HybridRetriever(Retriever vectorRetriever, Retriever vectorGraphRetriever, double vectorWeight, double vectorGraphWeight) {
		this(vectorRetriever, vectorGraphRetriever, vectorWeight, vectorGraphWeight, DEFAULT_RRF_K, DEFAULT_BRANCH_CANDIDATE_MULTIPLIER);
	}

	public HybridRetriever(Retriever vectorRetriever, Retriever vectorGraphRetriever, double vectorWeight, double vectorGraphWeight, int rrfK) {
		this(vectorRetriever, vectorGraphRetriever, vectorWeight, vectorGraphWeight, rrfK, DEFAULT_BRANCH_CANDIDATE_MULTIPLIER);
	}

	public HybridRetriever(Retriever vectorRetriever, Retriever vectorGraphRetriever, double vectorWeight, double vectorGraphWeight, int rrfK, int branchCandidateMultiplier) {
		this.vectorRetriever = Objects.requireNonNull(vectorRetriever, "vectorRetriever must not be null");
		this.vectorGraphRetriever = Objects.requireNonNull(vectorGraphRetriever, "vectorGraphRetriever must not be null");
		this.vectorWeight = requireWeight(vectorWeight, "vectorWeight");
		this.vectorGraphWeight = requireWeight(vectorGraphWeight, "vectorGraphWeight");
		this.rrfK = requireRrfK(rrfK);
		this.branchCandidateMultiplier = requireBranchCandidateMultiplier(branchCandidateMultiplier);
		if (vectorWeight + vectorGraphWeight <= 0.0) throw new IllegalArgumentException("At least one hybrid retrieval weight must be greater than zero");
	}

	@Override
	public RetrievalResult retrieve(String query) throws RetrievalException {

		String normalizedQuery = query == null ? "" : query.trim();

		throw new RetrievalException("Project-scoped Hybrid retrieval requires RetrievalRequest with projectId", normalizedQuery, "", RetrievalOperation.VALIDATE, null);
	}

	@Override
	public RetrievalResult retrieve(RetrievalRequest request) throws RetrievalException {

		validateRequest(request);

		long startedAt = System.nanoTime();

		int finalTopK = request.getTopK();
		int branchTopK = calculateBranchTopK(finalTopK);

		RetrievalRequest branchRequest = createBranchRequest(request, branchTopK);

		System.out.println("[RAG-HYBRID] vectorWeight=" + vectorWeight);
		System.out.println("[RAG-HYBRID] vectorGraphWeight=" + vectorGraphWeight);
		System.out.println("[RAG-HYBRID] rrfK=" + rrfK);
		System.out.println("[RAG-HYBRID] branchCandidateMultiplier=" + branchCandidateMultiplier);
		System.out.println("[RAG-HYBRID] branchTopK=" + branchTopK);
		System.out.println("[RAG-HYBRID] finalTopK=" + finalTopK);

		RetrievalResult vectorResult = vectorRetriever.retrieve(branchRequest);
		RetrievalResult vectorGraphResult = vectorGraphRetriever.retrieve(branchRequest);

		validateResult(vectorResult, "Vector Retriever", request);
		validateResult(vectorGraphResult, "Vector Graph Retriever", request);

		System.out.println("[RAG-HYBRID] vectorResults=" + vectorResult.size());
		System.out.println("[RAG-HYBRID] vectorGraphResults=" + vectorGraphResult.size());

		List<VectorSearchResult> fusedResults = fuse(vectorResult.getSearchResults(), vectorGraphResult.getSearchResults(), finalTopK, branchTopK);

		return RetrievalResult.builder()
				.query(resolveQuery(vectorResult, vectorGraphResult, request))
				.model(resolveModel(vectorResult, vectorGraphResult))
				.searchResults(fusedResults)
				.durationNanos(System.nanoTime() - startedAt)
				.dimensions(resolveDimensions(vectorResult, vectorGraphResult))
				.build();
	}

	/**
	 * 원본 RetrievalRequest의 검색 조건은 모두 유지하고
	 * Branch에서 사용할 topK만 확대한다.
	 */
	private RetrievalRequest createBranchRequest(RetrievalRequest request, int branchTopK) {

		return RetrievalRequest.builder()
				.projectId(request.getProjectId())
				.query(request.getQuery())
				.topK(branchTopK)
				.minimumScore(request.getMinimumScore())
				.similarityType(request.getSimilarityType())
				.chunkTypes(request.getChunkTypes())
				.sourcePaths(request.getSourcePaths())
				.epubTypes(request.getEpubTypes())
				.languages(request.getLanguages())
				.metadataFilters(request.getMetadataFilters())
				.includeRejected(request.isIncludeRejected())
				.preserveDocumentOrder(request.isPreserveDocumentOrder())
				.build();
	}

	private int calculateBranchTopK(int finalTopK) {

		try {
			return Math.multiplyExact(finalTopK, branchCandidateMultiplier);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException("Hybrid branchTopK overflow. finalTopK=" + finalTopK + ", branchCandidateMultiplier=" + branchCandidateMultiplier, exception);
		}
	}

	private List<VectorSearchResult> fuse(List<VectorSearchResult> vectorResults, List<VectorSearchResult> vectorGraphResults, int finalTopK, int branchTopK) {

		Map<String, FusionCandidate> candidates = new LinkedHashMap<>();

		addVectorResults(candidates, vectorResults);
		addVectorGraphResults(candidates, vectorGraphResults);

		if (candidates.isEmpty()) return List.of();

		List<FusionCandidate> fused = new ArrayList<>(candidates.values());

		fused.sort(
				Comparator
						.comparingDouble(FusionCandidate::getHybridScore)
						.reversed()
						.thenComparingInt(FusionCandidate::getBestRank)
						.thenComparing(FusionCandidate::getId));

		int resultCount = Math.min(finalTopK, fused.size());
		List<VectorSearchResult> results = new ArrayList<>(resultCount);

		for (int index = 0; index < resultCount; index++) results.add(createHybridResult(fused.get(index), index + 1, finalTopK, branchTopK));

		logResults(results);

		return List.copyOf(results);
	}

	private void addVectorResults(Map<String, FusionCandidate> candidates, List<VectorSearchResult> results) {

		if (results == null || results.isEmpty()) return;

		for (int index = 0; index < results.size(); index++) {

			VectorSearchResult result = results.get(index);

			if (!isUsable(result)) continue;

			int rank = resolveRank(result, index);

			FusionCandidate candidate = candidates.computeIfAbsent(result.getId(), FusionCandidate::new);

			candidate.acceptVector(result, rank, calculateRrfScore(vectorWeight, rank));
		}
	}

	private void addVectorGraphResults(Map<String, FusionCandidate> candidates, List<VectorSearchResult> results) {

		if (results == null || results.isEmpty()) return;

		for (int index = 0; index < results.size(); index++) {

			VectorSearchResult result = results.get(index);

			if (!isUsable(result)) continue;

			int rank = resolveRank(result, index);

			FusionCandidate candidate = candidates.computeIfAbsent(result.getId(), FusionCandidate::new);

			candidate.acceptVectorGraph(result, rank, calculateRrfScore(vectorGraphWeight, rank));
		}
	}

	private VectorSearchResult createHybridResult(FusionCandidate candidate, int finalRank, int finalTopK, int branchTopK) {

		VectorSearchResult baseResult = candidate.getRepresentativeResult();

		if (baseResult == null) throw new IllegalStateException("Hybrid fusion candidate has no representative result: " + candidate.getId());

		double hybridScore = candidate.getHybridScore();

		Map<String, String> metadata = new LinkedHashMap<>(baseResult.getMetadata());

		preserveVectorGraphRetrievalSource(candidate, metadata);

		metadata.put(METADATA_RETRIEVAL_MODE, RETRIEVAL_MODE_HYBRID);
		metadata.put(METADATA_RETRIEVAL_SOURCE, resolveRetrievalSource(candidate));

		metadata.put(METADATA_VECTOR_MATCHED, Boolean.toString(candidate.hasVectorResult()));
		metadata.put(METADATA_VECTOR_GRAPH_MATCHED, Boolean.toString(candidate.hasVectorGraphResult()));

		metadata.put(METADATA_VECTOR_WEIGHT, Double.toString(vectorWeight));
		metadata.put(METADATA_VECTOR_GRAPH_WEIGHT, Double.toString(vectorGraphWeight));
		metadata.put(METADATA_RRF_K, Integer.toString(rrfK));

		metadata.put(METADATA_BRANCH_CANDIDATE_MULTIPLIER, Integer.toString(branchCandidateMultiplier));
		metadata.put(METADATA_BRANCH_TOP_K, Integer.toString(branchTopK));
		metadata.put(METADATA_FINAL_TOP_K, Integer.toString(finalTopK));

		metadata.put(METADATA_VECTOR_RRF_SCORE, Double.toString(candidate.getVectorRrfScore()));
		metadata.put(METADATA_VECTOR_GRAPH_RRF_SCORE, Double.toString(candidate.getVectorGraphRrfScore()));

		metadata.put(METADATA_HYBRID_SCORE, Double.toString(hybridScore));
		metadata.put(METADATA_FINAL_SCORE, Double.toString(hybridScore));

		if (candidate.hasVectorResult()) {
			metadata.put(METADATA_VECTOR_RANK, Integer.toString(candidate.getVectorRank()));
			metadata.put(METADATA_VECTOR_FINAL_SCORE, Double.toString(candidate.getVectorResult().getScore()));
		}

		if (candidate.hasVectorGraphResult()) {
			metadata.put(METADATA_VECTOR_GRAPH_RANK, Integer.toString(candidate.getVectorGraphRank()));
			metadata.put(METADATA_VECTOR_GRAPH_FINAL_SCORE, Double.toString(candidate.getVectorGraphResult().getScore()));
		}

		return VectorSearchResult.builder()
				.record(baseResult.getRecord())
				.score(hybridScore)
				.rank(finalRank)
				.similarityType(baseResult.getSimilarityType())
				.accepted(baseResult.isAccepted())
				.metadata(metadata)
				.build();
	}

	private void preserveVectorGraphRetrievalSource(FusionCandidate candidate, Map<String, String> metadata) {

		if (!candidate.hasVectorGraphResult()) return;

		String retrievalSource = candidate.getVectorGraphResult().getMetadata(METADATA_RETRIEVAL_SOURCE);

		if (retrievalSource == null || retrievalSource.isBlank()) return;

		metadata.put(METADATA_VECTOR_GRAPH_RETRIEVAL_SOURCE, retrievalSource);
	}

	private String resolveRetrievalSource(FusionCandidate candidate) {

		if (candidate.hasVectorResult() && candidate.hasVectorGraphResult()) return RETRIEVAL_SOURCE_HYBRID;
		if (candidate.hasVectorGraphResult()) return RETRIEVAL_SOURCE_VECTOR_GRAPH;

		return RETRIEVAL_SOURCE_VECTOR;
	}

	private double calculateRrfScore(double weight, int rank) {
		if (weight <= 0.0 || rank <= 0) return 0.0;
		return weight / (rrfK + rank);
	}

	private int resolveRank(VectorSearchResult result, int index) {
		if (result != null && result.hasRank()) return result.getRank();
		return index + 1;
	}

	private boolean isUsable(VectorSearchResult result) {
		return result != null && result.getRecord() != null && result.getChunk() != null && result.getId() != null && !result.getId().isBlank();
	}

	private String resolveQuery(RetrievalResult vectorResult, RetrievalResult vectorGraphResult, RetrievalRequest request) {

		if (vectorResult != null && vectorResult.getQuery() != null && !vectorResult.getQuery().isBlank()) return vectorResult.getQuery();
		if (vectorGraphResult != null && vectorGraphResult.getQuery() != null && !vectorGraphResult.getQuery().isBlank()) return vectorGraphResult.getQuery();

		return request.getQuery();
	}

	private String resolveModel(RetrievalResult vectorResult, RetrievalResult vectorGraphResult) throws RetrievalException {

		if (vectorResult != null && vectorResult.getModel() != null && !vectorResult.getModel().isBlank()) return vectorResult.getModel();
		if (vectorGraphResult != null && vectorGraphResult.getModel() != null && !vectorGraphResult.getModel().isBlank()) return vectorGraphResult.getModel();

		throw new RetrievalException("Hybrid retrieval model could not be resolved", "", "", RetrievalOperation.RESULT_MAPPING, null);
	}

	private int resolveDimensions(RetrievalResult vectorResult, RetrievalResult vectorGraphResult) {

		if (vectorResult != null && vectorResult.getDimensions() > 0) return vectorResult.getDimensions();
		if (vectorGraphResult != null && vectorGraphResult.getDimensions() > 0) return vectorGraphResult.getDimensions();

		return 0;
	}

	private void validateResult(RetrievalResult result, String retrieverName, RetrievalRequest request) throws RetrievalException {
		if (result == null) throw new RetrievalException(retrieverName + " returned null result", request.getQuery(), "", RetrievalOperation.RESULT_MAPPING, null);
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

	private void logResults(List<VectorSearchResult> results) {

		System.out.println("[RAG-HYBRID] ========================================");
		System.out.println("[RAG-HYBRID] Final result count=" + (results == null ? 0 : results.size()));

		if (results == null || results.isEmpty()) {
			System.out.println("[RAG-HYBRID] ========================================");
			return;
		}

		for (VectorSearchResult result : results) {

			System.out.println(
					"[RAG-HYBRID] rank=" + result.getRank()
					+ ", hybridScore=" + format(result.getScore())
					+ ", source=" + result.getMetadata(METADATA_RETRIEVAL_SOURCE)
					+ ", vectorRank=" + result.getMetadata(METADATA_VECTOR_RANK)
					+ ", vectorGraphRank=" + result.getMetadata(METADATA_VECTOR_GRAPH_RANK)
					+ ", branchTopK=" + result.getMetadata(METADATA_BRANCH_TOP_K)
					+ ", sourcePath=" + (result.getChunk() == null ? "" : result.getChunk().getSourcePath())
					+ ", chunkId=" + result.getId());
		}

		System.out.println("[RAG-HYBRID] ========================================");
	}

	private String format(double value) {
		return String.format(java.util.Locale.ROOT, "%.8f", value);
	}

	private static double requireWeight(double weight, String fieldName) {
		if (!Double.isFinite(weight)) throw new IllegalArgumentException(fieldName + " must be finite");
		if (weight < 0.0 || weight > 1.0) throw new IllegalArgumentException(fieldName + " must be between 0.0 and 1.0");
		return weight;
	}

	private static int requireRrfK(int rrfK) {
		if (rrfK < 0) throw new IllegalArgumentException("rrfK must be greater than or equal to zero");
		return rrfK;
	}

	private static int requireBranchCandidateMultiplier(int branchCandidateMultiplier) {
		if (branchCandidateMultiplier < 1) throw new IllegalArgumentException("branchCandidateMultiplier must be greater than zero");
		return branchCandidateMultiplier;
	}

	@Override
	public boolean isAvailable() {
		return vectorRetriever.isAvailable() && vectorGraphRetriever.isAvailable();
	}

	private static final class FusionCandidate {

		private final String id;

		private VectorSearchResult vectorResult;
		private VectorSearchResult vectorGraphResult;

		private int vectorRank;
		private int vectorGraphRank;

		private double vectorRrfScore;
		private double vectorGraphRrfScore;

		private FusionCandidate(String id) {
			this.id = Objects.requireNonNull(id, "id must not be null");
		}

		private void acceptVector(VectorSearchResult result, int rank, double rrfScore) {

			if (result == null) return;
			if (vectorResult != null && vectorRank <= rank) return;

			vectorResult = result;
			vectorRank = rank;
			vectorRrfScore = rrfScore;
		}

		private void acceptVectorGraph(VectorSearchResult result, int rank, double rrfScore) {

			if (result == null) return;
			if (vectorGraphResult != null && vectorGraphRank <= rank) return;

			vectorGraphResult = result;
			vectorGraphRank = rank;
			vectorGraphRrfScore = rrfScore;
		}

		private String getId() {
			return id;
		}

		private VectorSearchResult getVectorResult() {
			return vectorResult;
		}

		private VectorSearchResult getVectorGraphResult() {
			return vectorGraphResult;
		}

		private boolean hasVectorResult() {
			return vectorResult != null;
		}

		private boolean hasVectorGraphResult() {
			return vectorGraphResult != null;
		}

		private int getVectorRank() {
			return vectorRank;
		}

		private int getVectorGraphRank() {
			return vectorGraphRank;
		}

		private double getVectorRrfScore() {
			return vectorRrfScore;
		}

		private double getVectorGraphRrfScore() {
			return vectorGraphRrfScore;
		}

		private double getHybridScore() {
			return vectorRrfScore + vectorGraphRrfScore;
		}

		private int getBestRank() {

			int bestRank = Integer.MAX_VALUE;

			if (vectorRank > 0) bestRank = Math.min(bestRank, vectorRank);
			if (vectorGraphRank > 0) bestRank = Math.min(bestRank, vectorGraphRank);

			return bestRank;
		}

		private VectorSearchResult getRepresentativeResult() {
			if (vectorGraphResult != null) return vectorGraphResult;
			return vectorResult;
		}
	}
}