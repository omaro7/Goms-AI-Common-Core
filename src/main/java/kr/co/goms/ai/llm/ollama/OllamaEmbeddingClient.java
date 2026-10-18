/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import kr.co.goms.ai.json.JsonMapper;
import kr.co.goms.ai.llm.ollama.model.OllamaEmbeddingRequest;
import kr.co.goms.ai.llm.ollama.model.OllamaEmbeddingResponse;
import kr.co.goms.ai.rag.embedding.EmbeddingClient;
import kr.co.goms.ai.rag.embedding.EmbeddingException;
import kr.co.goms.ai.rag.embedding.EmbeddingPurpose;
import kr.co.goms.ai.rag.embedding.EmbeddingRequest;
import kr.co.goms.ai.rag.embedding.EmbeddingResponse;

/**
 * Ollama {@code POST /api/embed} API를 사용하는
 * {@link EmbeddingClient} 구현체입니다.
 *
 * <p>주요 책임:</p>
 *
 * <ul>
 *     <li>공통 {@link EmbeddingRequest}를 Ollama 요청 DTO로 변환</li>
 *     <li>Ollama {@code /api/embed} 호출</li>
 *     <li>JSON 응답 역직렬화</li>
 *     <li>임베딩 벡터 검증</li>
 *     <li>선택적 L2 정규화</li>
 *     <li>공통 {@link EmbeddingResponse}로 변환</li>
 * </ul>
 */
public final class OllamaEmbeddingClient
    implements EmbeddingClient {

    private static final String EMBED_PATH = "/api/embed";

    private static final Duration DEFAULT_CONNECT_TIMEOUT =
        Duration.ofSeconds(10);

    private static final Duration DEFAULT_REQUEST_TIMEOUT =
        Duration.ofMinutes(2);

    private static final double NORMALIZATION_EPSILON = 1.0e-12;

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final URI embedEndpoint;
    private final Duration requestTimeout;

    /**
     * 기본 HTTP 클라이언트를 생성하여 사용합니다.
     *
     * @param configuration Ollama 연결 설정
     * @param jsonMapper JSON 변환기
     */
    public OllamaEmbeddingClient(
        OllamaConfiguration configuration,
        JsonMapper jsonMapper
    ) {
        this(
            createDefaultHttpClient(configuration),
            configuration,
            jsonMapper
        );
    }

    /**
     * 외부에서 생성한 HTTP 클라이언트를 사용합니다.
     *
     * <p>단위 테스트나 공통 HTTP 설정을 적용할 때 사용합니다.</p>
     *
     * @param httpClient HTTP 클라이언트
     * @param configuration Ollama 연결 설정
     * @param jsonMapper JSON 변환기
     */
    public OllamaEmbeddingClient(
        HttpClient httpClient,
        OllamaConfiguration configuration,
        JsonMapper jsonMapper
    ) {
        this.httpClient = Objects.requireNonNull(
            httpClient,
            "httpClient must not be null"
        );

        this.jsonMapper = Objects.requireNonNull(
            jsonMapper,
            "jsonMapper must not be null"
        );

        Objects.requireNonNull(
            configuration,
            "configuration must not be null"
        );

        this.embedEndpoint = createEmbedEndpoint(
            configuration.getBaseUrl()
        );

        Duration configuredTimeout =
            configuration.getRequestTimeout();

        this.requestTimeout = configuredTimeout == null
            || configuredTimeout.isZero()
            || configuredTimeout.isNegative()
            ? DEFAULT_REQUEST_TIMEOUT
            : configuredTimeout;
    }

    @Override
    public EmbeddingResponse embed(
        EmbeddingRequest request
    ) throws EmbeddingException {

        validateRequest(request);

        OllamaEmbeddingRequest ollamaRequest =
            toOllamaRequest(request);

        String requestBody = serializeRequest(
            request,
            ollamaRequest
        );

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(embedEndpoint)
            .timeout(requestTimeout)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(requestBody)
            )
            .build();

        long startedAt = System.nanoTime();

        try {
            HttpResponse<String> httpResponse =
                httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
                );

            long clientDurationNanos =
                System.nanoTime() - startedAt;

            validateHttpResponse(
                request,
                httpResponse
            );

            OllamaEmbeddingResponse ollamaResponse =
                deserializeResponse(
                    request,
                    httpResponse.body()
                );

            return toEmbeddingResponse(
                request,
                ollamaResponse,
                clientDurationNanos
            );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new EmbeddingException(
                "Ollama embedding request was interrupted",
                request.getModel(),
                request.getRequestId(),
                0,
                true,
                exception
            );

        } catch (ConnectException exception) {
            throw new EmbeddingException(
                "Failed to connect to Ollama: "
                    + embedEndpoint,
                request.getModel(),
                request.getRequestId(),
                0,
                true,
                exception
            );

        } catch (IOException exception) {
            throw new EmbeddingException(
                "I/O error while calling Ollama embedding API",
                request.getModel(),
                request.getRequestId(),
                0,
                true,
                exception
            );
        }
    }

    /**
     * 공통 임베딩 요청을 Ollama API 요청으로 변환합니다.
     */
    private OllamaEmbeddingRequest toOllamaRequest(
        EmbeddingRequest request
    ) {
        List<String> inputs = prepareInputs(request);

        return OllamaEmbeddingRequest.builder()
            .model(request.getModel())
            .input(inputs)
            .truncate(request.isTruncate())
            .build();
    }

    /**
     * 임베딩 목적과 모델 특성에 따라 입력 텍스트를 준비합니다.
     *
     * <p>현재 기본 구현에서는 원문을 그대로 전달합니다. 특정 모델이
     * query/passage 접두어를 요구하면 이 메서드에서 처리할 수 있습니다.</p>
     */
    private List<String> prepareInputs(
        EmbeddingRequest request
    ) {
        List<String> prepared =
            new ArrayList<>(request.size());

        for (String input : request.getInputs()) {
            prepared.add(
                prepareInput(
                    request.getModel(),
                    request.getPurpose(),
                    input
                )
            );
        }

        return List.copyOf(prepared);
    }

    /**
     * 모델별 검색 접두어를 적용할 수 있는 확장 지점입니다.
     *
     * <p>{@code bge-m3}와 {@code nomic-embed-text}의 사용 전략이
     * 달라질 수 있으므로 모델명을 기반으로 선택적으로 처리합니다.</p>
     */
    protected String prepareInput(
        String model,
        EmbeddingPurpose purpose,
        String input
    ) {
        String normalizedInput = requireText(
            input,
            "input"
        );

        /*
         * 기본값은 입력 원문을 그대로 전달합니다.
         *
         * 모델별 접두어가 필요할 경우 예:
         *
         * if (isNomicModel(model)) {
         *     return purpose == EmbeddingPurpose.QUERY
         *         ? "search_query: " + normalizedInput
         *         : "search_document: " + normalizedInput;
         * }
         */

        return normalizedInput;
    }

    /**
     * 요청 DTO를 JSON 문자열로 변환합니다.
     */
    private String serializeRequest(
        EmbeddingRequest request,
        OllamaEmbeddingRequest ollamaRequest
    ) throws EmbeddingException {

        try {
            return jsonMapper.toJson(ollamaRequest);

        } catch (RuntimeException exception) {
            throw new EmbeddingException(
                "Failed to serialize Ollama embedding request",
                request.getModel(),
                request.getRequestId(),
                exception
            );
        }
    }

    /**
     * Ollama 응답 JSON을 응답 DTO로 변환합니다.
     */
    private OllamaEmbeddingResponse deserializeResponse(
        EmbeddingRequest request,
        String responseBody
    ) throws EmbeddingException {

        if (responseBody == null || responseBody.isBlank()) {
            throw new EmbeddingException(
                "Ollama embedding response body is empty",
                request.getModel(),
                request.getRequestId()
            );
        }

        try {
            OllamaEmbeddingResponse response =
                jsonMapper.fromJson(
                    responseBody,
                    OllamaEmbeddingResponse.class
                );

            if (response == null) {
                throw new EmbeddingException(
                    "Ollama embedding response could not be parsed",
                    request.getModel(),
                    request.getRequestId()
                );
            }

            return response;

        } catch (EmbeddingException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new EmbeddingException(
                "Failed to deserialize Ollama embedding response",
                request.getModel(),
                request.getRequestId(),
                exception
            );
        }
    }

    /**
     * Ollama 응답을 공통 임베딩 응답으로 변환합니다.
     */
    private EmbeddingResponse toEmbeddingResponse(
        EmbeddingRequest request,
        OllamaEmbeddingResponse ollamaResponse,
        long clientDurationNanos
    ) throws EmbeddingException {

        try {
            ollamaResponse.validate();

            List<float[]> vectors =
                ollamaResponse.getEmbeddingsAsFloatArrays();

            if (vectors.size() != request.size()) {
                throw new EmbeddingException(
                    "Ollama embedding count mismatch. expected="
                        + request.size()
                        + ", actual="
                        + vectors.size(),
                    request.getModel(),
                    request.getRequestId()
                );
            }

            boolean normalized = false;

            if (request.isNormalize()) {
                vectors = normalizeAll(vectors);
                normalized = true;
            } else {
                vectors = copyVectors(vectors);
            }

            long totalDurationNanos =
                ollamaResponse.getTotalDuration();

            /*
             * 구형 서버나 테스트 응답에서 total_duration이 없을 경우
             * 클라이언트 측 측정 시간을 사용합니다.
             */
            if (totalDurationNanos <= 0) {
                totalDurationNanos = clientDurationNanos;
            }

            String responseModel =
                ollamaResponse.getModel();

            if (responseModel == null
                || responseModel.isBlank()) {

                responseModel = request.getModel();
            }

            EmbeddingResponse response =
                EmbeddingResponse.builder()
                    .model(responseModel)
                    .embeddings(vectors)
                    .requestId(request.getRequestId())
                    .inputTokenCount(
                        ollamaResponse.getPromptEvalCount()
                    )
                    .totalDurationNanos(totalDurationNanos)
                    .loadDurationNanos(
                        ollamaResponse.getLoadDuration()
                    )
                    .createdAt(System.currentTimeMillis())
                    .normalized(normalized)
                    .build();

            validateResponse(request, response);

            return response;

        } catch (EmbeddingException exception) {
            throw exception;

        } catch (IllegalArgumentException
                 | IllegalStateException exception) {

            throw new EmbeddingException(
                "Invalid Ollama embedding response",
                request.getModel(),
                request.getRequestId(),
                exception
            );
        }
    }

    /**
     * HTTP 상태 코드를 검증합니다.
     */
    private void validateHttpResponse(
        EmbeddingRequest request,
        HttpResponse<String> response
    ) throws EmbeddingException {

        int statusCode = response.statusCode();

        if (statusCode >= 200 && statusCode < 300) {
            return;
        }

        boolean retryable = isRetryableStatus(statusCode);

        String errorMessage = extractErrorMessage(
            response.body()
        );

        throw new EmbeddingException(
            "Ollama embedding API failed. status="
                + statusCode
                + ", error="
                + errorMessage,
            request.getModel(),
            request.getRequestId(),
            statusCode,
            retryable
        );
    }

    /**
     * Ollama 오류 응답에서 error 필드를 추출합니다.
     *
     * <p>오류 DTO를 별도로 두지 않고 작은 내부 클래스로 처리합니다.</p>
     */
    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "empty response body";
        }

        try {
            OllamaErrorResponse errorResponse =
                jsonMapper.fromJson(
                    responseBody,
                    OllamaErrorResponse.class
                );

            if (errorResponse != null
                && errorResponse.getError() != null
                && !errorResponse.getError().isBlank()) {

                return errorResponse.getError().trim();
            }

        } catch (RuntimeException ignored) {
            // JSON 오류 응답이 아니면 원문 일부를 사용합니다.
        }

        return abbreviate(responseBody, 500);
    }

    /**
     * 모든 임베딩 벡터를 L2 정규화합니다.
     */
    private List<float[]> normalizeAll(
        List<float[]> vectors
    ) throws EmbeddingException {

        List<float[]> normalized =
            new ArrayList<>(vectors.size());

        for (int index = 0;
             index < vectors.size();
             index++) {

            try {
                normalized.add(
                    normalizeVector(vectors.get(index))
                );

            } catch (IllegalArgumentException exception) {
                throw new EmbeddingException(
                    "Failed to normalize embedding at index "
                        + index,
                    "",
                    "",
                    exception
                );
            }
        }

        return List.copyOf(normalized);
    }

    /**
     * 벡터를 L2 정규화합니다.
     *
     * <pre>
     * normalized[i] = vector[i] / sqrt(sum(vector[i]^2))
     * </pre>
     */
    private float[] normalizeVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException(
                "vector must not be null or empty"
            );
        }

        double squaredSum = 0.0;

        for (int index = 0;
             index < vector.length;
             index++) {

            float value = vector[index];

            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(
                    "vector contains invalid value at index "
                        + index
                );
            }

            squaredSum += (double) value * value;
        }

        double magnitude = Math.sqrt(squaredSum);

        if (!Double.isFinite(magnitude)
            || magnitude <= NORMALIZATION_EPSILON) {

            throw new IllegalArgumentException(
                "vector magnitude must be greater than zero"
            );
        }

        float[] normalized =
            new float[vector.length];

        for (int index = 0;
             index < vector.length;
             index++) {

            normalized[index] =
                (float) (vector[index] / magnitude);
        }

        return normalized;
    }

    /**
     * 벡터 목록을 방어적으로 복사합니다.
     */
    private List<float[]> copyVectors(
        List<float[]> vectors
    ) {
        List<float[]> copies =
            new ArrayList<>(vectors.size());

        for (float[] vector : vectors) {
            copies.add(vector.clone());
        }

        return List.copyOf(copies);
    }

    /**
     * 요청 필수값과 클라이언트 지원 여부를 검증합니다.
     */
    private void validateRequest(
        EmbeddingRequest request
    ) throws EmbeddingException {

        if (request == null) {
            throw new EmbeddingException(
                "Embedding request must not be null"
            );
        }

        if (!supports(request.getModel())) {
            throw new EmbeddingException(
                "Unsupported embedding model: "
                    + request.getModel(),
                request.getModel(),
                request.getRequestId()
            );
        }

        if (request.getInputs() == null
            || request.getInputs().isEmpty()) {

            throw new EmbeddingException(
                "Embedding request inputs must not be empty",
                request.getModel(),
                request.getRequestId()
            );
        }

        /*
         * Ollama /api/embed 자체에는 dimensions 축소 필드가 없습니다.
         * 잘못된 기대를 방지하기 위해 명시적으로 차단합니다.
         */
        if (request.hasDimensions()) {
            throw new EmbeddingException(
                "Ollama /api/embed does not support custom dimensions: "
                    + request.getDimensions(),
                request.getModel(),
                request.getRequestId()
            );
        }
    }

    @Override
    public boolean supports(String model) {
        return model != null && !model.isBlank();
    }

    /**
     * Ollama 연결 가능 여부를 간단히 확인합니다.
     *
     * <p>모델 존재 여부까지 확인하려면 별도의 HealthService 또는
     * {@code /api/tags} 조회를 사용하는 것이 적절합니다.</p>
     */
    @Override
    public boolean isAvailable(String model) {
        if (!supports(model)) {
            return false;
        }

        /*
         * embed()를 호출하지 않고 서버 상태만 확인하려면
         * OllamaHealthService를 주입하는 구조로 확장할 수 있습니다.
         */
        return true;
    }

    private static HttpClient createDefaultHttpClient(
        OllamaConfiguration configuration
    ) {
        Objects.requireNonNull(
            configuration,
            "configuration must not be null"
        );

        Duration connectTimeout =
            configuration.getConnectTimeout();

        if (connectTimeout == null
            || connectTimeout.isZero()
            || connectTimeout.isNegative()) {

            connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        }

        return HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }

    /**
     * 설정의 Base URL에 {@code /api/embed} 경로를 결합합니다.
     *
     * 지원 형태:
     *
     * <ul>
     *     <li>{@code http://localhost:11434}</li>
     *     <li>{@code http://localhost:11434/}</li>
     *     <li>{@code http://localhost:11434/api}</li>
     * </ul>
     */
    private static URI createEmbedEndpoint(String baseUrl) {
        String normalized = requireText(
            baseUrl,
            "baseUrl"
        );

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(
                0,
                normalized.length() - 1
            );
        }

        if (normalized.endsWith("/api")) {
            normalized = normalized.substring(
                0,
                normalized.length() - 4
            );
        }

        URI endpoint = URI.create(
            normalized + EMBED_PATH
        );

        String scheme = endpoint.getScheme();

        if (!"http".equalsIgnoreCase(scheme)
            && !"https".equalsIgnoreCase(scheme)) {

            throw new IllegalArgumentException(
                "Ollama baseUrl must use http or https: "
                    + baseUrl
            );
        }

        return endpoint;
    }

    private static boolean isRetryableStatus(int statusCode) {
        return statusCode == 408
            || statusCode == 425
            || statusCode == 429
            || statusCode >= 500;
    }

    private static String abbreviate(
        String value,
        int maximumLength
    ) {
        String normalized = value == null
            ? ""
            : value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();

        if (normalized.length() <= maximumLength) {
            return normalized;
        }

        return normalized.substring(0, maximumLength)
            + "...";
    }

    private static String requireText(
        String value,
        String fieldName
    ) {
        String normalized =
            value == null ? "" : value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }

        return normalized;
    }

    public URI getEmbedEndpoint() {
        return embedEndpoint;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Ollama 공통 오류 응답 DTO입니다.
     *
     * <pre>
     * {
     *   "error": "model not found"
     * }
     * </pre>
     */
    private static final class OllamaErrorResponse {

        private String error;

        public OllamaErrorResponse() {
        }

        public String getError() {
            return error;
        }

        @SuppressWarnings("unused")
        public void setError(String error) {
            this.error = error;
        }
    }
}