/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.document;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Locale;
import java.util.Objects;

import kr.co.goms.ai.rag.model.DocumentSource;
import kr.co.goms.ai.rag.model.DocumentSourceType;

/**
 * 파일 시스템 기반 {@link DocumentLoader} 기본 구현체입니다.
 *
 * <p>실제 파일의 절대 경로와 RAG에서 사용하는 프로젝트 상대 경로를
 * 분리하여 {@link DocumentSource}를 생성합니다.</p>
 *
 * <pre>
 * 실제 파일:
 * C:/workspace/book/OEBPS/Text/chapter01.xhtml
 *
 * DocumentSource.relativePath:
 * OEBPS/Text/chapter01.xhtml
 * </pre>
 */

public final class DefaultDocumentLoader
    implements DocumentLoader {

    /**
     * 기본 문자 인코딩입니다.
     */
    public static final Charset DEFAULT_CHARSET =
        StandardCharsets.UTF_8;

    private final Charset defaultCharset;

    /**
     * UTF-8을 기본 인코딩으로 사용합니다.
     */
    public DefaultDocumentLoader() {
        this(DEFAULT_CHARSET);
    }

    /**
     * 기본 인코딩을 지정합니다.
     *
     * @param defaultCharset 기본 문자 인코딩
     */
    public DefaultDocumentLoader(
        Charset defaultCharset
    ) {
        this.defaultCharset =
            Objects.requireNonNullElse(
                defaultCharset,
                DEFAULT_CHARSET
            );
    }

    /**
     * 단독 파일을 로드합니다.
     *
     * <p>프로젝트 루트 정보가 없는 경우 실제 파일명 또는
     * 전달된 상대 경로를 logical sourcePath로 사용합니다.</p>
     */
    @Override
    public DocumentSource load(
        Path path
    ) throws DocumentLoadException {

        requirePath(
            path,
            "path"
        );

        Path actualPath =
            path
                .toAbsolutePath()
                .normalize();

        Path sourcePath =
            path.isAbsolute()
                ? actualPath.getFileName()
                : path.normalize();

        return loadInternal(
            actualPath,
            sourcePath
        );
    }

    /**
     * 프로젝트 루트와 상대 경로를 사용하여 문서를 로드합니다.
     *
     * <p>실제 파일은 {@code projectRoot.resolve(relativePath)}에서
     * 읽고, {@link DocumentSource#getRelativePath()}에는
     * {@code relativePath}를 그대로 보존합니다.</p>
     */
    @Override
    public DocumentSource load(
        Path projectRoot,
        Path relativePath
    ) throws DocumentLoadException {

        requirePath(
            projectRoot,
            "projectRoot"
        );

        requirePath(
            relativePath,
            "relativePath"
        );

        Path normalizedRoot =
            projectRoot
                .toAbsolutePath()
                .normalize();

        Path normalizedRelativePath =
            relativePath.normalize();

        if (normalizedRelativePath.isAbsolute()) {
            throw new IllegalArgumentException(
                "relativePath must be relative: "
                    + relativePath
            );
        }

        Path actualPath =
            normalizedRoot
                .resolve(
                    normalizedRelativePath
                )
                .normalize();

        /*
         * ../ 등을 사용하여 프로젝트 루트 밖으로 이동하는 것을
         * 방지합니다.
         */
        if (!actualPath.startsWith(
            normalizedRoot
        )) {
            throw new IllegalArgumentException(
                "relativePath escapes project root: "
                    + relativePath
            );
        }

        return loadInternal(
            actualPath,
            normalizedRelativePath
        );
    }

    /**
     * 실제 파일을 읽어 DocumentSource를 생성하는 공통 구현부입니다.
     *
     * @param actualPath 실제 파일 시스템 경로
     * @param sourcePath RAG에서 사용할 논리적 상대 경로
     */
    private DocumentSource loadInternal(
        Path actualPath,
        Path sourcePath
    ) throws DocumentLoadException {

        requirePath(
            actualPath,
            "actualPath"
        );

        requirePath(
            sourcePath,
            "sourcePath"
        );

        Path normalizedActualPath =
            actualPath
                .toAbsolutePath()
                .normalize();

        Path normalizedSourcePath =
            sourcePath.normalize();

        validateFile(
            normalizedActualPath,
            normalizedSourcePath
        );

        if (!supports(
            normalizedActualPath
        )) {
            throw new DocumentLoadException(
                "Unsupported document type: "
                    + normalizePath(
                        normalizedActualPath
                    ),
                normalizePath(
                    normalizedSourcePath
                ),
                null
            );
        }

        Charset charset =
            resolveCharset(
                normalizedActualPath
            );

        try {
            String content =
                Files.readString(
                    normalizedActualPath,
                    charset
                );

            long size =
                Files.size(
                    normalizedActualPath
                );

            FileTime lastModifiedTime =
                Files.getLastModifiedTime(
                    normalizedActualPath,
                    LinkOption.NOFOLLOW_LINKS
                );

            return createDocumentSource(
                normalizedActualPath,
                normalizedSourcePath,
                content,
                charset,
                size,
                lastModifiedTime.toMillis()
            );

        } catch (IOException exception) {
            throw new DocumentLoadException(
                "Failed to read document: "
                    + normalizePath(
                        normalizedActualPath
                    ),
                normalizePath(
                    normalizedSourcePath
                ),
                exception
            );
        }
    }

    /**
     * DocumentSource를 생성합니다.
     *
     * <p>DocumentSource의 Builder 구조가 변경될 경우
     * 이 메서드만 수정하면 됩니다.</p>
     */
    private DocumentSource createDocumentSource(
        Path actualPath,
        Path sourcePath,
        String content,
        Charset charset,
        long size,
        long lastModifiedAt
    ) {
        return DocumentSource.builder()
            .path(actualPath)
            .relativePath(
                normalizePath(
                    sourcePath
                )
            )
            .type(
                resolveType(
                    actualPath
                )
            )
            .content(content)
            .charset(charset)
            .size(size)
            .lastModifiedAt(
                lastModifiedAt
            )
            .build();
    }

    /**
     * 현재 Loader가 지원하는 파일인지 확인합니다.
     */
    @Override
    public boolean supports(
        Path path
    ) {
        if (path == null
            || path.getFileName() == null) {

            return false;
        }

        String extension =
            extensionOf(path);

        return "xhtml".equals(extension)
            || "html".equals(extension)
            || "htm".equals(extension)
            || "xml".equals(extension)
            || "txt".equals(extension)
            || "md".equals(extension);
    }

    /**
     * 확장자를 DocumentSourceType으로 변환합니다.
     */
    private DocumentSourceType resolveType(
        Path path
    ) {
        String extension =
            extensionOf(path);

        switch (extension) {

            case "xhtml":
                return DocumentSourceType.XHTML;

            case "html":
            case "htm":
                return DocumentSourceType.HTML;

            case "xml":
                return DocumentSourceType.XML;

            case "txt":
                return DocumentSourceType.TEXT;

            case "md":
                return DocumentSourceType.MARKDOWN;

            default:
                return DocumentSourceType.UNKNOWN;
        }
    }

    /**
     * 현재는 모든 텍스트 문서를 기본 인코딩으로 읽습니다.
     *
     * <p>향후 XML 선언이나 BOM을 읽어서 charset을 판단하려면
     * 이 메서드를 확장하면 됩니다.</p>
     */
    private Charset resolveCharset(
        Path path
    ) {
        return defaultCharset;
    }

    /**
     * 파일 존재 여부와 읽기 가능 여부를 검증합니다.
     */
    private void validateFile(
        Path actualPath,
        Path sourcePath
    ) throws DocumentLoadException {

        if (!Files.exists(
            actualPath,
            LinkOption.NOFOLLOW_LINKS
        )) {
            throw new DocumentLoadException(
                "Document does not exist: "
                    + normalizePath(
                        actualPath
                    ),
                normalizePath(
                    sourcePath
                ),
                null
            );
        }

        if (!Files.isRegularFile(
            actualPath,
            LinkOption.NOFOLLOW_LINKS
        )) {
            throw new DocumentLoadException(
                "Document is not a regular file: "
                    + normalizePath(
                        actualPath
                    ),
                normalizePath(
                    sourcePath
                ),
                null
            );
        }

        if (!Files.isReadable(
            actualPath
        )) {
            throw new DocumentLoadException(
                "Document is not readable: "
                    + normalizePath(
                        actualPath
                    ),
                normalizePath(
                    sourcePath
                ),
                null
            );
        }
    }

    private static void requirePath(
        Path value,
        String fieldName
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                fieldName
                    + " must not be null"
            );
        }
    }

    private static String extensionOf(
        Path path
    ) {
        if (path == null
            || path.getFileName() == null) {

            return "";
        }

        String fileName =
            path.getFileName()
                .toString();

        int index =
            fileName.lastIndexOf('.');

        if (index < 0
            || index
                == fileName.length() - 1) {

            return "";
        }

        return fileName
            .substring(index + 1)
            .toLowerCase(
                Locale.ROOT
            );
    }

    private static String normalizePath(
        Path path
    ) {
        if (path == null) {
            return "";
        }

        return path
            .normalize()
            .toString()
            .replace('\\', '/');
    }

    public Charset getDefaultCharset() {
        return defaultCharset;
    }

    @Override
    public String toString() {
        return "DefaultDocumentLoader{" +
            "defaultCharset="
                + defaultCharset +
            '}';
    }
}