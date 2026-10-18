/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.model;

import java.util.Locale;

/**
 * RAG 인덱싱 대상 원본 문서 유형입니다.
 */
public enum DocumentSourceType {

    XHTML("application/xhtml+xml"),

    HTML("text/html"),

    NAV("application/xhtml+xml"),

    OPF("application/oebps-package+xml"),

    NCX("application/x-dtbncx+xml"),

    CSS("text/css"),

    XML("application/xml"),

    TEXT("text/plain"),

    MARKDOWN("text/markdown"),

    JSON("application/json"),

    UNKNOWN("application/octet-stream");

    private final String defaultMediaType;

    DocumentSourceType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    /**
     * 파일명 또는 경로의 확장자로 문서 유형을 추론합니다.
     */
    public static DocumentSourceType fromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return UNKNOWN;
        }

        String normalized = fileName
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace('\\', '/');

        if (normalized.endsWith(".xhtml")) {
            if (isNavFile(normalized)) {
                return NAV;
            }

            return XHTML;
        }

        if (normalized.endsWith(".html")
            || normalized.endsWith(".htm")) {

            return HTML;
        }

        if (normalized.endsWith(".opf")) {
            return OPF;
        }

        if (normalized.endsWith(".ncx")) {
            return NCX;
        }

        if (normalized.endsWith(".css")) {
            return CSS;
        }

        if (normalized.endsWith(".xml")) {
            return XML;
        }

        if (normalized.endsWith(".md")
            || normalized.endsWith(".markdown")) {

            return MARKDOWN;
        }

        if (normalized.endsWith(".json")) {
            return JSON;
        }

        if (normalized.endsWith(".txt")) {
            return TEXT;
        }

        return UNKNOWN;
    }

    /**
     * MIME 타입으로 문서 유형을 추론합니다.
     */
    public static DocumentSourceType fromMediaType(
        String mediaType
    ) {
        if (mediaType == null || mediaType.isBlank()) {
            return UNKNOWN;
        }

        String normalized = mediaType
            .trim()
            .toLowerCase(Locale.ROOT);

        int parameterIndex = normalized.indexOf(';');

        if (parameterIndex >= 0) {
            normalized = normalized.substring(0, parameterIndex).trim();
        }

        switch (normalized) {
            case "application/xhtml+xml":
                return XHTML;

            case "text/html":
                return HTML;

            case "application/oebps-package+xml":
                return OPF;

            case "application/x-dtbncx+xml":
                return NCX;

            case "text/css":
                return CSS;

            case "application/xml":
            case "text/xml":
                return XML;

            case "text/plain":
                return TEXT;

            case "text/markdown":
                return MARKDOWN;

            case "application/json":
                return JSON;

            default:
                return UNKNOWN;
        }
    }

    private static boolean isNavFile(String normalizedPath) {
        int separatorIndex = normalizedPath.lastIndexOf('/');

        String fileName = separatorIndex >= 0
            ? normalizedPath.substring(separatorIndex + 1)
            : normalizedPath;

        return "nav.xhtml".equals(fileName)
            || "toc.xhtml".equals(fileName)
            || fileName.endsWith("_nav.xhtml")
            || fileName.endsWith("-nav.xhtml");
    }
}