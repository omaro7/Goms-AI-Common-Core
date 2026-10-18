/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.embedding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 임베딩 모델에 전달할 요청 정보입니다.
 *
 * <p>단일 질의 임베딩과 여러 DocumentChunk의 일괄 임베딩을
 * 동일한 요청 모델로 처리합니다.</p>
 *
 * <pre>
 * EmbeddingRequest request = EmbeddingRequest.builder()
 *     .model("bge-m3")
 *     .input("꽃은 피어날 시간을 스스로 알고 있다.")
 *     .build();
 * </pre>
 */
public final class EmbeddingRequest {

    /**
     * 사용할 임베딩 모델명.
     *
     * 예:
     * bge-m3
     * nomic-embed-text
     */
    private final String model;

    /**
     * 임베딩할 입력 목록.
     *
     * 단일 요청도 하나의 요소를 가진 목록으로 관리합니다.
     */
    private final List<String> inputs;

    /**
     * 출력 벡터 정규화 요청 여부.
     *
     * 실제 지원 여부는 EmbeddingClient 구현체나 모델에 따라 다릅니다.
     */
    private final boolean normalize;

    /**
     * 임베딩 차원 축소 요청값.
     *
     * 0이면 모델 기본 차원을 사용합니다.
     */
    private final int dimensions;

    /**
     * 입력을 자를 수 있는지 여부.
     *
     * true이면 모델의 최대 컨텍스트 길이를 초과할 때
     * 서버 또는 클라이언트가 입력을 자를 수 있습니다.
     */
    private final boolean truncate;

    /**
     * 요청 추적용 식별자.
     */
    private final String requestId;

    /**
     * 임베딩 목적.
     *
     * 예:
     * document
     * query
     */
    private final EmbeddingPurpose purpose;

    /**
     * 추가 모델 옵션.
     *
     * 특정 임베딩 서버 또는 모델의 확장 설정을 전달할 때 사용합니다.
     */
    private final Map<String, Object> options;

    private EmbeddingRequest(Builder builder) {
        this.model = requireText(builder.model, "model");
        this.inputs = immutableInputs(builder.inputs);
        this.normalize = builder.normalize;
        this.dimensions = validateDimensions(builder.dimensions);
        this.truncate = builder.truncate;
        this.requestId = normalizeText(builder.requestId);
        this.purpose = Objects.requireNonNullElse(
            builder.purpose,
            EmbeddingPurpose.DOCUMENT
        );
        this.options = immutableOptions(builder.options);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 단일 입력 요청을 간단히 생성합니다.
     *
     * @param model 임베딩 모델명
     * @param input 임베딩할 텍스트
     * @return 임베딩 요청
     */
    public static EmbeddingRequest of(
        String model,
        String input
    ) {
        return builder()
            .model(model)
            .input(input)
            .build();
    }

    /**
     * 검색 질의 임베딩 요청을 생성합니다.
     *
     * @param model 임베딩 모델명
     * @param query 검색 질의
     * @return 질의 임베딩 요청
     */
    public static EmbeddingRequest forQuery(
        String model,
        String query
    ) {
        return builder()
            .model(model)
            .input(query)
            .purpose(EmbeddingPurpose.QUERY)
            .build();
    }

    /**
     * 문서 임베딩 요청을 생성합니다.
     *
     * @param model 임베딩 모델명
     * @param document 문서 텍스트
     * @return 문서 임베딩 요청
     */
    public static EmbeddingRequest forDocument(
        String model,
        String document
    ) {
        return builder()
            .model(model)
            .input(document)
            .purpose(EmbeddingPurpose.DOCUMENT)
            .build();
    }

    public String getModel() {
        return model;
    }

    public List<String> getInputs() {
        return inputs;
    }

    /**
     * 단일 입력을 반환합니다.
     *
     * 입력이 여러 개인 경우 첫 번째 입력을 반환합니다.
     */
    public String getInput() {
        return inputs.get(0);
    }

    public boolean isNormalize() {
        return normalize;
    }

    public int getDimensions() {
        return dimensions;
    }

    public boolean hasDimensions() {
        return dimensions > 0;
    }

    public boolean isTruncate() {
        return truncate;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean hasRequestId() {
        return !requestId.isBlank();
    }

    public EmbeddingPurpose getPurpose() {
        return purpose;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public Object getOption(String key) {
        if (key == null) {
            return null;
        }

        return options.get(key);
    }

    public boolean hasOption(String key) {
        return key != null && options.containsKey(key);
    }

    public int size() {
        return inputs.size();
    }

    public boolean isBatch() {
        return inputs.size() > 1;
    }

    private static List<String> immutableInputs(
        List<String> inputs
    ) {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException(
                "inputs must not be empty"
            );
        }

        List<String> copy = new ArrayList<>(inputs.size());

        for (int index = 0; index < inputs.size(); index++) {
            String input = normalizeText(inputs.get(index));

            if (input.isBlank()) {
                throw new IllegalArgumentException(
                    "input must not be blank at index " + index
                );
            }

            copy.add(input);
        }

        return Collections.unmodifiableList(copy);
    }

    private static Map<String, Object> immutableOptions(
        Map<String, Object> options
    ) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> copy = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : options.entrySet()) {
            String key = normalizeText(entry.getKey());

            if (!key.isBlank() && entry.getValue() != null) {
                copy.put(key, entry.getValue());
            }
        }

        return Collections.unmodifiableMap(copy);
    }

    private static int validateDimensions(int dimensions) {
        if (dimensions < 0) {
            throw new IllegalArgumentException(
                "dimensions must be greater than or equal to zero"
            );
        }

        return dimensions;
    }

    private static String requireText(
        String value,
        String fieldName
    ) {
        String normalized = normalizeText(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }

        return normalized;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "EmbeddingRequest{" +
            "model='" + model + '\'' +
            ", inputCount=" + inputs.size() +
            ", normalize=" + normalize +
            ", dimensions=" + dimensions +
            ", truncate=" + truncate +
            ", requestId='" + requestId + '\'' +
            ", purpose=" + purpose +
            ", optionCount=" + options.size() +
            '}';
    }

    public static final class Builder {

        private String model;
        private final List<String> inputs = new ArrayList<>();
        private boolean normalize = true;
        private int dimensions;
        private boolean truncate = true;
        private String requestId;
        private EmbeddingPurpose purpose =
            EmbeddingPurpose.DOCUMENT;
        private final Map<String, Object> options =
            new LinkedHashMap<>();

        private Builder() {
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * 기존 입력을 유지하고 단일 입력을 추가합니다.
         */
        public Builder input(String input) {
            this.inputs.add(input);
            return this;
        }

        /**
         * 기존 입력을 제거하고 새 입력 목록으로 교체합니다.
         */
        public Builder inputs(List<String> inputs) {
            this.inputs.clear();

            if (inputs != null) {
                this.inputs.addAll(inputs);
            }

            return this;
        }

        /**
         * 여러 입력을 기존 목록에 추가합니다.
         */
        public Builder addInputs(List<String> inputs) {
            if (inputs != null) {
                this.inputs.addAll(inputs);
            }

            return this;
        }

        public Builder normalize(boolean normalize) {
            this.normalize = normalize;
            return this;
        }

        public Builder dimensions(int dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder truncate(boolean truncate) {
            this.truncate = truncate;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder purpose(EmbeddingPurpose purpose) {
            this.purpose = purpose;
            return this;
        }

        public Builder option(String key, Object value) {
            String normalizedKey = normalizeText(key);

            if (!normalizedKey.isBlank() && value != null) {
                options.put(normalizedKey, value);
            }

            return this;
        }

        public Builder options(Map<String, Object> options) {
            if (options == null) {
                return this;
            }

            for (Map.Entry<String, Object> entry : options.entrySet()) {
                option(entry.getKey(), entry.getValue());
            }

            return this;
        }

        public EmbeddingRequest build() {
            return new EmbeddingRequest(this);
        }
    }
}