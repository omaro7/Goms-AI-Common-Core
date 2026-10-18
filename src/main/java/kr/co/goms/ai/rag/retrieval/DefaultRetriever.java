/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.retrieval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import kr.co.goms.ai.rag.embedding.EmbeddingClient;
import kr.co.goms.ai.rag.embedding.EmbeddingException;
import kr.co.goms.ai.rag.embedding.EmbeddingModelProvider;
import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.vector.VectorRecord;
import kr.co.goms.ai.rag.vector.VectorSearchRequest;
import kr.co.goms.ai.rag.vector.VectorSearchResult;
import kr.co.goms.ai.rag.vector.VectorStore;
import kr.co.goms.ai.rag.vector.VectorStoreException;
import kr.co.goms.ai.util.GomsStringUtil;

/**
 * 기본 {@link Retriever} 구현체입니다.
 *
 * <p>사용자 질의를 임베딩하고 VectorStore를 검색한 후,
 * 설정에 따라 Adaptive Controlled Lexical Rerank를 수행하여
 * 최종 검색 결과를 생성합니다.</p>
 *
 * <pre>
 * query
 *   ↓
 * EmbeddingClient.embedQuery()
 *   ↓
 * VectorSearchRequest
 *   ↓
 * VectorStore.search()
 *   ↓
 * Candidate Top-K
 *   ↓
 * Query Intent
 *   ├─ DEFAULT  → rerankWeight 0.30
 *   └─ LOCATION → rerankWeight 0.58
 *   ↓
 * Adaptive Controlled Lexical Rerank (optional)
 *   ↓
 * Score Threshold
 *   ↓
 * Final Top-K
 *   ↓
 * RetrievalResult
 * </pre>
 *
 * <h2>Pure Vector 정책</h2>
 *
 * <p>{@code rerankEnabled=false}이면 VectorStore에서 반환한 순수 Vector
 * similarity 결과를 lexical boost 없이 그대로 사용합니다.</p>
 *
 * <h2>Adaptive Controlled Lexical Rerank 정책</h2>
 *
 * <p>{@code rerankEnabled=true}이면 Vector similarity를 주 신호로 유지하고
 * Heading, Content, Heading-Content Relation 및 Title Pattern lexical boost를
 * 질의 의도별 가중치로 제한하여 적용합니다.</p>
 *
 * <p>일반 질의는 기본적으로 {@code 0.30}, 장소 식별 질의는
 * {@code 0.58}의 rerank weight를 사용합니다.</p>
 *
 * <p>V5 이전의 score clipping은 적용하지 않습니다.
 * 따라서 lexical boost로 인한 1.0 대량 동점 문제를 방지합니다.</p>
 *
 * <h2>곰스북 1줄 원칙</h2>
 *
 * <p>메서드 선언, 단순 조건문, 변수 선언, return 및 단순 메서드 호출은
 * 가능한 한 한 줄로 작성합니다.</p>
 *
 * <p>Builder, Stream 및 복합 처리처럼 여러 단계의 의미를 가지는 코드는
 * 가독성을 위해 의미 단위로 줄을 나눕니다.</p>
 */
public final class DefaultRetriever implements Retriever {

    /**
     * Hybrid Rerank 사용 시 Raw Vector 후보 확장 배수입니다.
     */
    private static final int CANDIDATE_MULTIPLIER = 10;

    /**
     * 일반 질의의 기본 Controlled Lexical Rerank 가중치입니다.
     */
    private static final double DEFAULT_RERANK_WEIGHT = 0.30;

    /**
     * 장소 식별 질의의 기본 Controlled Lexical Rerank 가중치입니다.
     */
    private static final double DEFAULT_LOCATION_RERANK_WEIGHT = 0.58;

    private static final double EXACT_HEADING_BOOST = 0.18;
    private static final double HEADING_KEYWORD_BOOST = 0.06;
    private static final double CONTENT_KEYWORD_BOOST = 0.04;
    private static final double HEADING_CONTENT_BOOST = 0.05;
    private static final double TITLE_PATTERN_BOOST = 0.10;

    private static final double MAX_HEADING_KEYWORD_BOOST = 0.18;
    private static final double MAX_CONTENT_KEYWORD_BOOST = 0.16;

    /**
     * 질의 Embedding Vector를 생성합니다.
     */
    private final EmbeddingClient embeddingClient;

    /**
     * 현재 Embedding Model을 제공합니다.
     */
    private final EmbeddingModelProvider embeddingModelProvider;

    /**
     * Vector 검색을 수행합니다.
     */
    private final VectorStore vectorStore;

    /**
     * 기본 검색 결과 개수입니다.
     */
    private final int defaultTopK;

    /**
     * 기본 최소 유사도 점수입니다.
     */
    private final double defaultMinimumScore;

    /**
     * Vector 검색 이후 Adaptive Controlled Lexical Rerank 적용 여부입니다.
     */
    private final boolean rerankEnabled;

    /**
     * DEFAULT 질의의 Controlled Lexical Rerank 가중치입니다.
     */
    private final double rerankWeight;

    /**
     * LOCATION 질의의 Controlled Lexical Rerank 가중치입니다.
     */
    private final double locationRerankWeight;

    /**
     * 기본 Retriever를 생성합니다.
     *
     * <p>Adaptive Controlled Lexical Rerank를 활성화하고
     * DEFAULT=0.30, LOCATION=0.58을 사용합니다.</p>
     *
     * @param embeddingClient 질의 임베딩 클라이언트
     * @param embeddingModelProvider 임베딩 모델 제공자
     * @param vectorStore 벡터 저장소
     */
    public DefaultRetriever(EmbeddingClient embeddingClient, EmbeddingModelProvider embeddingModelProvider, VectorStore vectorStore) {

        this(
                embeddingClient,
                embeddingModelProvider,
                vectorStore,
                RetrievalRequest.DEFAULT_TOP_K,
                RetrievalRequest.DEFAULT_MINIMUM_SCORE,
                true,
                DEFAULT_RERANK_WEIGHT,
                DEFAULT_LOCATION_RERANK_WEIGHT);
    }

    /**
     * 기본 검색 설정을 지정하여 Retriever를 생성합니다.
     *
     * @param embeddingClient 질의 임베딩 클라이언트
     * @param embeddingModelProvider 임베딩 모델 제공자
     * @param vectorStore 벡터 저장소
     * @param defaultTopK 기본 검색 결과 개수
     * @param defaultMinimumScore 기본 최소 유사도 점수
     */
    public DefaultRetriever(EmbeddingClient embeddingClient, EmbeddingModelProvider embeddingModelProvider, VectorStore vectorStore, int defaultTopK, double defaultMinimumScore) {

        this(
                embeddingClient,
                embeddingModelProvider,
                vectorStore,
                defaultTopK,
                defaultMinimumScore,
                true,
                DEFAULT_RERANK_WEIGHT,
                DEFAULT_LOCATION_RERANK_WEIGHT);
    }

    /**
     * 검색 설정과 Rerank 적용 여부를 지정하여 Retriever를 생성합니다.
     *
     * @param embeddingClient 질의 임베딩 클라이언트
     * @param embeddingModelProvider 임베딩 모델 제공자
     * @param vectorStore 벡터 저장소
     * @param defaultTopK 기본 검색 결과 개수
     * @param defaultMinimumScore 기본 최소 유사도 점수
     * @param rerankEnabled Adaptive Controlled Lexical Rerank 적용 여부
     */
    public DefaultRetriever(EmbeddingClient embeddingClient, EmbeddingModelProvider embeddingModelProvider, VectorStore vectorStore, int defaultTopK, double defaultMinimumScore, boolean rerankEnabled) {

        this(
                embeddingClient,
                embeddingModelProvider,
                vectorStore,
                defaultTopK,
                defaultMinimumScore,
                rerankEnabled,
                DEFAULT_RERANK_WEIGHT,
                DEFAULT_LOCATION_RERANK_WEIGHT);
    }

    /**
     * 검색 설정과 DEFAULT Rerank Weight를 지정하여 Retriever를 생성합니다.
     *
     * <p>LOCATION 질의에는 기본값 0.58을 사용합니다.</p>
     *
     * @param embeddingClient 질의 임베딩 클라이언트
     * @param embeddingModelProvider 임베딩 모델 제공자
     * @param vectorStore 벡터 저장소
     * @param defaultTopK 기본 검색 결과 개수
     * @param defaultMinimumScore 기본 최소 유사도 점수
     * @param rerankEnabled Adaptive Controlled Lexical Rerank 적용 여부
     * @param rerankWeight DEFAULT 질의 Rerank Weight
     */
    public DefaultRetriever(EmbeddingClient embeddingClient, EmbeddingModelProvider embeddingModelProvider, VectorStore vectorStore, int defaultTopK, double defaultMinimumScore, boolean rerankEnabled, double rerankWeight) {

        this(
                embeddingClient,
                embeddingModelProvider,
                vectorStore,
                defaultTopK,
                defaultMinimumScore,
                rerankEnabled,
                rerankWeight,
                DEFAULT_LOCATION_RERANK_WEIGHT);
    }

    /**
     * 검색 설정과 질의 의도별 Rerank Weight를 지정하여 Retriever를 생성합니다.
     *
     * @param embeddingClient 질의 임베딩 클라이언트
     * @param embeddingModelProvider 임베딩 모델 제공자
     * @param vectorStore 벡터 저장소
     * @param defaultTopK 기본 검색 결과 개수
     * @param defaultMinimumScore 기본 최소 유사도 점수
     * @param rerankEnabled Adaptive Controlled Lexical Rerank 적용 여부
     * @param rerankWeight DEFAULT 질의 Rerank Weight
     * @param locationRerankWeight LOCATION 질의 Rerank Weight
     */
    public DefaultRetriever(
            EmbeddingClient embeddingClient,
            EmbeddingModelProvider embeddingModelProvider,
            VectorStore vectorStore,
            int defaultTopK,
            double defaultMinimumScore,
            boolean rerankEnabled,
            double rerankWeight,
            double locationRerankWeight) {

        this.embeddingClient = Objects.requireNonNull(embeddingClient, "embeddingClient must not be null");
        this.embeddingModelProvider = Objects.requireNonNull(embeddingModelProvider, "embeddingModelProvider must not be null");
        this.vectorStore = Objects.requireNonNull(vectorStore, "vectorStore must not be null");

        if (defaultTopK < 1) throw new IllegalArgumentException("defaultTopK must be greater than zero");
        if (!Double.isFinite(defaultMinimumScore) || defaultMinimumScore < -1.0 || defaultMinimumScore > 1.0) throw new IllegalArgumentException("defaultMinimumScore must be between -1.0 and 1.0");
        if (!Double.isFinite(rerankWeight) || rerankWeight < 0.0) throw new IllegalArgumentException("rerankWeight must be greater than or equal to zero");
        if (!Double.isFinite(locationRerankWeight) || locationRerankWeight < 0.0) throw new IllegalArgumentException("locationRerankWeight must be greater than or equal to zero");

        this.defaultTopK = defaultTopK;
        this.defaultMinimumScore = defaultMinimumScore;
        this.rerankEnabled = rerankEnabled;
        this.rerankWeight = rerankWeight;
        this.locationRerankWeight = locationRerankWeight;
    }

    /**
     * Project Scope가 없는 검색은 허용하지 않습니다.
     */
    @Override
    public RetrievalResult retrieve(String query) throws RetrievalException {

        String normalizedQuery = validateQuery(query);

        throw new RetrievalException(
                "Project-scoped retrieval requires RetrievalRequest with projectId",
                normalizedQuery,
                "",
                RetrievalOperation.VALIDATE,
                null);
    }

    /**
     * 지정된 검색 조건으로 관련 문서를 검색합니다.
     */
    @Override
    public RetrievalResult retrieve(RetrievalRequest request) throws RetrievalException {

        validateRequest(request);

        String query = request.getQuery();
        String model = resolveEmbeddingModel();

        long startedAt = System.nanoTime();

        try {

            float[] queryVector = embedQuery(query, model);

            VectorSearchRequest searchRequest = createVectorSearchRequest(request, queryVector, model);

            List<VectorSearchResult> searchResults = searchVectorStore(query, model, searchRequest, request.getMinimumScore());

            List<VectorSearchResult> finalResults = prepareResults(searchResults, request);

            return RetrievalResult.builder()
                    .query(query)
                    .model(model)
                    .searchResults(finalResults)
                    .durationNanos(System.nanoTime() - startedAt)
                    .dimensions(queryVector.length)
                    .build();

        } catch (RetrievalException exception) {

            throw exception;

        } catch (RuntimeException exception) {

            throw new RetrievalException(
                    "Unexpected error while retrieving documents",
                    query,
                    model,
                    RetrievalOperation.RESULT_MAPPING,
                    exception);
        }
    }

    /**
     * 사용자 질의를 임베딩합니다.
     */
    private float[] embedQuery(String query, String model) throws RetrievalException {

        try {

            float[] queryVector = embeddingClient.embedQuery(model, query);

            validateQueryVector(queryVector, query, model);

            return queryVector;

        } catch (EmbeddingException exception) {

            throw new RetrievalException(
                    "Failed to embed retrieval query",
                    query,
                    model,
                    RetrievalOperation.EMBED_QUERY,
                    exception);

        } catch (RuntimeException exception) {

            throw new RetrievalException(
                    "Invalid query embedding result",
                    query,
                    model,
                    RetrievalOperation.EMBED_QUERY,
                    exception);
        }
    }

    /**
     * RetrievalRequest를 VectorSearchRequest로 변환합니다.
     *
     * <p>Rerank가 활성화된 경우 최종 Top-K보다 넓은 후보군을 먼저 검색한 후
     * Adaptive Controlled Lexical Rerank에서 최종 순위를 계산합니다.</p>
     *
     * <p>Pure Vector 모드에서는 불필요한 후보 확장을 하지 않고
     * 요청된 Top-K만 검색합니다.</p>
     */
    private VectorSearchRequest createVectorSearchRequest(RetrievalRequest request, float[] queryVector, String model) throws RetrievalException {

        try {

            int candidateTopK = rerankEnabled
                    ? Math.max(request.getTopK(), request.getTopK() * CANDIDATE_MULTIPLIER)
                    : request.getTopK();

            return VectorSearchRequest.builder()
                    .projectId(request.getProjectId())
                    .queryVector(queryVector)
                    .model(model)
                    .topK(candidateTopK)
                    .minimumScore(-1.0)
                    .similarityType(request.getSimilarityType())
                    .chunkTypes(request.getChunkTypes())
                    .sourcePaths(request.getSourcePaths())
                    .epubTypes(request.getEpubTypes())
                    .languages(request.getLanguages())
                    .metadataFilters(request.getMetadataFilters())
                    .includeRejected(true)
                    .build();

        } catch (RuntimeException exception) {

            throw new RetrievalException(
                    "Failed to create vector search request",
                    request.getQuery(),
                    model,
                    RetrievalOperation.VALIDATE,
                    exception);
        }
    }

    /**
     * VectorStore 검색을 수행하고 설정에 따라 Adaptive Controlled Lexical Rerank를 적용합니다.
     *
     * <p>{@code rerankEnabled=false}이면 Raw Vector 검색 결과에 lexical
     * boost를 적용하지 않고 minimumScore만 적용합니다.</p>
     */
    private List<VectorSearchResult> searchVectorStore(String query, String model, VectorSearchRequest searchRequest, double minimumScore) throws RetrievalException {

        try {

            List<VectorSearchResult> searchResults = vectorStore.search(searchRequest);

            if (searchResults == null) {
                throw new RetrievalException(
                        "VectorStore returned null search results",
                        query,
                        model,
                        RetrievalOperation.VECTOR_SEARCH,
                        null);
            }

            if (!rerankEnabled) {

                List<VectorSearchResult> filteredResults = applyScoreThreshold(searchResults, minimumScore);

                logTopKResults("[VECTOR] " + query, filteredResults, minimumScore);

                return List.copyOf(filteredResults);
            }

            logTopKResults("[RAW] " + query, searchResults, -1.0);

            List<VectorSearchResult> rerankedResults = rerank(query, searchResults);

            List<VectorSearchResult> filteredResults = applyScoreThreshold(rerankedResults, minimumScore);

            logTopKResults("[FINAL] " + query, filteredResults, minimumScore);

            return List.copyOf(filteredResults);

        } catch (VectorStoreException exception) {

            throw new RetrievalException(
                    "Failed to search vector store",
                    query,
                    model,
                    RetrievalOperation.VECTOR_SEARCH,
                    exception);
        }
    }

    /**
     * 검색 결과를 최종 정렬하고 요청된 Top-K만 반환합니다.
     */
    private List<VectorSearchResult> prepareResults(List<VectorSearchResult> searchResults, RetrievalRequest request) {

        if (searchResults == null || searchResults.isEmpty()) return List.of();

        List<VectorSearchResult> results = new ArrayList<>(searchResults.size());

        for (VectorSearchResult result : searchResults) {
            if (result != null) results.add(result);
        }

        if (request.isPreserveDocumentOrder()) {

            results.sort(
                    Comparator
                            .comparing((VectorSearchResult result) -> normalizePath(result.getChunk().getSourcePath()))
                            .thenComparingInt(result -> result.getChunk().getSequence())
                            .thenComparing(VectorSearchResult::getId));

        } else {

            results.sort(
                    Comparator
                            .comparingDouble(VectorSearchResult::getScore)
                            .reversed()
                            .thenComparing(VectorSearchResult::getId));
        }

        int resultCount = Math.min(request.getTopK(), results.size());

        List<VectorSearchResult> rankedResults = new ArrayList<>(resultCount);

        for (int index = 0; index < resultCount; index++) rankedResults.add(results.get(index).withRank(index + 1));

        return List.copyOf(rankedResults);
    }

    /**
     * Vector similarity와 lexical 정보를 결합하여 검색 결과를 재정렬합니다.
     *
     * <p>질의 의도를 먼저 판별한 후 DEFAULT 또는 LOCATION 가중치를
     * lexical boost에 적용합니다.</p>
     */
    private List<VectorSearchResult> rerank(String query, List<VectorSearchResult> results) {

        if (results == null || results.isEmpty()) return List.of();

        String normalizedQuery = GomsStringUtil.normalizeForMatch(query);
        List<String> keywords = GomsStringUtil.extractKeywords(query);
        QueryIntent queryIntent = resolveQueryIntent(query);
        double effectiveRerankWeight = resolveRerankWeight(queryIntent);

        System.out.println("[RAG-RERANK] intent=" + queryIntent + ", rerankWeight=" + String.format(Locale.ROOT, "%.4f", effectiveRerankWeight));

        List<VectorSearchResult> reranked = new ArrayList<>(results.size());

        for (VectorSearchResult result : results) {

            if (result == null) continue;

            VectorRecord record = result.getRecord();

            if (record == null) continue;

            DocumentChunk chunk = record.getChunk();

            if (chunk == null) continue;

            double finalScore = calculateRerankScore(normalizedQuery, keywords, result.getScore(), chunk, effectiveRerankWeight);

            reranked.add(
                    VectorSearchResult.builder()
                            .record(record)
                            .score(finalScore)
                            .similarityType(result.getSimilarityType())
                            .accepted(result.isAccepted())
                            .build());
        }

        reranked.sort(
                Comparator
                        .comparingDouble(VectorSearchResult::getScore)
                        .reversed()
                        .thenComparing(VectorSearchResult::getId));

        List<VectorSearchResult> ranked = new ArrayList<>(reranked.size());

        for (int index = 0; index < reranked.size(); index++) ranked.add(reranked.get(index).withRank(index + 1));

        return List.copyOf(ranked);
    }

    /**
     * 단일 Chunk의 Adaptive Controlled Lexical Rerank 점수를 계산합니다.
     */
    private double calculateRerankScore(
            String normalizedQuery,
            List<String> keywords,
            double vectorScore,
            DocumentChunk chunk,
            double effectiveRerankWeight) {

        double lexicalBoost = calculateLexicalBoost(normalizedQuery, keywords, chunk);

        return vectorScore + (lexicalBoost * effectiveRerankWeight);
    }

    /**
     * 단일 Chunk의 lexical boost를 계산합니다.
     */
    private double calculateLexicalBoost(String normalizedQuery, List<String> keywords, DocumentChunk chunk) {

        String heading = GomsStringUtil.normalizeHeading(chunk.getTitle());
        String content = GomsStringUtil.normalizeForMatch(chunk.getContent());

        double headingBoost = 0.0;
        double contentBoost = 0.0;
        double relationBoost = 0.0;
        double patternBoost = 0.0;

        /*
         * 질문에 heading 자체가 포함되어 있으면
         * 해당 section을 강하게 우대합니다.
         */
        if (!heading.isBlank() && heading.length() >= 2 && normalizedQuery.contains(heading)) headingBoost += EXACT_HEADING_BOOST;

        int headingMatches = 0;
        int contentMatches = 0;

        for (String keyword : keywords) {

            if (keyword.isBlank()) continue;

            if (!heading.isBlank() && heading.contains(keyword)) headingMatches++;
            if (!content.isBlank() && content.contains(keyword)) contentMatches++;
        }

        headingBoost += Math.min(MAX_HEADING_KEYWORD_BOOST, headingMatches * HEADING_KEYWORD_BOOST);
        contentBoost += Math.min(MAX_CONTENT_KEYWORD_BOOST, contentMatches * CONTENT_KEYWORD_BOOST);

        /*
         * Heading과 Content가 모두 질의와 관련 있으면
         * section + paragraph 일치로 판단하여 추가 boost합니다.
         */
        if (headingMatches > 0 && contentMatches > 0) relationBoost += HEADING_CONTENT_BOOST;

        /*
         * "제목"을 묻는 질문에서 실제 제목 표기 『...』가
         * 포함된 Chunk를 추가 우대합니다.
         */
        if (GomsStringUtil.containsKeyword(keywords, "제목") && GomsStringUtil.containsTitlePattern(chunk.getContent())) patternBoost += TITLE_PATTERN_BOOST;

        return headingBoost + contentBoost + relationBoost + patternBoost;
    }

    /**
     * 사용자 질의의 검색 의도를 반환합니다.
     *
     * <p>특정 Golden Case나 질문 전체 문자열에 의존하지 않고,
     * 일반적인 한국어 장소 식별 표현만 사용합니다.</p>
     */
    private QueryIntent resolveQueryIntent(String query) {

        if (query == null || query.isBlank()) return QueryIntent.DEFAULT;

        String normalized = query.trim();

        if (isLocationQuery(normalized)) return QueryIntent.LOCATION;

        return QueryIntent.DEFAULT;
    }

    /**
     * 장소 또는 위치 식별 질의 여부를 반환합니다.
     */
    private boolean isLocationQuery(String query) {

        return query.contains("어디")
                || query.contains("어느 곳")
                || query.contains("어떤 곳")
                || query.contains("어느 장소")
                || query.contains("어떤 장소")
                || query.contains("장소는")
                || query.contains("장소가")
                || query.contains("곳은 어디")
                || query.contains("곳이 어디");
    }

    /**
     * 질의 의도에 따른 Controlled Lexical Rerank 가중치를 반환합니다.
     */
    private double resolveRerankWeight(QueryIntent queryIntent) {

        return queryIntent == QueryIntent.LOCATION ? locationRerankWeight : rerankWeight;
    }

    /**
     * Threshold를 적용합니다.
     */
    private List<VectorSearchResult> applyScoreThreshold(List<VectorSearchResult> results, double minimumScore) {

        if (results == null || results.isEmpty()) return List.of();

        List<VectorSearchResult> filtered = new ArrayList<>();

        for (VectorSearchResult result : results) {

            if (result == null) continue;
            if (result.getScore() < minimumScore) continue;

            filtered.add(result);
        }

        List<VectorSearchResult> ranked = new ArrayList<>(filtered.size());

        for (int index = 0; index < filtered.size(); index++) ranked.add(filtered.get(index).withRank(index + 1));

        return List.copyOf(ranked);
    }

    /**
     * 요청을 검증합니다.
     */
    private void validateRequest(RetrievalRequest request) throws RetrievalException {

        if (request == null) {
            throw new RetrievalException(
                    "Retrieval request must not be null",
                    "",
                    "",
                    RetrievalOperation.VALIDATE,
                    null);
        }

        if (request.getProjectId() == null || request.getProjectId().isBlank()) {
            throw new RetrievalException(
                    "Retrieval projectId must not be blank",
                    request.getQuery(),
                    "",
                    RetrievalOperation.VALIDATE,
                    null);
        }

        try {

            validateQuery(request.getQuery());

        } catch (IllegalArgumentException exception) {

            throw new RetrievalException(
                    "Retrieval query is invalid",
                    request.getQuery(),
                    "",
                    RetrievalOperation.VALIDATE,
                    exception);
        }

        if (request.getTopK() < 1) {
            throw new RetrievalException(
                    "Retrieval topK must be greater than zero",
                    request.getQuery(),
                    "",
                    RetrievalOperation.VALIDATE,
                    null);
        }

        if (!Double.isFinite(request.getMinimumScore())) {
            throw new RetrievalException(
                    "Retrieval minimumScore must be finite",
                    request.getQuery(),
                    "",
                    RetrievalOperation.VALIDATE,
                    null);
        }
    }

    /**
     * 현재 임베딩 모델명을 조회합니다.
     */
    private String resolveEmbeddingModel() throws RetrievalException {

        String model;

        try {

            model = embeddingModelProvider.getModel();

        } catch (RuntimeException exception) {

            throw new RetrievalException(
                    "Failed to resolve embedding model",
                    "",
                    "",
                    RetrievalOperation.VALIDATE,
                    exception);
        }

        if (model == null || model.isBlank()) {
            throw new RetrievalException(
                    "Embedding model must not be blank",
                    "",
                    "",
                    RetrievalOperation.VALIDATE,
                    null);
        }

        return model.trim();
    }

    /**
     * 질의 벡터 값을 검증합니다.
     */
    private void validateQueryVector(float[] vector, String query, String model) throws RetrievalException {

        if (vector == null || vector.length == 0) {
            throw new RetrievalException(
                    "Query embedding vector must not be empty",
                    query,
                    model,
                    RetrievalOperation.EMBED_QUERY,
                    null);
        }

        for (int index = 0; index < vector.length; index++) {

            if (!Float.isFinite(vector[index])) {
                throw new RetrievalException(
                        "Query embedding contains invalid value at index " + index,
                        query,
                        model,
                        RetrievalOperation.EMBED_QUERY,
                        null);
            }
        }
    }

    /**
     * Retriever 사용 가능 여부를 반환합니다.
     */
    @Override
    public boolean isAvailable() {

        String model;

        try {

            model = embeddingModelProvider.getModel();

        } catch (RuntimeException exception) {

            return false;
        }

        if (model == null || model.isBlank()) return false;

        return embeddingClient.isAvailable(model) && vectorStore.isAvailable();
    }

    /**
     * Retrieval 진단 로그를 출력합니다.
     */
    private void logTopKResults(String query, List<VectorSearchResult> results, double threshold) {

        System.out.println("[RAG] ========================================");
        System.out.println("[RAG] Retrieval query = " + query);
        System.out.println("[RAG] Result count = " + (results == null ? 0 : results.size()));
        System.out.println("[RAG] Retrieval threshold = " + threshold);
        System.out.println("[RAG] Rerank enabled = " + rerankEnabled);

        if (results == null || results.isEmpty()) {

            System.out.println("[RAG] No retrieval results.");
            System.out.println("[RAG] ========================================");

            return;
        }

        int rank = 1;

        for (VectorSearchResult result : results) {

            if (result == null) continue;

            VectorRecord record = result.getRecord();

            if (record == null) continue;

            DocumentChunk chunk = record.getChunk();

            if (chunk == null) continue;

            System.out.println();
            System.out.println("[RAG][TOP-" + rank + "]");
            System.out.println("[RAG] score      = " + String.format(Locale.ROOT, "%.6f", result.getScore()));
            System.out.println("[RAG] sourcePath = " + chunk.getSourcePath());
            System.out.println("[RAG] chunkId    = " + chunk.getId());
            System.out.println("[RAG] heading    = " + chunk.getTitle());
            System.out.println("[RAG] type       = " + chunk.getType());
            System.out.println("[RAG] sequence   = " + chunk.getSequence());
            System.out.println("[RAG] text       = " + GomsStringUtil.abbreviate(chunk.getContent(), 500));

            rank++;
        }

        System.out.println("[RAG] ========================================");
    }

    public EmbeddingClient getEmbeddingClient() {

        return embeddingClient;
    }

    public EmbeddingModelProvider getEmbeddingModelProvider() {

        return embeddingModelProvider;
    }

    public VectorStore getVectorStore() {

        return vectorStore;
    }

    public int getDefaultTopK() {

        return defaultTopK;
    }

    public double getDefaultMinimumScore() {

        return defaultMinimumScore;
    }

    public boolean isRerankEnabled() {

        return rerankEnabled;
    }

    public double getRerankWeight() {

        return rerankWeight;
    }

    public double getLocationRerankWeight() {

        return locationRerankWeight;
    }

    private static String normalizePath(String path) {

        return path == null ? "" : path.trim().replace('\\', '/');
    }

    /**
     * Adaptive Controlled Lexical Rerank에서 사용하는 질의 의도입니다.
     */
    private enum QueryIntent {

        DEFAULT,
        LOCATION
    }
}