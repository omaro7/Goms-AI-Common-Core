/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.document;

/**
 * 원본 문서를 DocumentSource로 변환하는 과정에서 발생한 예외입니다.
 */
public class DocumentLoadException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String sourcePath;

    public DocumentLoadException(String message) {
        this(message, null, null);
    }

    public DocumentLoadException(
        String message,
        Throwable cause
    ) {
        this(message, null, cause);
    }

    public DocumentLoadException(
        String message,
        String sourcePath
    ) {
        this(message, sourcePath, null);
    }

    public DocumentLoadException(
        String message,
        String sourcePath,
        Throwable cause
    ) {
        super(message, cause);
        this.sourcePath = sourcePath == null
            ? ""
            : sourcePath.trim();
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public boolean hasSourcePath() {
        return !sourcePath.isBlank();
    }
}