/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.path;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import kr.co.goms.ai.epub.service.PublishDirectoryProvider;
import kr.co.goms.ai.rag.eval.profile.RagEvaluationProfile;
import kr.co.goms.ai.rag.util.RagUtil;

/**
 * RAG Evaluation 경로 기본 Resolver.
 *
 * <pre>
 * DefaultRagEvaluationPathResolver
 *         │
 *         └─ resolveLatestGoldenVersion()
 *               ↓
 *              1, 2, 3 ...
 *
 * RagEvaluationPathResolver default
 *         │
 *         └─ resolveLatestGoldenDataset()
 *               ↓
 *         resolveGoldenDataset(projectId, latestVersion)
 *
 * dataset/
 * ├─ rag-lunchwork_seoul-golden-v1.json
 * ├─ rag-lunchwork_seoul-golden-v2.json
 * ├─ rag-lunchwork_seoul-golden-v3.json
 * └─ rag-lunchwork_seoul-golden-v10.json
 *
 * eval/
 * └─ benchmark/
 *    └─ vector-store-v1/
 *       └─ rag-lunchwork_seoul-vector-store-benchmark-v1.json
 * </pre>
 */
public final class DefaultRagEvaluationPathResolver implements RagEvaluationPathResolver {

	private static final String EVAL_DIRECTORY = "eval";
	private static final String DATASET_DIRECTORY = "dataset";
	private static final String REPORTS_DIRECTORY = "reports";
	private static final String RESULT_DIRECTORY = "result";
	private static final String BASELINE_DIRECTORY = "baseline";
	private static final String REGRESSION_DIRECTORY = "regression";
	private static final String BENCHMARK_DIRECTORY = "benchmark";
	private static final String VECTOR_STORE_BENCHMARK_DIRECTORY = "vector-store-v1";

	private final PublishDirectoryProvider publishDirectoryProvider;

	public DefaultRagEvaluationPathResolver(PublishDirectoryProvider publishDirectoryProvider) {
		this.publishDirectoryProvider = Objects.requireNonNull(publishDirectoryProvider, "publishDirectoryProvider must not be null");
	}

	@Override
	public Path resolveEvalDirectory(String projectId) {
		return resolveProjectDirectory(projectId).resolve(EVAL_DIRECTORY);
	}

	@Override
	public Path resolveGoldenDataset(String projectId, int version) {

		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		int normalizedVersion = requireVersion(version);

		return resolveDatasetDirectory(normalizedProjectId).resolve("rag-" + normalizedProjectId + "-golden-v" + normalizedVersion + ".json");
	}

	@Override
	public int resolveLatestGoldenVersion(String projectId) {

		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		Path datasetDirectory = resolveDatasetDirectory(normalizedProjectId);

		if (!Files.isDirectory(datasetDirectory)) throw new IllegalStateException("RAG evaluation dataset directory does not exist: " + datasetDirectory);

		Pattern pattern = Pattern.compile("^rag-" + Pattern.quote(normalizedProjectId) + "-golden-v(\\d+)\\.json$");

		try (Stream<Path> paths = Files.list(datasetDirectory)) {
			return paths.filter(Files::isRegularFile)
					.map(Path::getFileName)
					.map(Path::toString)
					.map(pattern::matcher)
					.filter(Matcher::matches)
					.mapToInt(matcher -> parseVersion(matcher.group(1)))
					.max()
					.orElseThrow(() -> new IllegalStateException("RAG Golden Dataset not found: " + datasetDirectory));
		} catch (IOException exception) {
			throw new IllegalStateException("Failed to resolve latest RAG Golden Dataset version: " + datasetDirectory, exception);
		}
	}

	@Override
	public Path resolveResultDirectory(String projectId) {
		return resolveEvalDirectory(projectId).resolve(RESULT_DIRECTORY);
	}

	@Override
	public Path resolveGoldenReport(String projectId, RagEvaluationProfile profile) {

		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		RagEvaluationProfile normalizedProfile = Objects.requireNonNull(profile, "profile must not be null");
		String reportName = normalizedProfile.getReportName();

		return resolveReportsDirectory(normalizedProjectId)
				.resolve(reportName)
				.resolve("rag-" + normalizedProjectId + "-" + reportName + "-report.json")
				.normalize();
	}

	@Override
	public Path resolveBaselineDirectory(String projectId) {
		return resolveEvalDirectory(projectId).resolve(BASELINE_DIRECTORY);
	}

	@Override
	public Path resolveGoldenBaseline(String projectId, int version) {

		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		int normalizedVersion = requireVersion(version);

		return resolveBaselineDirectory(normalizedProjectId).resolve("rag-" + normalizedProjectId + "-baseline-v" + normalizedVersion + ".json");
	}

	@Override
	public Path resolveRegressionDirectory(String projectId) {
		return resolveEvalDirectory(projectId).resolve(REGRESSION_DIRECTORY);
	}

	@Override
	public Path resolveGoldenRegression(String projectId, int version) {

		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		int normalizedVersion = requireVersion(version);

		return resolveRegressionDirectory(normalizedProjectId).resolve("rag-" + normalizedProjectId + "-regression-v" + normalizedVersion + ".json");
	}

	@Override
	public Path resolveBenchmarkDirectory(String projectId) {
		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		return resolveEvalDirectory(normalizedProjectId).resolve(BENCHMARK_DIRECTORY);
	}

	@Override
	public Path resolveVectorStoreBenchmarkReport(String projectId) {

		String normalizedProjectId = RagUtil.requireProjectId(projectId);

		return resolveBenchmarkDirectory(normalizedProjectId)
				.resolve(VECTOR_STORE_BENCHMARK_DIRECTORY)
				.resolve("rag-" + normalizedProjectId + "-vector-store-benchmark-v1.json")
				.normalize();
	}

	@Override
	public Path resolveGoldenReport(String projectId, int version) {

		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		int normalizedVersion = requireVersion(version);

		return resolveResultDirectory(normalizedProjectId)
				.resolve("rag-" + normalizedProjectId + "-golden-v" + normalizedVersion + "-report.json")
				.normalize();
	}
	
	private Path resolveDatasetDirectory(String projectId) {
		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		return resolveEvalDirectory(normalizedProjectId).resolve(DATASET_DIRECTORY);
	}

	private Path resolveReportsDirectory(String projectId) {
		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		return resolveEvalDirectory(normalizedProjectId).resolve(REPORTS_DIRECTORY);
	}

	private Path resolveProjectDirectory(String projectId) {

		String normalizedProjectId = RagUtil.requireProjectId(projectId);
		Path publishRoot = publishDirectoryProvider.getPublishDirectory();

		if (publishRoot == null) throw new IllegalStateException("Publish directory is not available.");

		return publishRoot.resolve(normalizedProjectId).normalize();
	}

	private static int requireVersion(int version) {

		if (version <= 0) throw new IllegalArgumentException("version must be greater than zero");

		return version;
	}

	private static int parseVersion(String value) {

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException exception) {
			throw new IllegalStateException("Invalid RAG Golden Dataset version: " + value, exception);
		}
	}

}