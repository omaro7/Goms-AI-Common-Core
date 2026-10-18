/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

/**
 * 원본 문서를 DocumentChunk 목록으로 변환하는 과정에서 발생한 예외입니다.
 */
public class DocumentIndexException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String sourcePath;

    public DocumentIndexException(String message) {
        this(message, null, null);
    }

    public DocumentIndexException(
        String message,
        Throwable cause
    ) {
        this(message, null, cause);
    }

    public DocumentIndexException(
        String message,
        String sourcePath
    ) {
        this(message, sourcePath, null);
    }

    public DocumentIndexException(
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