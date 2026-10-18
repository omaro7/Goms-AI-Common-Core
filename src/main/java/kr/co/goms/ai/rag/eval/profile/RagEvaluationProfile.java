/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.profile;

import java.util.Locale;
import java.util.Objects;

import kr.co.goms.ai.rag.retrieval.RagRetrievalMode;

/**
 * RAG Golden Evaluation 실험 프로필.
 */
public final class RagEvaluationProfile {

	private static final double HYBRID_VECTOR_WEIGHT = 0.70;
	private static final double HYBRID_VECTOR_GRAPH_WEIGHT = 0.30;
	private static final int HYBRID_RRF_K = 60;

	private static final int HYBRID_V1_BRANCH_CANDIDATE_MULTIPLIER = 1;
	private static final int HYBRID_V2_BRANCH_CANDIDATE_MULTIPLIER = 2;
	private static final int HYBRID_V3_BRANCH_CANDIDATE_MULTIPLIER = 2;
	private static final int HYBRID_V4_BRANCH_CANDIDATE_MULTIPLIER = 2;
	private static final int HYBRID_V5_BRANCH_CANDIDATE_MULTIPLIER = 2;
	private static final int HYBRID_V6_BRANCH_CANDIDATE_MULTIPLIER = 2;

	private static final double HYBRID_GRAPH_WEIGHT = 0.025;
	private static final int HYBRID_GRAPH_SEED_LIMIT = 1;

	private final RagRetrievalMode retrievalMode;
	private final RagEvaluationVersion version;
	private final String experimentId;
	private final String reportName;

	private RagEvaluationProfile(RagRetrievalMode retrievalMode, RagEvaluationVersion version) {
		this.retrievalMode = Objects.requireNonNull(retrievalMode, "retrievalMode must not be null");
		this.version = Objects.requireNonNull(version, "version must not be null");
		validateProfile(this.retrievalMode, this.version);
		this.experimentId = createExperimentId(this.retrievalMode, this.version);
		this.reportName = createReportName(this.retrievalMode, this.version);
	}

	public static RagEvaluationProfile of(RagRetrievalMode retrievalMode) {
		return of(retrievalMode, RagEvaluationVersion.V1);
	}

	public static RagEvaluationProfile of(RagRetrievalMode retrievalMode, RagEvaluationVersion version) {
		return new RagEvaluationProfile(retrievalMode, version);
	}

	public RagRetrievalMode getRetrievalMode() {
		return retrievalMode;
	}

	public RagEvaluationVersion getVersion() {
		return version;
	}

	public String getExperimentId() {
		return experimentId;
	}

	public String getReportName() {
		return reportName;
	}

	public boolean isVectorOnly() {
		return retrievalMode == RagRetrievalMode.VECTOR_ONLY;
	}

	public boolean isVectorGraph() {
		return retrievalMode == RagRetrievalMode.VECTOR_GRAPH;
	}

	public boolean isHybrid() {
		return retrievalMode == RagRetrievalMode.HYBRID;
	}

	public double getGraphWeight() {

		if (isHybrid()) return HYBRID_GRAPH_WEIGHT;
		if (!isVectorGraph()) return 0.0;

		return switch (version) {
			case V1, V2 -> 0.10;
			case V3, V4 -> 0.05;
			case V5, V6 -> 0.025;
		};
	}

	public int getGraphSeedLimit() {

		if (isHybrid()) return HYBRID_GRAPH_SEED_LIMIT;
		if (!isVectorGraph()) return 0;

		return switch (version) {
			case V1, V2, V3 -> Integer.MAX_VALUE;
			case V4, V5, V6 -> 1;
		};
	}

	public boolean isGraphCandidateChunkFilterEnabled() {
		if (isHybrid()) return true;
		return isVectorGraph() && version == RagEvaluationVersion.V6;
	}

	public double getHybridVectorWeight() {
		return HYBRID_VECTOR_WEIGHT;
	}

	public double getHybridVectorGraphWeight() {
		return HYBRID_VECTOR_GRAPH_WEIGHT;
	}

	public int getHybridRrfK() {
		return HYBRID_RRF_K;
	}

	public int getHybridBranchCandidateMultiplier() {

		if (!isHybrid()) return 1;

		return switch (version) {
			case V1 -> HYBRID_V1_BRANCH_CANDIDATE_MULTIPLIER;
			case V2 -> HYBRID_V2_BRANCH_CANDIDATE_MULTIPLIER;
	        case V3 -> HYBRID_V3_BRANCH_CANDIDATE_MULTIPLIER;
	        case V4 -> HYBRID_V4_BRANCH_CANDIDATE_MULTIPLIER;
	        case V5 -> HYBRID_V5_BRANCH_CANDIDATE_MULTIPLIER;
	        case V6 -> HYBRID_V5_BRANCH_CANDIDATE_MULTIPLIER;
			default -> throw new IllegalStateException("Unsupported HYBRID evaluation version: " + version);
		};
	}

	public String getHybridVectorExperimentId() {
		return "VECTOR_ONLY_V1";
	}

	public String getHybridVectorGraphExperimentId() {
		return "VECTOR_GRAPH_V6";
	}

	private static void validateProfile(RagRetrievalMode retrievalMode, RagEvaluationVersion version) {

		if (retrievalMode != RagRetrievalMode.HYBRID) return;

		if (version != RagEvaluationVersion.V1 && version != RagEvaluationVersion.V2 && version != RagEvaluationVersion.V3 
				&& version != RagEvaluationVersion.V4
				&& version != RagEvaluationVersion.V5
				&& version != RagEvaluationVersion.V6
				) {
			throw new IllegalArgumentException("Unsupported HYBRID evaluation version: " + version);
		}
	}

	private static String createExperimentId(RagRetrievalMode retrievalMode, RagEvaluationVersion version) {
		return retrievalMode.name() + "_" + version.name();
	}

	private static String createReportName(RagRetrievalMode retrievalMode, RagEvaluationVersion version) {
		return retrievalMode.name().toLowerCase(Locale.ROOT).replace('_', '-') + "-" + version.getValue();
	}

	@Override
	public String toString() {
		return "RagEvaluationProfile{retrievalMode=" + retrievalMode
				+ ", version=" + version
				+ ", experimentId='" + experimentId + '\''
				+ ", reportName='" + reportName + '\''
				+ ", graphWeight=" + getGraphWeight()
				+ ", graphSeedLimit=" + getGraphSeedLimit()
				+ ", graphCandidateChunkFilterEnabled=" + isGraphCandidateChunkFilterEnabled()
				+ ", hybridVectorWeight=" + getHybridVectorWeight()
				+ ", hybridVectorGraphWeight=" + getHybridVectorGraphWeight()
				+ ", hybridRrfK=" + getHybridRrfK()
				+ ", hybridBranchCandidateMultiplier=" + getHybridBranchCandidateMultiplier()
				+ '}';
	}
}