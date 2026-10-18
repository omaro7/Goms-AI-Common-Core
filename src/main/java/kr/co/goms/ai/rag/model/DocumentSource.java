/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.model;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * RAG 인덱싱 대상이 되는 원본 문서입니다.
 *
 * <p>실제 파일 시스템 경로와 프로젝트 기준 상대 경로를
 * 분리하여 관리합니다.</p>
 *
 * <pre>
 * 실제 파일 경로:
 * C:/workspace/book/OEBPS/Text/chapter01.xhtml
 *
 * 프로젝트 상대 경로:
 * OEBPS/Text/chapter01.xhtml
 * </pre>
 *
 * <p>RAG의 문서 식별에는 가능한 한 {@code relativePath}를
 * 사용합니다. 프로젝트가 다른 디렉터리로 이동하더라도
 * 동일한 문서 식별 규칙을 유지할 수 있기 때문입니다.</p>
 * 
 * Path filePath = Path.of(
    "C:/workspace/book/OEBPS/Text/chapter01.xhtml"
);

	DocumentSource source = DocumentSource.builder()
    .id("OEBPS/Text/chapter01.xhtml")
    .relativePath("OEBPS/Text/chapter01.xhtml")
    .filePath(filePath)
    .type(DocumentSourceType.XHTML)
    .mediaType("application/xhtml+xml")
    .content(xhtmlContent)
    .charset(StandardCharsets.UTF_8)
    .language("ko")
    .lastModifiedTime(
        Files.getLastModifiedTime(filePath).toMillis()
    )
    .size(Files.size(filePath))
    .metadata("manifestId", "chapter01")
    .metadata("spineIndex", "1")
    .metadata("epubType", "chapter")
    .build();
    
 */


public final class DocumentSource {

    /**
     * 실제 파일 시스템 경로입니다.
     */
    private final Path path;

    /**
     * 프로젝트 루트 기준 상대 경로입니다.
     *
     * <p>예: {@code OEBPS/Text/chapter01.xhtml}</p>
     */
    private final String relativePath;

    /**
     * 문서 유형입니다.
     */
    private final DocumentSourceType type;

    /**
     * 문서 전체 텍스트 내용입니다.
     */
    private final String content;

    /**
     * 문서 문자 인코딩입니다.
     */
    private final Charset charset;

    /**
     * 파일 크기입니다.
     *
     * <p>byte 단위입니다.</p>
     */
    private final long size;

    /**
     * 마지막 수정 시각입니다.
     *
     * <p>Epoch milliseconds 단위입니다.</p>
     */
    private final long lastModifiedAt;

    /**
     * 원본 문서 전체 내용의 해시입니다.
     *
     * <p>선택 값입니다. 비어 있으면 RagIndexer에서
     * HashService를 사용하여 계산할 수 있습니다.</p>
     */
    private final String contentHash;

    /**
     * 문서 표시용 제목입니다.
     *
     * <p>선택 값입니다.</p>
     */
    private final String title;

    /**
     * 문서 언어입니다.
     *
     * <p>예: ko, en, ko-KR</p>
     */
    private final String language;

    /**
     * EPUB 의미 유형입니다.
     *
     * <p>예: chapter, toc, cover, titlepage</p>
     */
    private final String epubType;

    /**
     * 추가 메타데이터입니다.
     */
    private final Map<String, String> metadata;

    private DocumentSource(
        Builder builder
    ) {
        this.path =
            Objects.requireNonNull(
                builder.path,
                "path must not be null"
            )
            .toAbsolutePath()
            .normalize();

        this.relativePath =
            requireRelativePath(
                builder.relativePath
            );

        this.type =
            builder.type == null
                ? DocumentSourceType.UNKNOWN
                : builder.type;

        this.content =
            builder.content == null
                ? ""
                : normalizeContent(
                    builder.content
                );

        this.charset =
            builder.charset == null
                ? StandardCharsets.UTF_8
                : builder.charset;

        this.size =
            validateNonNegative(
                builder.size,
                "size"
            );

        this.lastModifiedAt =
            validateNonNegative(
                builder.lastModifiedAt,
                "lastModifiedAt"
            );

        this.contentHash =
            normalize(
                builder.contentHash
            );

        this.title =
            normalize(
                builder.title
            );

        this.language =
            normalizeLanguage(
                builder.language
            );

        this.epubType =
            normalize(
                builder.epubType
            );

        this.metadata =
            immutableMetadata(
                builder.metadata
            );
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 실제 파일 경로를 반환합니다.
     */
    public Path getPath() {
        return path;
    }

    /**
     * 프로젝트 기준 상대 경로를 반환합니다.
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * RAG 계층에서 사용할 원본 문서 식별 경로입니다.
     *
     * <p>현재는 relativePath와 동일합니다.</p>
     */
    public String getSourcePath() {
        return relativePath;
    }

    public DocumentSourceType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public Charset getCharset() {
        return charset;
    }

    public long getSize() {
        return size;
    }

    public long getLastModifiedAt() {
        return lastModifiedAt;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }

    public String getEpubType() {
        return epubType;
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

        return metadata.get(
            key.trim()
        );
    }

    /**
     * 지정된 메타데이터 값을 반환합니다.
     *
     * <p>키가 없거나 값이 비어 있는 경우
     * 지정된 기본값을 반환합니다.</p>
     *
     * @param key 메타데이터 키
     * @param defaultValue 기본값
     * @return 메타데이터 값 또는 기본값
     */
    public String getMetadataOrDefault(
        String key,
        String defaultValue
    ) {
        if (key == null) {
            return defaultValue;
        }

        String normalizedKey =
            key.trim();

        if (normalizedKey.isBlank()) {
            return defaultValue;
        }

        String value =
            metadata.get(
                normalizedKey
            );

        if (value == null
            || value.isBlank()) {

            return defaultValue;
        }

        return value;
    }
    
    public boolean hasMetadata(
        String key
    ) {
        return key != null
            && metadata.containsKey(
                key.trim()
            );
    }

    public boolean hasContent() {
        return !content.isBlank();
    }

    public boolean hasContentHash() {
        return !contentHash.isBlank();
    }

    public boolean hasTitle() {
        return !title.isBlank();
    }

    public boolean hasLanguage() {
        return !language.isBlank();
    }

    public boolean hasEpubType() {
        return !epubType.isBlank();
    }

    /**
     * 실제 파일명이 존재하는 경우 반환합니다.
     */
    public String getFileName() {
        Path fileName =
            path.getFileName();

        return fileName == null
            ? ""
            : fileName.toString();
    }

    /**
     * 파일 확장자를 반환합니다.
     */
    public String getExtension() {
        String fileName =
            getFileName();

        int index =
            fileName.lastIndexOf('.');

        if (index < 0
            || index == fileName.length() - 1) {

            return "";
        }

        return fileName
            .substring(index + 1)
            .toLowerCase(
                java.util.Locale.ROOT
            );
    }

    /**
     * 현재 문서가 지정된 유형인지 확인합니다.
     */
    public boolean isType(
        DocumentSourceType type
    ) {
        return type != null
            && this.type == type;
    }

    /**
     * 프로젝트 상대 경로가 지정된 경로와 일치하는지 확인합니다.
     */
    public boolean isSourcePath(
        String sourcePath
    ) {
        return normalizePath(
            sourcePath
        ).equals(
            relativePath
        );
    }

    /**
     * Builder 기반으로 현재 객체의 복사본을 생성합니다.
     */
    public Builder toBuilder() {
        return builder()
            .path(path)
            .relativePath(relativePath)
            .type(type)
            .content(content)
            .charset(charset)
            .size(size)
            .lastModifiedAt(lastModifiedAt)
            .contentHash(contentHash)
            .title(title)
            .language(language)
            .epubType(epubType)
            .metadata(metadata);
    }

    private static String requireRelativePath(
        String value
    ) {
        String normalized =
            normalizePath(value);

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                "relativePath must not be blank"
            );
        }

        /*
         * 프로젝트 기준 상대 경로이므로
         * Windows 드라이브나 루트 경로를 허용하지 않습니다.
         */
        Path path;

        try {
            path =
                Path.of(normalized)
                    .normalize();

        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                "Invalid relativePath: "
                    + value,
                exception
            );
        }

        if (path.isAbsolute()) {
            throw new IllegalArgumentException(
                "relativePath must not be absolute: "
                    + value
            );
        }

        String normalizedResult =
            normalizePath(
                path.toString()
            );

        if (normalizedResult.equals("..")
            || normalizedResult.startsWith("../")) {

            throw new IllegalArgumentException(
                "relativePath must not escape project root: "
                    + value
            );
        }

        return normalizedResult;
    }

    private static Map<String, String>
        immutableMetadata(
            Map<String, String> values
        ) {

        if (values == null
            || values.isEmpty()) {

            return Collections.emptyMap();
        }

        Map<String, String> copy =
            new LinkedHashMap<>();

        for (Map.Entry<String, String> entry
            : values.entrySet()) {

            String key =
                normalize(entry.getKey());

            if (key.isBlank()) {
                continue;
            }

            copy.put(
                key,
                normalize(entry.getValue())
            );
        }

        if (copy.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(
            copy
        );
    }

    private static long validateNonNegative(
        long value,
        String fieldName
    ) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                fieldName
                    + " must be greater than or equal to zero"
            );
        }

        return value;
    }

    private static String normalizeContent(
        String value
    ) {
        if (value == null) {
            return "";
        }

        /*
         * XHTML 원문 자체는 보존해야 하므로
         * 불필요한 공백 축약은 하지 않습니다.
         * 개행 문자만 통일합니다.
         */
        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n');
    }

    private static String normalizeLanguage(
        String value
    ) {
        String normalized =
            normalize(value);

        if (normalized.isBlank()) {
            return "";
        }

        return normalized
            .replace('_', '-')
            .toLowerCase(
                java.util.Locale.ROOT
            );
    }

    private static String normalizePath(
        String value
    ) {
        if (value == null) {
            return "";
        }

        String normalized =
            value.trim()
                .replace('\\', '/');

        while (normalized.startsWith("./")) {
            normalized =
                normalized.substring(2);
        }

        while (normalized.contains("//")) {
            normalized =
                normalized.replace("//", "/");
        }

        while (normalized.endsWith("/")
            && normalized.length() > 1) {

            normalized =
                normalized.substring(
                    0,
                    normalized.length() - 1
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

    @Override
    public String toString() {
        return "DocumentSource{" +
            "path=" + path +
            ", relativePath='" + relativePath + '\'' +
            ", type=" + type +
            ", size=" + size +
            ", lastModifiedAt=" + lastModifiedAt +
            ", contentLength=" + content.length() +
            ", contentHash='"
                + contentHash + '\'' +
            ", title='" + title + '\'' +
            ", language='" + language + '\'' +
            ", epubType='" + epubType + '\'' +
            ", metadataCount="
                + metadata.size() +
            '}';
    }

    public static final class Builder {

        private Path path;
        private String relativePath;

        private DocumentSourceType type =
            DocumentSourceType.UNKNOWN;

        private String content;

        private Charset charset =
            StandardCharsets.UTF_8;

        private long size;
        private long lastModifiedAt;

        private String contentHash;
        private String title;
        private String language;
        private String epubType;

        private final Map<String, String> metadata =
            new LinkedHashMap<>();

        private Builder() {
        }

        public Builder path(
            Path path
        ) {
            this.path = path;
            return this;
        }

        public Builder relativePath(
            String relativePath
        ) {
            this.relativePath =
                relativePath;

            return this;
        }

        public Builder relativePath(
            Path relativePath
        ) {
            this.relativePath =
                relativePath == null
                    ? null
                    : relativePath
                        .normalize()
                        .toString()
                        .replace('\\', '/');

            return this;
        }

        public Builder type(
            DocumentSourceType type
        ) {
            this.type = type;
            return this;
        }

        public Builder content(
            String content
        ) {
            this.content = content;
            return this;
        }

        public Builder charset(
            Charset charset
        ) {
            this.charset = charset;
            return this;
        }

        public Builder size(
            long size
        ) {
            this.size = size;
            return this;
        }

        public Builder lastModifiedAt(
            long lastModifiedAt
        ) {
            this.lastModifiedAt =
                lastModifiedAt;

            return this;
        }

        public Builder contentHash(
            String contentHash
        ) {
            this.contentHash =
                contentHash;

            return this;
        }

        public Builder title(
            String title
        ) {
            this.title = title;
            return this;
        }

        public Builder language(
            String language
        ) {
            this.language =
                language;

            return this;
        }

        public Builder epubType(
            String epubType
        ) {
            this.epubType =
                epubType;

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

        public Builder clearMetadata() {
            metadata.clear();
            return this;
        }

        public DocumentSource build() {
            return new DocumentSource(
                this
            );
        }
    }
}