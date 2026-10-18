/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ollama Embedding API 응답 모델입니다.
 *
 * <p>Ollama {@code POST /api/embed} 응답 JSON을 역직렬화하는
 * 전송 전용 DTO입니다.</p>
 *
 * <pre>
 * {
 *   "model": "bge-m3",
 *   "embeddings": [
 *     [0.012, -0.128, 0.443]
 *   ],
 *   "total_duration": 14143917,
 *   "load_duration": 1019500,
 *   "prompt_eval_count": 8
 * }
 * </pre>
 */
public final class OllamaEmbeddingResponse {

    /**
     * 실제 응답에 사용된 모델명입니다.
     */
    private String model;

    /**
     * 입력별 임베딩 벡터 목록입니다.
     *
     * <p>Ollama JSON 필드명과 동일하게 유지합니다.</p>
     */
    private List<List<Double>> embeddings;

    /**
     * 전체 요청 처리 시간입니다.
     *
     * <p>단위는 나노초입니다.</p>
     */
    private long total_duration;

    /**
     * 모델 로딩 시간입니다.
     *
     * <p>단위는 나노초입니다.</p>
     */
    private long load_duration;

    /**
     * 임베딩 입력 처리에 사용된 토큰 수입니다.
     */
    private long prompt_eval_count;

    /**
     * Gson 역직렬화를 위한 기본 생성자입니다.
     */
    public OllamaEmbeddingResponse() {
    }

    public String getModel() {
        return normalize(model);
    }

    public void setModel(String model) {
        this.model = model;
    }

    /**
     * 임베딩 벡터 목록을 반환합니다.
     *
     * <p>외부에서 응답 객체의 내부 상태를 변경하지 못하도록
     * 읽기 전용 복사본을 반환합니다.</p>
     */
    public List<List<Double>> getEmbeddings() {
        if (embeddings == null || embeddings.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<Double>> result =
            new ArrayList<>(embeddings.size());

        for (List<Double> embedding : embeddings) {
            if (embedding == null) {
                result.add(Collections.emptyList());
                continue;
            }

            result.add(
                Collections.unmodifiableList(
                    new ArrayList<>(embedding)
                )
            );
        }

        return Collections.unmodifiableList(result);
    }

    public void setEmbeddings(
        List<List<Double>> embeddings
    ) {
        this.embeddings = embeddings;
    }

    public long getTotalDuration() {
        return total_duration;
    }

    public void setTotalDuration(long totalDuration) {
        if (totalDuration < 0) {
            throw new IllegalArgumentException(
                "totalDuration must be greater than or equal to zero"
            );
        }

        this.total_duration = totalDuration;
    }

    public long getLoadDuration() {
        return load_duration;
    }

    public void setLoadDuration(long loadDuration) {
        if (loadDuration < 0) {
            throw new IllegalArgumentException(
                "loadDuration must be greater than or equal to zero"
            );
        }

        this.load_duration = loadDuration;
    }

    public long getPromptEvalCount() {
        return prompt_eval_count;
    }

    public void setPromptEvalCount(long promptEvalCount) {
        if (promptEvalCount < 0) {
            throw new IllegalArgumentException(
                "promptEvalCount must be greater than or equal to zero"
            );
        }

        this.prompt_eval_count = promptEvalCount;
    }

    /**
     * 응답에 임베딩 벡터가 포함되어 있는지 확인합니다.
     */
    public boolean hasEmbeddings() {
        return embeddings != null && !embeddings.isEmpty();
    }

    /**
     * 응답에 포함된 임베딩 벡터 개수를 반환합니다.
     */
    public int size() {
        return embeddings == null
            ? 0
            : embeddings.size();
    }

    /**
     * 배치 임베딩 응답인지 확인합니다.
     */
    public boolean isBatch() {
        return size() > 1;
    }

    /**
     * 첫 번째 임베딩 벡터의 차원을 반환합니다.
     *
     * <p>응답이 없으면 0을 반환합니다.</p>
     */
    public int getDimensions() {
        if (!hasEmbeddings()) {
            return 0;
        }

        List<Double> firstEmbedding = embeddings.get(0);

        return firstEmbedding == null
            ? 0
            : firstEmbedding.size();
    }

    /**
     * 특정 위치의 임베딩을 float 배열로 변환합니다.
     *
     * @param index 임베딩 인덱스
     * @return float 배열로 변환한 임베딩
     */
    public float[] getEmbeddingAsFloatArray(int index) {
        if (embeddings == null) {
            throw new IndexOutOfBoundsException(
                "Embedding response is empty"
            );
        }

        List<Double> embedding = embeddings.get(index);

        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalStateException(
                "Embedding is null or empty at index " + index
            );
        }

        float[] result = new float[embedding.size()];

        for (int dimensionIndex = 0;
             dimensionIndex < embedding.size();
             dimensionIndex++) {

            Double value = embedding.get(dimensionIndex);

            if (value == null || !Double.isFinite(value)) {
                throw new IllegalStateException(
                    "Invalid embedding value at embeddingIndex="
                        + index
                        + ", dimensionIndex="
                        + dimensionIndex
                );
            }

            result[dimensionIndex] = value.floatValue();
        }

        return result;
    }

    /**
     * 첫 번째 임베딩을 float 배열로 반환합니다.
     */
    public float[] getEmbeddingAsFloatArray() {
        return getEmbeddingAsFloatArray(0);
    }

    /**
     * 모든 임베딩을 float 배열 목록으로 변환합니다.
     */
    public List<float[]> getEmbeddingsAsFloatArrays() {
        if (!hasEmbeddings()) {
            return Collections.emptyList();
        }

        List<float[]> result =
            new ArrayList<>(embeddings.size());

        for (int index = 0;
             index < embeddings.size();
             index++) {

            result.add(
                getEmbeddingAsFloatArray(index)
            );
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * 모든 임베딩 벡터의 차원이 동일한지 검증합니다.
     */
    public void validate() {
        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                "Ollama embedding response model must not be blank"
            );
        }

        if (!hasEmbeddings()) {
            throw new IllegalStateException(
                "Ollama embedding response does not contain embeddings"
            );
        }

        int expectedDimensions = -1;

        for (int embeddingIndex = 0;
             embeddingIndex < embeddings.size();
             embeddingIndex++) {

            List<Double> embedding =
                embeddings.get(embeddingIndex);

            if (embedding == null || embedding.isEmpty()) {
                throw new IllegalStateException(
                    "Embedding must not be null or empty at index "
                        + embeddingIndex
                );
            }

            if (expectedDimensions < 0) {
                expectedDimensions = embedding.size();
            } else if (embedding.size() != expectedDimensions) {
                throw new IllegalStateException(
                    "Embedding dimension mismatch. expected="
                        + expectedDimensions
                        + ", actual="
                        + embedding.size()
                        + ", index="
                        + embeddingIndex
                );
            }

            validateValues(
                embedding,
                embeddingIndex
            );
        }
    }

    private void validateValues(
        List<Double> embedding,
        int embeddingIndex
    ) {
        for (int dimensionIndex = 0;
             dimensionIndex < embedding.size();
             dimensionIndex++) {

            Double value = embedding.get(dimensionIndex);

            if (value == null || !Double.isFinite(value)) {
                throw new IllegalStateException(
                    "Invalid embedding value at embeddingIndex="
                        + embeddingIndex
                        + ", dimensionIndex="
                        + dimensionIndex
                );
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "OllamaEmbeddingResponse{" +
            "model='" + model + '\'' +
            ", embeddingCount=" + size() +
            ", dimensions=" + getDimensions() +
            ", totalDuration=" + total_duration +
            ", loadDuration=" + load_duration +
            ", promptEvalCount=" + prompt_eval_count +
            '}';
    }
}