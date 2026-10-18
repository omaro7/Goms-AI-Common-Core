/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kr.co.goms.ai.rag.model.DocumentChunk;


/**
 * RAG 검색 결과를 LLM 프롬프트에 전달하기 위한 최종 컨텍스트입니다.
 *
 * <p>컨텍스트 길이 제한과 출처 선택은 {@link RagContextBuilder}가
 * 담당하며, 이 클래스는 생성된 결과를 불변 상태로 보관합니다.</p>
 */
public final class RagContext {

    private final String query;
    private final String contextText;
    private final List<RagSource> sources;
    private final String embeddingModel;
    private final long retrievalDurationNanos;
    private final long createdAt;
    private final boolean truncated;
    private final int originalCharacterCount;
    private final int characterCount;
    private final Map<String, String> metadata;

    private RagContext(Builder builder) {
        this.query = requireText(
            builder.query,
            "query"
        );

        this.contextText =
            normalizeMultiline(
                builder.contextText
            );

        this.sources =
            immutableSources(
                builder.sources
            );

        this.embeddingModel =
            normalize(
                builder.embeddingModel
            );

        this.retrievalDurationNanos =
            validateNonNegative(
                builder.retrievalDurationNanos,
                "retrievalDurationNanos"
            );

        this.createdAt =
            builder.createdAt <= 0
                ? System.currentTimeMillis()
                : builder.createdAt;

        this.truncated =
            builder.truncated;

        this.characterCount =
            this.contextText.length();

        this.originalCharacterCount =
            resolveOriginalCharacterCount(
                builder.originalCharacterCount,
                this.characterCount
            );

        this.metadata =
            immutableMetadata(
                builder.metadata
            );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RagContext empty(
        String query,
        String embeddingModel
    ) {
        return builder()
            .query(query)
            .embeddingModel(
                embeddingModel
            )
            .contextText("")
            .sources(List.of())
            .truncated(false)
            .originalCharacterCount(0)
            .createdAt(
                System.currentTimeMillis()
            )
            .build();
    }

    public String getQuery() {
        return query;
    }

    public String getContextText() {
        return contextText;
    }

    public List<RagSource> getSources() {
        return sources;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public long getRetrievalDurationNanos() {
        return retrievalDurationNanos;
    }

    public double getRetrievalDurationMillis() {
        return retrievalDurationNanos
            / 1_000_000.0;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public int getOriginalCharacterCount() {
        return originalCharacterCount;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public int getOmittedCharacterCount() {
        return Math.max(
            0,
            originalCharacterCount
                - characterCount
        );
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getMetadata(
        String key
    ) {
        if (key == null) {
            return null;
        }

        return metadata.get(key);
    }

    public boolean hasMetadata(
        String key
    ) {
        return key != null
            && metadata.containsKey(key);
    }

    public int size() {
        return sources.size();
    }

    public boolean isEmpty() {
        return sources.isEmpty()
            || contextText.isBlank();
    }

    public boolean hasContext() {
        return !contextText.isBlank();
    }

    public List<DocumentChunk> getChunks() {
        if (sources.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> chunks =
            new ArrayList<>(
                sources.size()
            );

        for (RagSource source : sources) {
            chunks.add(
                source.getChunk()
            );
        }

        return List.copyOf(chunks);
    }

    public double getHighestScore() {
        if (sources.isEmpty()) {
            return 0.0;
        }

        double highest =
            -Double.MAX_VALUE;

        for (RagSource source : sources) {
            highest = Math.max(
                highest,
                source.getScore()
            );
        }

        return highest
            == -Double.MAX_VALUE
                ? 0.0
                : highest;
    }

    public String toPromptBlock() {
        if (isEmpty()) {
            return """
                [참고 문서]
                검색된 관련 프로젝트 문서가 없습니다.
                """.trim();
        }

        StringBuilder builder =
            new StringBuilder();

        builder.append(
            """
            [참고 문서]
            아래 내용은 현재 프로젝트에서 검색된 실제 문서입니다.
            답변은 참고 문서의 내용을 우선하여 작성하십시오.
            참고 문서에 없는 프로젝트 정보는 임의로 생성하지 마십시오.

            """
        );

        builder.append(contextText);

        return builder
            .toString()
            .trim();
    }

    private static List<RagSource> immutableSources(
        List<RagSource> sources
    ) {
        if (sources == null
            || sources.isEmpty()) {

            return Collections.emptyList();
        }

        List<RagSource> copy =
            new ArrayList<>(
                sources.size()
            );

        for (RagSource source : sources) {
            if (source != null) {
                copy.add(source);
            }
        }

        return Collections.unmodifiableList(
            copy
        );
    }

    private static Map<String, String> immutableMetadata(
        Map<String, String> metadata
    ) {
        if (metadata == null
            || metadata.isEmpty()) {

            return Collections.emptyMap();
        }

        Map<String, String> copy =
            new LinkedHashMap<>();

        for (Map.Entry<String, String> entry
            : metadata.entrySet()) {

            String key =
                normalize(entry.getKey());

            String value =
                normalize(entry.getValue());

            if (!key.isBlank()) {
                copy.put(key, value);
            }
        }

        return Collections.unmodifiableMap(
            copy
        );
    }

    private static int resolveOriginalCharacterCount(
        int originalCharacterCount,
        int characterCount
    ) {
        if (originalCharacterCount < 0) {
            throw new IllegalArgumentException(
                "originalCharacterCount must be greater than "
                    + "or equal to zero"
            );
        }

        if (originalCharacterCount == 0) {
            return characterCount;
        }

        if (originalCharacterCount
            < characterCount) {

            throw new IllegalArgumentException(
                "originalCharacterCount must be greater than "
                    + "or equal to characterCount"
            );
        }

        return originalCharacterCount;
    }

    private static long validateNonNegative(
        long value,
        String fieldName
    ) {
        if (value < 0) {
            throw new IllegalArgumentException(
                fieldName
                    + " must be greater than or equal to zero"
            );
        }

        return value;
    }

    private static String requireText(
        String value,
        String fieldName
    ) {
        String normalized =
            normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName
                    + " must not be blank"
            );
        }

        return normalized;
    }

    private static String normalize(
        String value
    ) {
        return value == null
            ? ""
            : value.trim();
    }

    private static String normalizeMultiline(
        String value
    ) {
        if (value == null) {
            return "";
        }

        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replaceAll("[\\t ]+", " ")
            .replaceAll("\\n[\\t ]+", "\n")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    @Override
    public String toString() {
        return "RagContext{" +
            "query='" + query + '\'' +
            ", sourceCount=" + sources.size() +
            ", embeddingModel='" + embeddingModel + '\'' +
            ", retrievalDurationNanos="
                + retrievalDurationNanos +
            ", createdAt=" + createdAt +
            ", truncated=" + truncated +
            ", originalCharacterCount="
                + originalCharacterCount +
            ", characterCount="
                + characterCount +
            '}';
    }

    public static final class Builder {

        private String query;
        private String contextText;

        private final List<RagSource> sources =
            new ArrayList<>();

        private String embeddingModel;
        private long retrievalDurationNanos;
        private long createdAt;
        private boolean truncated;
        private int originalCharacterCount;

        private final Map<String, String> metadata =
            new LinkedHashMap<>();

        private Builder() {
        }

        public Builder query(
            String query
        ) {
            this.query = query;
            return this;
        }

        public Builder contextText(
            String contextText
        ) {
            this.contextText =
                contextText;

            return this;
        }

        public Builder source(
            RagSource source
        ) {
            if (source != null) {
                sources.add(source);
            }

            return this;
        }

        public Builder sources(
            List<RagSource> sources
        ) {
            this.sources.clear();

            if (sources != null) {
                for (RagSource source
                    : sources) {

                    source(source);
                }
            }

            return this;
        }

        public Builder embeddingModel(
            String embeddingModel
        ) {
            this.embeddingModel =
                embeddingModel;

            return this;
        }

        public Builder retrievalDurationNanos(
            long retrievalDurationNanos
        ) {
            this.retrievalDurationNanos =
                retrievalDurationNanos;

            return this;
        }

        public Builder createdAt(
            long createdAt
        ) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder truncated(
            boolean truncated
        ) {
            this.truncated = truncated;
            return this;
        }

        public Builder originalCharacterCount(
            int originalCharacterCount
        ) {
            this.originalCharacterCount =
                originalCharacterCount;

            return this;
        }

        public Builder metadata(
            String key,
            String value
        ) {
            String normalizedKey =
                normalize(key);

            if (!normalizedKey.isBlank()) {
                metadata.put(
                    normalizedKey,
                    normalize(value)
                );
            }

            return this;
        }

        public Builder metadata(
            Map<String, String> metadata
        ) {
            if (metadata == null) {
                return this;
            }

            for (Map.Entry<String, String> entry
                : metadata.entrySet()) {

                metadata(
                    entry.getKey(),
                    entry.getValue()
                );
            }

            return this;
        }

        public RagContext build() {
            return new RagContext(this);
        }
    }
}