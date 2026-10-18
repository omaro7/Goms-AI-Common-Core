/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.benchmark;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.rag.embedding.EmbeddingClient;
import kr.co.goms.ai.rag.embedding.EmbeddingException;
import kr.co.goms.ai.rag.embedding.EmbeddingModelProvider;
import kr.co.goms.ai.rag.eval.RagEvaluationCase;
import kr.co.goms.ai.rag.eval.RagEvaluationCaseType;
import kr.co.goms.ai.rag.eval.dataset.RagEvaluationDataset;
import kr.co.goms.ai.rag.eval.dataset.RagEvaluationDatasetLoader;
import kr.co.goms.ai.rag.vector.InMemoryVectorStore;
import kr.co.goms.ai.rag.vector.VectorRecord;
import kr.co.goms.ai.rag.vector.VectorSearchRequest;
import kr.co.goms.ai.rag.vector.VectorSimilarityType;
import kr.co.goms.ai.rag.vector.VectorStore;
import kr.co.goms.ai.rag.vector.VectorStoreException;

/**
 * InMemoryVectorStore와 QdrantVectorStore의 순수 Vector Search Latency를 비교합니다.
 *
 * Query Embedding 시간은 Benchmark 대상에서 제외합니다.
 *
 * 동일한 Query Vector와 동일한 VectorRecord를 사용하여
 * Vector Store 구현체 자체의 Search Latency만 비교합니다.
 */
public final class DefaultVectorStoreBenchmarkService implements VectorStoreBenchmarkService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int DEFAULT_WARMUP_ITERATIONS = 3;
    private static final int DEFAULT_MEASUREMENT_ITERATIONS = 20;

    private final RagEvaluationDatasetLoader datasetLoader;
    private final EmbeddingClient embeddingClient;
    private final EmbeddingModelProvider embeddingModelProvider;
    private final VectorStore qdrantVectorStore;

    public DefaultVectorStoreBenchmarkService(
            RagEvaluationDatasetLoader datasetLoader,
            EmbeddingClient embeddingClient,
            EmbeddingModelProvider embeddingModelProvider,
            VectorStore qdrantVectorStore) {

        this.datasetLoader = Objects.requireNonNull(datasetLoader, "datasetLoader must not be null");
        this.embeddingClient = Objects.requireNonNull(embeddingClient, "embeddingClient must not be null");
        this.embeddingModelProvider = Objects.requireNonNull(embeddingModelProvider, "embeddingModelProvider must not be null");
        this.qdrantVectorStore = Objects.requireNonNull(qdrantVectorStore, "qdrantVectorStore must not be null");
    }

    @Override
    public VectorStoreBenchmarkReport benchmark(Path datasetPath) throws IOException, VectorStoreException, EmbeddingException {

        Objects.requireNonNull(datasetPath, "datasetPath must not be null");

        RagEvaluationDataset dataset = datasetLoader.load(datasetPath);
        String projectId = requireText(dataset.getProjectId(), "projectId");
        String model = requireText(embeddingModelProvider.getModel(), "model");

        List<BenchmarkQuery> queries = createBenchmarkQueries(dataset, model);

        if (queries.isEmpty()) throw new IllegalStateException("No ANSWERABLE benchmark queries found.");

        List<VectorRecord> records = qdrantVectorStore.findByProjectAndModel(projectId, model);

        if (records == null || records.isEmpty()) {
            throw new IllegalStateException("No Qdrant vectors found for projectId=" + projectId + ", model=" + model);
        }

        InMemoryVectorStore memoryVectorStore = new InMemoryVectorStore();

        memoryVectorStore.saveAll(records);

        warmup(memoryVectorStore, queries, projectId, model);
        warmup(qdrantVectorStore, queries, projectId, model);

        VectorBenchmarkResult memoryResult =
                measure(
                        "VECTOR_MEMORY_V1",
                        "InMemoryVectorStore",
                        memoryVectorStore,
                        queries,
                        projectId,
                        model,
                        records.size());

        VectorBenchmarkResult qdrantResult =
                measure(
                        "VECTOR_QDRANT_V1",
                        "QdrantVectorStore",
                        qdrantVectorStore,
                        queries,
                        projectId,
                        model,
                        records.size());

        return new VectorStoreBenchmarkReport(
                dataset.getName(),
                projectId,
                model,
                DEFAULT_TOP_K,
                memoryResult,
                qdrantResult);
    }

    private List<BenchmarkQuery> createBenchmarkQueries(RagEvaluationDataset dataset, String model) throws EmbeddingException {

        List<BenchmarkQuery> result = new ArrayList<>();

        for (RagEvaluationCase evaluationCase : dataset.getCases()) {

            if (evaluationCase == null) continue;
            if (evaluationCase.getType() != RagEvaluationCaseType.ANSWERABLE) continue;

            String question = evaluationCase.getQuestion();
            float[] vector = embeddingClient.embedQuery(model, question);

            if (vector == null || vector.length == 0) {
                throw new IllegalStateException("Query embedding is empty: " + evaluationCase.getId());
            }

            result.add(new BenchmarkQuery(evaluationCase.getId(), question, vector));
        }

        return List.copyOf(result);
    }

    private void warmup(VectorStore vectorStore, List<BenchmarkQuery> queries, String projectId, String model)  throws VectorStoreException {

        for (int iteration = 0; iteration < DEFAULT_WARMUP_ITERATIONS; iteration++) {
            for (BenchmarkQuery query : queries) {
                vectorStore.search(createSearchRequest(projectId, model, query.vector()));
            }
        }
    }

    private VectorBenchmarkResult measure(
            String experimentId,
            String backend,
            VectorStore vectorStore,
            List<BenchmarkQuery> queries,
            String projectId,
            String model,
            int vectorCount) throws VectorStoreException {

        List<Long> latencies = new ArrayList<>(
                queries.size() * DEFAULT_MEASUREMENT_ITERATIONS);

        for (int iteration = 0; iteration < DEFAULT_MEASUREMENT_ITERATIONS; iteration++) {

            for (BenchmarkQuery query : queries) {

                VectorSearchRequest request =
                        createSearchRequest(
                                projectId,
                                model,
                                query.vector());

                long startedAt = System.nanoTime();

                vectorStore.search(request);

                long durationNanos = System.nanoTime() - startedAt;

                latencies.add(durationNanos);
            }
        }

        VectorBenchmarkLatency latency = summarize(latencies);

        return new VectorBenchmarkResult(
                experimentId,
                backend,
                projectId,
                model,
                vectorCount,
                queries.size(),
                DEFAULT_WARMUP_ITERATIONS,
                DEFAULT_MEASUREMENT_ITERATIONS,
                latency);
    }

    private VectorSearchRequest createSearchRequest(String projectId, String model, float[] queryVector) {

        return VectorSearchRequest.builder()
                .projectId(projectId)
                .queryVector(queryVector)
                .model(model)
                .topK(DEFAULT_TOP_K)
                .minimumScore(0.0)
                .similarityType(VectorSimilarityType.COSINE)
                .build();
    }

    private VectorBenchmarkLatency summarize(List<Long> latencies) {

        if (latencies == null || latencies.isEmpty()) {
            return new VectorBenchmarkLatency(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        List<Long> sorted = new ArrayList<>(latencies);

        Collections.sort(sorted);

        long total = 0L;

        for (long latency : sorted) total += latency;

        double averageMs = nanosToMillis((double) total / sorted.size());
        double minMs = nanosToMillis(sorted.get(0));
        double maxMs = nanosToMillis(sorted.get(sorted.size() - 1));
        double p50Ms = nanosToMillis(percentile(sorted, 0.50));
        double p95Ms = nanosToMillis(percentile(sorted, 0.95));
        double p99Ms = nanosToMillis(percentile(sorted, 0.99));

        return new VectorBenchmarkLatency(
                sorted.size(),
                averageMs,
                minMs,
                maxMs,
                p50Ms,
                p95Ms,
                p99Ms);
    }

    /**
     * Nearest-rank percentile을 사용합니다.
     */
    private long percentile(List<Long> sorted, double percentile) {

        if (sorted.isEmpty()) return 0L;

        int rank = (int) Math.ceil(percentile * sorted.size());
        int index = Math.max(0, Math.min(sorted.size() - 1, rank - 1));

        return sorted.get(index);
    }

    private double nanosToMillis(double nanos) {
        return nanos / 1_000_000.0;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return value.trim();
    }

    private record BenchmarkQuery(String caseId, String question, float[] vector) {

        private BenchmarkQuery {
            caseId = requireText(caseId, "caseId");
            question = requireText(question, "question");
            vector = vector == null ? null : vector.clone();

            if (vector == null || vector.length == 0) throw new IllegalArgumentException("vector must not be empty");
        }

        @Override
        public float[] vector() {
            return vector.clone();
        }
    }
}