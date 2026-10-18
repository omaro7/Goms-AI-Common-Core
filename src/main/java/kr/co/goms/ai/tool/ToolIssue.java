/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tool 검증 또는 실행 과정에서 발견된 개별 이슈입니다.
 *
 * <p>이슈 코드, 심각도, 메시지, 관련 필드 및 확장 정보를 포함합니다.</p>
 */
public final class ToolIssue {

    private final String code;
    private final ToolIssueSeverity severity;
    private final String message;
    private final String field;
    private final Map<String, Object> details;

    private ToolIssue(Builder builder) {
        this.code = normalizeOptional(builder.code);

        this.severity = Objects.requireNonNull(
                builder.severity,
                "severity must not be null"
        );

        this.message = requireMessage(builder.message);
        this.field = normalizeOptional(builder.field);
        this.details = immutableDetails(builder.details);
    }

    /**
     * 새로운 Builder를 생성합니다.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기존 ToolIssue를 기반으로 Builder를 생성합니다.
     */
    public static Builder builder(ToolIssue source) {
        Objects.requireNonNull(
                source,
                "source must not be null"
        );

        return new Builder(source);
    }

    /**
     * 정보 수준 이슈를 생성합니다.
     */
    public static ToolIssue info(String message) {
        return builder()
                .severity(ToolIssueSeverity.INFO)
                .message(message)
                .build();
    }

    /**
     * 경고 수준 이슈를 생성합니다.
     */
    public static ToolIssue warning(String message) {
        return builder()
                .severity(ToolIssueSeverity.WARNING)
                .message(message)
                .build();
    }

    /**
     * 오류 수준 이슈를 생성합니다.
     */
    public static ToolIssue error(String message) {
        return builder()
                .severity(ToolIssueSeverity.ERROR)
                .message(message)
                .build();
    }

    /**
     * 치명적 오류 이슈를 생성합니다.
     */
    public static ToolIssue fatal(String message) {
        return builder()
                .severity(ToolIssueSeverity.FATAL)
                .message(message)
                .build();
    }

    /**
     * 이슈 코드를 반환합니다.
     */
    public String getCode() {
        return code;
    }

    /**
     * 이슈 심각도를 반환합니다.
     */
    public ToolIssueSeverity getSeverity() {
        return severity;
    }

    /**
     * 이슈 메시지를 반환합니다.
     */
    public String getMessage() {
        return message;
    }

    /**
     * 관련 입력 필드 또는 속성명을 반환합니다.
     */
    public String getField() {
        return field;
    }

    /**
     * 확장 상세 정보를 반환합니다.
     *
     * @return 수정할 수 없는 상세 정보 Map
     */
    public Map<String, Object> getDetails() {
        return details;
    }

    /**
     * 이슈 코드가 존재하는지 확인합니다.
     */
    public boolean hasCode() {
        return code != null;
    }

    /**
     * 관련 필드가 존재하는지 확인합니다.
     */
    public boolean hasField() {
        return field != null;
    }

    /**
     * 상세 정보가 존재하는지 확인합니다.
     */
    public boolean hasDetails() {
        return !details.isEmpty();
    }

    /**
     * 정보 수준인지 확인합니다.
     */
    public boolean isInfo() {
        return severity.isInfo();
    }

    /**
     * 경고 수준인지 확인합니다.
     */
    public boolean isWarning() {
        return severity.isWarning();
    }

    /**
     * 오류 수준인지 확인합니다.
     */
    public boolean isError() {
        return severity.isError();
    }

    /**
     * 치명적 오류인지 확인합니다.
     */
    public boolean isFatal() {
        return severity.isFatal();
    }

    /**
     * 특정 상세 정보값을 반환합니다.
     */
    public Object getDetail(String name) {
        if (name == null) {
            return null;
        }

        return details.get(name);
    }

    /**
     * 특정 상세 정보값을 지정한 타입으로 반환합니다.
     */
    public <T> T getDetail(
            String name,
            Class<T> type) {

        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        Object value = getDetail(name);

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Tool issue detail type mismatch. "
                            + "name=" + name
                            + ", expected=" + type.getName()
                            + ", actual="
                            + value.getClass().getName()
            );
        }

        return type.cast(value);
    }

    private static String requireMessage(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "message must not be blank"
            );
        }

        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static Map<String, Object> immutableDetails(
            Map<String, Object> source) {

        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> copied =
                new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry
                : source.entrySet()) {

            String key = entry.getKey();

            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException(
                        "detail name must not be blank"
                );
            }

            copied.put(
                    key.trim(),
                    deepCopyValue(entry.getValue())
            );
        }

        return Collections.unmodifiableMap(copied);
    }

    private static Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copied =
                    new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    throw new IllegalArgumentException(
                            "Nested detail Map must not contain null keys"
                    );
                }

                copied.put(
                        String.valueOf(entry.getKey()),
                        deepCopyValue(entry.getValue())
                );
            }

            return Collections.unmodifiableMap(copied);
        }

        if (value instanceof Iterable<?> iterable) {
            java.util.List<Object> copied =
                    new java.util.ArrayList<>();

            for (Object item : iterable) {
                copied.add(deepCopyValue(item));
            }

            return Collections.unmodifiableList(copied);
        }

        return value;
    }

    @Override
    public String toString() {
        return "ToolIssue{"
                + "code='" + code + '\''
                + ", severity=" + severity
                + ", message='" + message + '\''
                + ", field='" + field + '\''
                + ", detailNames=" + details.keySet()
                + '}';
    }

    /**
     * ToolIssue Builder입니다.
     */
    public static final class Builder {

        private String code;
        private ToolIssueSeverity severity =
                ToolIssueSeverity.ERROR;

        private String message;
        private String field;

        private final Map<String, Object> details =
                new LinkedHashMap<>();

        private Builder() {
        }

        private Builder(ToolIssue source) {
            this.code = source.code;
            this.severity = source.severity;
            this.message = source.message;
            this.field = source.field;
            this.details.putAll(source.details);
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder severity(
                ToolIssueSeverity severity) {

            this.severity = Objects.requireNonNull(
                    severity,
                    "severity must not be null"
            );

            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        /**
         * 상세 정보를 추가하거나 변경합니다.
         */
        public Builder detail(
                String name,
                Object value) {

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "detail name must not be blank"
                );
            }

            this.details.put(
                    name.trim(),
                    value
            );

            return this;
        }

        /**
         * 여러 상세 정보를 추가합니다.
         */
        public Builder details(
                Map<String, ?> values) {

            Objects.requireNonNull(
                    values,
                    "values must not be null"
            );

            for (Map.Entry<String, ?> entry
                    : values.entrySet()) {

                detail(
                        entry.getKey(),
                        entry.getValue()
                );
            }

            return this;
        }

        public Builder removeDetail(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(
                        "detail name must not be blank"
                );
            }

            this.details.remove(name);
            return this;
        }

        public Builder clearDetails() {
            this.details.clear();
            return this;
        }

        public ToolIssue build() {
            return new ToolIssue(this);
        }
    }
}