/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * RAG 검색과 임베딩 생성에 사용되는 문서 조각입니다.
 *
 * 하나의 XHTML 파일 전체를 저장하지 않고 제목, 문단, 이미지 설명,
 * 메타데이터 등의 의미 있는 단위로 분리하여 저장합니다.
 */
public final class DocumentChunk {

    /**
     * Chunk 고유 식별자.
     *
     * 예:
     * chapter01.xhtml#p_01
     * chapter01.xhtml#figure_01
     */
    private final String id;

    /**
     * Chunk가 추출된 원본 파일 경로.
     *
     * 예:
     * OEBPS/Text/chapter01.xhtml
     */
    private final String sourcePath;

    /**
     * 문서 또는 장 제목.
     */
    private final String title;

    /**
     * Chunk 유형.
     */
    private final DocumentChunkType type;

    /**
     * 검색과 임베딩에 사용되는 실제 텍스트.
     */
    private final String content;

    /**
     * 원본 문서 내 순서.
     */
    private final int sequence;

    /**
     * 문서 내 요소 식별자.
     *
     * 예:
     * p_01
     * chapter-title
     * figure_01
     */
    private final String elementId;

    /**
     * EPUB 의미 유형.
     *
     * 예:
     * chapter
     * titlepage
     * toc
     * bibliography
     */
    private final String epubType;

    /**
     * 언어 코드.
     *
     * 예:
     * ko
     * en
     */
    private final String language;

    /**
     * 추가 검색 필터에 사용하는 메타데이터.
     */
    private final Map<String, String> metadata;

    private DocumentChunk(Builder builder) {
        this.id = requireText(builder.id, "id");
        this.sourcePath = requireText(builder.sourcePath, "sourcePath");
        this.title = normalize(builder.title);
        this.type = Objects.requireNonNull(builder.type, "type must not be null");
        this.content = requireText(builder.content, "content");
        this.sequence = validateSequence(builder.sequence);
        this.elementId = normalize(builder.elementId);
        this.epubType = normalize(builder.epubType);
        this.language = normalize(builder.language);
        this.metadata = immutableMetadata(builder.metadata);
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public String getId() {
        return id;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getTitle() {
        return title;
    }

    public DocumentChunkType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getSequence() {
        return sequence;
    }

    public String getElementId() {
        return elementId;
    }

    public String getEpubType() {
        return epubType;
    }

    public String getLanguage() {
        return language;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getMetadata(String key) {
        if (key == null) {
            return null;
        }

        return metadata.get(key);
    }

    /**
     * 임베딩 모델에 전달할 텍스트를 생성합니다.
     *
     * 제목과 유형, 본문을 함께 전달하여 단순 본문만 사용하는 것보다
     * 검색 정확도를 높입니다.
     */
    public String toEmbeddingText() {
        StringBuilder text = new StringBuilder();

        if (!title.isBlank()) {
            text.append("제목: ")
                .append(title)
                .append('\n');
        }

        text.append("문서 유형: ")
            .append(type.name())
            .append('\n');

        if (!epubType.isBlank()) {
            text.append("EPUB 유형: ")
                .append(epubType)
                .append('\n');
        }

        text.append("내용: ")
            .append(content);

        return text.toString();
    }

    public boolean hasMetadata(String key) {
        return key != null && metadata.containsKey(key);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalize(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }

        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static int validateSequence(int sequence) {
        if (sequence < 0) {
            throw new IllegalArgumentException(
                "sequence must be greater than or equal to zero"
            );
        }

        return sequence;
    }

    private static Map<String, String> immutableMetadata(
        Map<String, String> metadata
    ) {
        if (metadata == null || metadata.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> copy = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = normalize(entry.getKey());
            String value = normalize(entry.getValue());

            if (!key.isBlank()) {
                copy.put(key, value);
            }
        }

        return Collections.unmodifiableMap(copy);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof DocumentChunk)) {
            return false;
        }

        DocumentChunk other = (DocumentChunk) object;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "DocumentChunk{" +
            "id='" + id + '\'' +
            ", sourcePath='" + sourcePath + '\'' +
            ", title='" + title + '\'' +
            ", type=" + type +
            ", sequence=" + sequence +
            ", elementId='" + elementId + '\'' +
            ", epubType='" + epubType + '\'' +
            ", language='" + language + '\'' +
            '}';
    }

    public static final class Builder {

        private String id;
        private String sourcePath;
        private String title;
        private DocumentChunkType type;
        private String content;
        private int sequence;
        private String elementId;
        private String epubType;
        private String language;
        private final Map<String, String> metadata =
            new LinkedHashMap<>();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder sourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder type(DocumentChunkType type) {
            this.type = type;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder sequence(int sequence) {
            this.sequence = sequence;
            return this;
        }

        public Builder elementId(String elementId) {
            this.elementId = elementId;
            return this;
        }

        public Builder epubType(String epubType) {
            this.epubType = epubType;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder metadata(String key, String value) {
            String normalizedKey = normalize(key);

            if (!normalizedKey.isBlank()) {
                metadata.put(normalizedKey, normalize(value));
            }

            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            if (metadata != null) {
                for (Map.Entry<String, String> entry
                    : metadata.entrySet()) {

                    metadata(entry.getKey(), entry.getValue());
                }
            }

            return this;
        }

        public DocumentChunk build() {
            return new DocumentChunk(this);
        }
    }
}