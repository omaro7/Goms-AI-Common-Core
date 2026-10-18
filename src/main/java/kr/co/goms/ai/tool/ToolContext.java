/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.util.ToolUtil;

/**
 * Tool 실행 중 공유되는 컨텍스트 정보입니다.
 *
 * <p>
 * Agent 요청과 Tool 실행 사이에서 다음 정보를 전달합니다.
 * </p>
 *
 * <ul>
 *     <li>Agent 요청 식별자</li>
 *     <li>대화 세션 식별자</li>
 *     <li>현재 프로젝트 루트</li>
 *     <li>현재 문서</li>
 *     <li>기타 확장 속성</li>
 * </ul>
 *
 * <p>사용 예시:</p>
 *
 * <pre>
 * ToolContext context = ToolContext.builder()
 *         .requestId("request-001")
 *         .sessionId("session-001")
 *         .projectRoot(Path.of("C:/workspace/GomsBook"))
 *         .currentFile("OEBPS/Text/chapter01.xhtml")
 *         .build();
 * </pre>
 *
 * <p>
 * 기존 코드와의 호환성을 위해 {@code projectRoot}와
 * {@code projectPath} 속성을 모두 지원합니다.
 * </p>
 */
public final class ToolContext {

    /**
     * 현재 프로젝트 루트 속성명.
     */
    public static final String PROJECT_ROOT_ATTRIBUTE =
            "projectRoot";

    /**
     * 기존 코드에서 사용하는 프로젝트 경로 속성명.
     */
    public static final String PROJECT_PATH_ATTRIBUTE =
            "projectPath";

    /**
     * 현재 편집 중인 문서 속성명.
     */
    public static final String CURRENT_FILE_ATTRIBUTE =
            "currentFile";

    private final String requestId;
    private final String sessionId;
    private final String runId;
    private final String projectId;
    private final Map<String, Object> attributes;

    private ToolContext(
            Builder builder) {

    	this.requestId = normalizeOptional(builder.requestId);
    	this.sessionId = normalizeOptional(builder.sessionId);
    	this.runId = normalizeOptional(builder.runId);
    	this.projectId = normalizeOptional(builder.projectId);
    	this.attributes = immutableAttributes(builder.attributes);
    }

    /**
     * 빈 ToolContext를 생성합니다.
     */
    public ToolContext() {
        this(builder());
    }

    /**
     * 확장 속성만 포함하는 ToolContext를 생성합니다.
     *
     * @param attributes 확장 속성
     */
    public ToolContext(
            Map<String, Object> attributes) {

        this(
                builder()
                        .attributes(
                                attributes
                        )
        );
    }

    /**
     * Builder를 생성합니다.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기존 Context를 기반으로 Builder를 생성합니다.
     *
     * @param source 원본 ToolContext
     * @return 원본 값이 복사된 Builder
     */
    public static Builder builder(
            ToolContext source) {

        Objects.requireNonNull(
                source,
                "source must not be null"
        );

        return new Builder(
                source
        );
    }

    /**
     * Agent 요청 식별자를 반환합니다.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 대화 세션 식별자를 반환합니다.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 확장 속성 목록을 반환합니다.
     *
     * @return 수정할 수 없는 속성 Map
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * 요청 식별자가 존재하는지 확인합니다.
     */
    public boolean hasRequestId() {
        return requestId != null;
    }

    /**
     * 세션 식별자가 존재하는지 확인합니다.
     */
    public boolean hasSessionId() {
        return sessionId != null;
    }

    /**
     * 확장 속성이 존재하는지 확인합니다.
     */
    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    /**
     * 현재 프로젝트 루트를 반환합니다.
     *
     * <p>
     * 다음 순서로 속성을 검색합니다.
     * </p>
     *
     * <ol>
     *     <li>{@code projectRoot}</li>
     *     <li>{@code projectPath}</li>
     * </ol>
     *
     * <p>
     * 값은 {@link Path} 또는 {@link String} 타입을 지원합니다.
     * </p>
     *
     * @return 정규화된 프로젝트 절대 경로 또는 {@code null}
     */
    public Path getProjectRoot() {

        Object value =
                getAttribute(
                        PROJECT_ROOT_ATTRIBUTE
                );

        if (value == null) {
            value =
                    getAttribute(
                            PROJECT_PATH_ATTRIBUTE
                    );
        }

        return toPath(
                value
        );
    }

    /**
     * 필수 프로젝트 루트를 반환합니다.
     *
     * @return 프로젝트 루트
     * @throws IllegalArgumentException 프로젝트 루트가 없는 경우
     */
    public Path requireProjectRoot() {

        Path projectRoot =
                getProjectRoot();

        if (projectRoot == null) {
            throw new IllegalArgumentException(
                    "Required Tool context project root is missing. "
                            + "Expected attribute: "
                            + PROJECT_ROOT_ATTRIBUTE
                            + " or "
                            + PROJECT_PATH_ATTRIBUTE
            );
        }

        return projectRoot;
    }

    /**
     * 프로젝트 경로를 문자열로 반환합니다.
     *
     * @return 프로젝트 절대 경로 문자열 또는 {@code null}
     */
    public String getProjectPath() {

        Path projectRoot =
                getProjectRoot();

        return projectRoot == null
                ? null
                : projectRoot.toString();
    }

    /**
     * 현재 편집 중인 파일 경로를 반환합니다.
     *
     * @return 현재 파일 경로 또는 {@code null}
     */
    public Path getCurrentFile() {

        Object value =
                getAttribute(
                        CURRENT_FILE_ATTRIBUTE
                );

        if (value == null) {
            return null;
        }

        /*
         * currentFile이 상대 경로라면 projectRoot 기준으로 해석한다.
         */
        Path currentFile =
                toPathRaw(
                        value
                );

        if (currentFile == null) {
            return null;
        }

        if (currentFile.isAbsolute()) {
            return currentFile
                    .toAbsolutePath()
                    .normalize();
        }

        Path projectRoot =
                getProjectRoot();

        if (projectRoot == null) {
            return currentFile
                    .normalize();
        }

        return projectRoot
                .resolve(
                        currentFile
                )
                .toAbsolutePath()
                .normalize();
    }

    /**
     * 현재 파일의 프로젝트 상대 경로를 반환합니다.
     *
     * @return 상대 경로 문자열 또는 {@code null}
     */
    public String getProjectRelativeCurrentFile() {

        Path currentFile =
                getCurrentFile();

        if (currentFile == null) {
            return null;
        }

        Path projectRoot =
                getProjectRoot();

        if (projectRoot != null
                && currentFile
                        .toAbsolutePath()
                        .normalize()
                        .startsWith(projectRoot)) {

            return projectRoot
                    .relativize(
                            currentFile
                                    .toAbsolutePath()
                                    .normalize()
                    )
                    .toString()
                    .replace(
                            '\\',
                            '/'
                    );
        }

        return currentFile
                .toString()
                .replace(
                        '\\',
                        '/'
                );
    }

    /**
     * 지정한 속성이 존재하는지 확인합니다.
     *
     * @param name 속성명
     * @return 속성이 존재하면 {@code true}
     */
    public boolean containsAttribute(
            String name) {

        return name != null
                && attributes.containsKey(
                        name
                );
    }

    /**
     * 속성값을 반환합니다.
     *
     * @param name 속성명
     * @return 속성값 또는 {@code null}
     */
    public Object getAttribute(
            String name) {

        if (name == null) {
            return null;
        }

        return attributes.get(
                name
        );
    }

    /**
     * 속성값을 지정한 타입으로 반환합니다.
     *
     * @param name 속성명
     * @param type 반환 타입
     * @param <T> 반환 타입
     * @return 속성값 또는 {@code null}
     */
    public <T> T getAttribute(
            String name,
            Class<T> type) {

        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        Object value =
                getAttribute(
                        name
                );

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Tool context attribute type mismatch. "
                            + "name=" + name
                            + ", expected="
                            + type.getName()
                            + ", actual="
                            + value.getClass().getName()
            );
        }

        return type.cast(
                value
        );
    }

    /**
     * 속성값을 기본값과 함께 반환합니다.
     */
    public <T> T getAttributeOrDefault(
            String name,
            Class<T> type,
            T defaultValue) {

        T value =
                getAttribute(
                        name,
                        type
                );

        return value != null
                ? value
                : defaultValue;
    }

    /**
     * 필수 속성값을 반환합니다.
     *
     * @param name 속성명
     * @return 속성값
     */
    public Object requireAttribute(
            String name) {

        validateAttributeName(
                name
        );

        if (!attributes.containsKey(
                name)) {

            throw new IllegalArgumentException(
                    "Required Tool context attribute is missing: "
                            + name
            );
        }

        Object value =
                attributes.get(
                        name
                );

        if (value == null) {
            throw new IllegalArgumentException(
                    "Required Tool context attribute "
                            + "must not be null: "
                            + name
            );
        }

        return value;
    }

    /**
     * 필수 속성값을 지정한 타입으로 반환합니다.
     */
    public <T> T requireAttribute(
            String name,
            Class<T> type) {

        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        Object value =
                requireAttribute(
                        name
                );

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Required Tool context attribute type mismatch. "
                            + "name=" + name
                            + ", expected="
                            + type.getName()
                            + ", actual="
                            + value.getClass().getName()
            );
        }

        return type.cast(
                value
        );
    }

    /**
     * 필수 문자열 속성을 반환합니다.
     */
    public String requireStringAttribute(
            String name) {

        Object value =
                requireAttribute(
                        name
                );

        if (!(value instanceof String stringValue)) {
            throw new IllegalArgumentException(
                    "Required Tool context attribute "
                            + "must be a string: "
                            + name
            );
        }

        if (stringValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Required Tool context string attribute "
                            + "must not be blank: "
                            + name
            );
        }

        return stringValue;
    }

    /**
     * 프로젝트 경로 속성을 Path로 변환합니다.
     */
    private static Path toPath(
            Object value) {

        Path path =
                toPathRaw(
                        value
                );

        if (path == null) {
            return null;
        }

        return path
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Path 또는 String 속성을 Path로 변환합니다.
     */
    private static Path toPathRaw(
            Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Path path) {
            return path.normalize();
        }

        if (value instanceof String pathText) {

            if (pathText.isBlank()) {
                return null;
            }

            return Path.of(
                    pathText.trim()
            ).normalize();
        }

        throw new IllegalArgumentException(
                "Tool context path attribute must be "
                        + "Path or String. actual="
                        + value.getClass().getName()
        );
    }

    private static String normalizeOptional(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    private static Map<String, Object>
            immutableAttributes(
                    Map<String, Object> source) {

        if (source == null
                || source.isEmpty()) {

            return Map.of();
        }

        Map<String, Object> copied =
                new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry
                : source.entrySet()) {

            String name =
                    entry.getKey();

            validateAttributeName(
                    name
            );

            copied.put(
                    name.trim(),
                    ToolUtil.deepCopy(
                            entry.getValue()
                    )
            );
        }

        return Collections.unmodifiableMap(
                copied
        );
    }

    private static void validateAttributeName(
            String name) {

        if (name == null
                || name.isBlank()) {

            throw new IllegalArgumentException(
                    "attribute name must not be blank"
            );
        }
    }
    
    public String getRunId() {
        return runId;
    }

    public String getProjectId() {
        return projectId;
    }
    
    public boolean hasRunId() {
        return runId != null;
    }

    public boolean hasProjectId() {
        return projectId != null;
    }

    @Override
    public String toString() {

        return "ToolContext{"
                + "requestId='" + requestId + '\''
                + ", sessionId='" + sessionId + '\''
                + ", runId='" + runId + '\''
                + ", projectId='" + projectId + '\''
                + ", projectRoot='" + getProjectRoot() + '\''
                + ", currentFile='" + getProjectRelativeCurrentFile() + '\''
                + ", attributeNames=" + attributes.keySet()
                + '}';
    }

    /**
     * ToolContext Builder.
     */
    public static final class Builder {

    	private String requestId;
    	private String sessionId;
    	private String runId;
    	private String projectId;

        private final Map<String, Object> attributes =
                new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(
                ToolContext source) {

            this.requestId = source.requestId;
            this.sessionId = source.sessionId;
            this.runId = source.runId;
            this.projectId = source.projectId;

            this.attributes.putAll(
                    source.attributes
            );
        }
        /**
         * Agent 요청 식별자를 설정합니다.
         */
        public Builder requestId(
                String requestId) {

            this.requestId =
                    requestId;

            return this;
        }

        /**
         * 세션 식별자를 설정합니다.
         */
        public Builder sessionId(
                String sessionId) {

            this.sessionId =
                    sessionId;

            return this;
        }
        
        public Builder runId(
                String runId) {

            this.runId = runId;

            return this;
        }

        public Builder projectId(
                String projectId) {

            this.projectId = projectId;

            return this;
        }        

        /**
         * 프로젝트 루트를 설정합니다.
         *
         * <p>
         * 내부적으로 projectRoot 속성에 Path 타입으로 저장합니다.
         * </p>
         */
        public Builder projectRoot(
                Path projectRoot) {

            Objects.requireNonNull(
                    projectRoot,
                    "projectRoot must not be null"
            );

            return attribute(
                    PROJECT_ROOT_ATTRIBUTE,
                    projectRoot
                            .toAbsolutePath()
                            .normalize()
            );
        }

        /**
         * 프로젝트 루트를 문자열로 설정합니다.
         */
        public Builder projectRoot(
                String projectRoot) {

            if (projectRoot == null
                    || projectRoot.isBlank()) {

                throw new IllegalArgumentException(
                        "projectRoot must not be blank"
                );
            }

            return projectRoot(
                    Path.of(
                            projectRoot.trim()
                    )
            );
        }

        /**
         * 기존 projectPath 속성을 설정합니다.
         *
         * <p>
         * 기존 코드 호환을 위한 메서드입니다.
         * 신규 코드에서는 {@link #projectRoot(Path)} 사용을 권장합니다.
         * </p>
         */
        public Builder projectPath(
                Path projectPath) {

            Objects.requireNonNull(
                    projectPath,
                    "projectPath must not be null"
            );

            return attribute(
                    PROJECT_PATH_ATTRIBUTE,
                    projectPath
                            .toAbsolutePath()
                            .normalize()
            );
        }

        /**
         * 현재 projectPath 문자열 속성을 설정합니다.
         */
        public Builder projectPath(
                String projectPath) {

            if (projectPath == null
                    || projectPath.isBlank()) {

                throw new IllegalArgumentException(
                        "projectPath must not be blank"
                );
            }

            return projectPath(
                    Path.of(
                            projectPath.trim()
                    )
            );
        }

        /**
         * 현재 파일을 설정합니다.
         */
        public Builder currentFile(
                Path currentFile) {

            Objects.requireNonNull(
                    currentFile,
                    "currentFile must not be null"
            );

            return attribute(
                    CURRENT_FILE_ATTRIBUTE,
                    currentFile.normalize()
            );
        }

        /**
         * 현재 파일을 문자열로 설정합니다.
         */
        public Builder currentFile(
                String currentFile) {

            if (currentFile == null
                    || currentFile.isBlank()) {

                throw new IllegalArgumentException(
                        "currentFile must not be blank"
                );
            }

            return currentFile(
                    Path.of(
                            currentFile.trim()
                    )
            );
        }

        /**
         * 확장 속성을 추가하거나 변경합니다.
         */
        public Builder attribute(
                String name,
                Object value) {

            validateAttributeName(
                    name
            );

            this.attributes.put(
                    name.trim(),
                    value
            );

            return this;
        }

        /**
         * 여러 확장 속성을 추가합니다.
         */
        public Builder attributes(
                Map<String, ?> attributes) {

            Objects.requireNonNull(
                    attributes,
                    "attributes must not be null"
            );

            for (Map.Entry<String, ?> entry
                    : attributes.entrySet()) {

                attribute(
                        entry.getKey(),
                        entry.getValue()
                );
            }

            return this;
        }

        /**
         * 확장 속성을 제거합니다.
         */
        public Builder removeAttribute(
                String name) {

            validateAttributeName(
                    name
            );

            this.attributes.remove(
                    name
            );

            return this;
        }

        /**
         * 모든 확장 속성을 제거합니다.
         */
        public Builder clearAttributes() {

            this.attributes.clear();

            return this;
        }

        /**
         * ToolContext를 생성합니다.
         */
        public ToolContext build() {
            return new ToolContext(
                    this
            );
        }
    }
}