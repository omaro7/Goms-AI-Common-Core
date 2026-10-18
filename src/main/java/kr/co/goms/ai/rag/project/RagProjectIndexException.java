/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.project;

/**
 * EPUB 프로젝트 탐색, 문서 인덱싱, 동기화 또는
 * 기존 인덱스 정리 과정에서 발생하는 예외입니다.
 *
 * <p>프로젝트 단위 작업에서 실패한 단계와 프로젝트 경로,
 * 대상 문서 경로를 함께 보관합니다.</p>
 */
public class RagProjectIndexException
    extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * 예외가 발생한 프로젝트 인덱싱 작업입니다.
     */
    private final RagProjectIndexOperation operation;

    /**
     * EPUB 프로젝트 루트 경로입니다.
     */
    private final String projectRoot;

    /**
     * 프로젝트 기준 대상 문서 경로입니다.
     *
     * <p>프로젝트 전체 작업에서 특정 문서와 관계없는 경우
     * 빈 문자열입니다.</p>
     */
    private final String sourcePath;

    /**
     * 기본 예외를 생성합니다.
     *
     * @param message 예외 메시지
     */
    public RagProjectIndexException(
        String message
    ) {
        this(
            message,
            RagProjectIndexOperation.UNKNOWN,
            "",
            "",
            null
        );
    }

    /**
     * 원인 예외를 포함한 기본 예외를 생성합니다.
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public RagProjectIndexException(
        String message,
        Throwable cause
    ) {
        this(
            message,
            RagProjectIndexOperation.UNKNOWN,
            "",
            "",
            cause
        );
    }

    /**
     * 작업 유형을 포함한 예외를 생성합니다.
     *
     * @param message 예외 메시지
     * @param operation 실패한 작업
     */
    public RagProjectIndexException(
        String message,
        RagProjectIndexOperation operation
    ) {
        this(
            message,
            operation,
            "",
            "",
            null
        );
    }

    /**
     * 프로젝트 경로를 포함한 예외를 생성합니다.
     *
     * @param message 예외 메시지
     * @param operation 실패한 작업
     * @param projectRoot 프로젝트 루트
     */
    public RagProjectIndexException(
        String message,
        RagProjectIndexOperation operation,
        String projectRoot
    ) {
        this(
            message,
            operation,
            projectRoot,
            "",
            null
        );
    }

    /**
     * 프로젝트와 문서 경로를 포함한 예외를 생성합니다.
     *
     * @param message 예외 메시지
     * @param operation 실패한 작업
     * @param projectRoot 프로젝트 루트
     * @param sourcePath 대상 문서 경로
     */
    public RagProjectIndexException(
        String message,
        RagProjectIndexOperation operation,
        String projectRoot,
        String sourcePath
    ) {
        this(
            message,
            operation,
            projectRoot,
            sourcePath,
            null
        );
    }

    /**
     * 프로젝트 인덱싱 예외를 생성합니다.
     *
     * @param message 예외 메시지
     * @param operation 실패한 작업
     * @param projectRoot 프로젝트 루트
     * @param sourcePath 대상 문서 경로
     * @param cause 원인 예외
     */
    public RagProjectIndexException(
        String message,
        RagProjectIndexOperation operation,
        String projectRoot,
        String sourcePath,
        Throwable cause
    ) {
        super(
            normalizeMessage(message),
            cause
        );

        this.operation =
            operation == null
                ? RagProjectIndexOperation.UNKNOWN
                : operation;

        this.projectRoot =
            normalizePath(projectRoot);

        this.sourcePath =
            normalizePath(sourcePath);
    }

    /**
     * 예외가 발생한 작업을 반환합니다.
     *
     * @return 프로젝트 인덱싱 작업
     */
    public RagProjectIndexOperation getOperation() {
        return operation;
    }

    /**
     * 프로젝트 루트 경로를 반환합니다.
     *
     * @return 정규화된 프로젝트 경로
     */
    public String getProjectRoot() {
        return projectRoot;
    }

    /**
     * 프로젝트 경로가 설정되어 있는지 확인합니다.
     *
     * @return 프로젝트 경로 존재 여부
     */
    public boolean hasProjectRoot() {
        return !projectRoot.isBlank();
    }

    /**
     * 대상 문서 경로를 반환합니다.
     *
     * @return 프로젝트 기준 문서 경로
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * 대상 문서 경로가 설정되어 있는지 확인합니다.
     *
     * @return 문서 경로 존재 여부
     */
    public boolean hasSourcePath() {
        return !sourcePath.isBlank();
    }

    /**
     * 원인 예외가 존재하는지 확인합니다.
     *
     * @return 원인 예외 존재 여부
     */
    public boolean hasCause() {
        return getCause() != null;
    }

    /**
     * 예외 위치를 사람이 읽기 쉬운 문자열로 반환합니다.
     *
     * @return 프로젝트와 문서 경로 정보
     */
    public String getLocationDescription() {
        if (hasProjectRoot()
            && hasSourcePath()) {

            return projectRoot
                + " :: "
                + sourcePath;
        }

        if (hasProjectRoot()) {
            return projectRoot;
        }

        if (hasSourcePath()) {
            return sourcePath;
        }

        return "";
    }

    /**
     * 작업과 경로를 포함한 상세 메시지를 반환합니다.
     *
     * @return 상세 진단 메시지
     */
    public String getDetailedMessage() {
        StringBuilder builder =
            new StringBuilder();

        builder.append('[')
            .append(operation)
            .append("] ")
            .append(getMessage());

        String location =
            getLocationDescription();

        if (!location.isBlank()) {
            builder.append(" (")
                .append(location)
                .append(')');
        }

        return builder.toString();
    }

    private static String normalizeMessage(
        String message
    ) {
        String normalized =
            message == null
                ? ""
                : message.trim();

        return normalized.isBlank()
            ? "Project RAG indexing failed"
            : normalized;
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

        while (normalized.contains("//")) {
            normalized =
                normalized.replace("//", "/");
        }

        if (normalized.endsWith("/")
            && normalized.length() > 1) {

            normalized =
                normalized.substring(
                    0,
                    normalized.length() - 1
                );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "RagProjectIndexException{" +
            "message='" + getMessage() + '\'' +
            ", operation=" + operation +
            ", projectRoot='" + projectRoot + '\'' +
            ", sourcePath='" + sourcePath + '\'' +
            ", cause="
                + (getCause() == null
                    ? "null"
                    : getCause()
                        .getClass()
                        .getSimpleName()) +
            '}';
    }
}