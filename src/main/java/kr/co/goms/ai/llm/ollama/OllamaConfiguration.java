/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Ollama 연결 및 실행 설정입니다.
 * 
 * OllamaConfiguration configuration = OllamaConfiguration.builder()
        .baseUrl("http://localhost:11434")
        .chatModel("gemma4:31b-cloud")
        .embeddingModel("nomic-embed-text")
        .build();
 */
public final class OllamaConfiguration {

    public static final String DEFAULT_BASE_URL = "http://localhost:11434";

    public static final String DEFAULT_VERSION_ENDPOINT = "/api/version";

    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofMinutes(5);

    private final String baseUrl;
    private final String model;

    private final Duration connectTimeout;
    private final Duration requestTimeout;

    private final boolean enabled;
    private final String versionEndpoint;
    
    private final String chatModel;
    private final String embeddingModel;

    private OllamaConfiguration(Builder builder) {

        this.baseUrl = normalizeBaseUrl(builder.baseUrl);

        this.model = normalizeOptional(builder.model);
        
        this.chatModel = requireText(
                builder.chatModel,
                "chatModel"
            );

        this.embeddingModel = requireText(
                builder.embeddingModel,
                "embeddingModel"
            );
		
        this.connectTimeout =
                validateDuration(
                        builder.connectTimeout,
                        "connectTimeout"
                );

        this.requestTimeout =
                validateDuration(
                        builder.requestTimeout,
                        "requestTimeout"
                );

        this.enabled = builder.enabled;

        this.versionEndpoint =
                normalizeEndpoint(
                        builder.versionEndpoint,
                        DEFAULT_VERSION_ENDPOINT
                );
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기본 설정을 생성합니다.
     */
    public static OllamaConfiguration defaults(
            String model) {

        return builder()
                .model(model)
                .build();
    }

    // =========================================================
    // Getter 스타일 API
    // =========================================================

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getVersionEndpoint() {
        return versionEndpoint;
    }
    
    public String getChatModel() {
        return chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    // =========================================================
    // 기존 record 스타일 호환 API
    // =========================================================

    /**
     * 기존 코드 호환용입니다.
     */
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * 기존 코드 호환용입니다.
     */
    public String model() {
        return model;
    }

    /**
     * 기존 코드 호환용입니다.
     */
    public Duration connectTimeout() {
        return connectTimeout;
    }

    /**
     * 기존 코드 호환용입니다.
     */
    public Duration requestTimeout() {
        return requestTimeout;
    }

    /**
     * 기존 코드 호환용입니다.
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * 기존 코드 호환용입니다.
     */
    public URI versionEndpoint() {
        return URI.create(
                baseUrl + versionEndpoint
        );
    }

    public String getVersionEndpointPath() {
        return versionEndpoint;
    }
    
    // =========================================================
    // URL
    // =========================================================

    public String getChatUrl() {
        return baseUrl + "/api/chat";
    }

    public String getTagsUrl() {
        return baseUrl + "/api/tags";
    }

    public String getVersionUrl() {
        return baseUrl + versionEndpoint;
    }

    public String chatEndpoint() {
        return "/api/chat";
    }

    public String tagsEndpoint() {
        return "/api/tags";
    }

    // =========================================================
    // 상태
    // =========================================================

    public boolean hasModel() {
        return model != null
                && !model.isBlank();
    }

    /**
     * 현재 설정이 Local Ollama 서버를 가리키는지 확인합니다.
     */
    public boolean isLocalServer() {
        try {
            URI uri = URI.create(baseUrl);

            String host = uri.getHost();

            if (host == null) {
                return false;
            }

            return "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host);

        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * 기존 설정을 기반으로 Builder를 반환합니다.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    // =========================================================
    // Validation
    // =========================================================

    private static String normalizeBaseUrl(
            String value) {

        String normalized =
                value == null || value.isBlank()
                        ? DEFAULT_BASE_URL
                        : value.trim();

        while (normalized.endsWith("/")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        if (!normalized.startsWith("http://")
                && !normalized.startsWith("https://")) {

            throw new IllegalArgumentException(
                    "Ollama baseUrl must start with "
                            + "http:// or https:// : "
                            + normalized
            );
        }

        return normalized;
    }

    private static String normalizeEndpoint(
            String value,
            String defaultValue) {

        String normalized =
                value == null || value.isBlank()
                        ? defaultValue
                        : value.trim();

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return normalized;
    }

    private static String normalizeOptional(
            String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static Duration validateDuration(
            Duration value,
            String fieldName) {

        Objects.requireNonNull(
                value,
                fieldName + " must not be null"
        );

        if (value.isZero()
                || value.isNegative()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " must be greater than zero"
            );
        }

        return value;
    }
    
    private static String requireText(
            String value,
            String fieldName
        ) {
            String normalized =
                Objects.requireNonNullElse(value, "").trim();

            if (normalized.isBlank()) {
                throw new IllegalArgumentException(
                    fieldName + " must not be blank"
                );
            }

            return normalized;
        }

    @Override
    public String toString() {
        return "OllamaConfiguration{"
                + "baseUrl='" + baseUrl + '\''
                + ", model='" + model + '\''
                + ", connectTimeout=" + connectTimeout
                + ", requestTimeout=" + requestTimeout
                + ", enabled=" + enabled
                + ", versionEndpoint='"
                + versionEndpoint + '\''
                + ", localServer="
                + isLocalServer()
                + '}';
    }

    // =========================================================
    // Builder
    // =========================================================

    public static final class Builder {

        private String baseUrl =
                DEFAULT_BASE_URL;

        private String model;

        private String chatModel = "gemma4:31b-cloud";
        
        private String embeddingModel = "nomic-embed-text";
        
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;

        private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;

        private boolean enabled = true;

        private String versionEndpoint = DEFAULT_VERSION_ENDPOINT;

        private Builder() {
        }

        private Builder(
                OllamaConfiguration source) {

            this.baseUrl = source.baseUrl;

            this.model = source.model;

            this.connectTimeout = source.connectTimeout;

            this.requestTimeout =  source.requestTimeout;

            this.enabled = source.enabled;

            this.versionEndpoint = source.versionEndpoint;
            
            this.chatModel = source.chatModel;
            
            this.embeddingModel = source.embeddingModel;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder chatModel(String chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder embeddingModel(
            String embeddingModel
        ) {
            this.embeddingModel = embeddingModel;
            return this;
        }

        public Builder connectTimeout(
                Duration connectTimeout) {

            this.connectTimeout =
                    connectTimeout;

            return this;
        }

        public Builder requestTimeout(
                Duration requestTimeout) {

            this.requestTimeout =
                    requestTimeout;

            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder versionEndpoint(
                String versionEndpoint) {

            this.versionEndpoint =
                    versionEndpoint;

            return this;
        }

        public OllamaConfiguration build() {
            return new OllamaConfiguration(this);
        }
    }
}