/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)1
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tool 요청 검증 결과입니다.
 *
 * <p>Tool 실행 전에 입력 인자와 실행 컨텍스트의 유효성을 검사한 결과를
 * 표현합니다.</p>
 */
public final class ToolValidationResult {

    private final boolean valid;
    private final String message;
    private final List<ToolIssue> issues;

    private ToolValidationResult(Builder builder) {
        this.valid = builder.valid;
        this.message = normalizeOptional(builder.message);
        this.issues = immutableIssues(builder.issues);

        validateState();
    }

    /**
     * Builder를 생성합니다.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 기존 검증 결과를 기반으로 Builder를 생성합니다.
     */
    public static Builder builder(
            ToolValidationResult source) {

        Objects.requireNonNull(
                source,
                "source must not be null"
        );

        return new Builder(source);
    }

    /**
     * 검증 성공 결과를 생성합니다.
     */
    public static ToolValidationResult valid() {
        return builder()
                .valid(true)
                .build();
    }

    /**
     * 메시지를 포함하는 검증 성공 결과를 생성합니다.
     */
    public static ToolValidationResult valid(
            String message) {

        return builder()
                .valid(true)
                .message(message)
                .build();
    }

    /**
     * 검증 실패 결과를 생성합니다.
     */
    public static ToolValidationResult invalid(
            String message) {

        return builder()
                .valid(false)
                .message(message)
                .build();
    }

    /**
     * 단일 이슈를 포함하는 검증 실패 결과를 생성합니다.
     */
    public static ToolValidationResult invalid(
            ToolIssue issue) {

        return builder()
                .valid(false)
                .issue(issue)
                .build();
    }

    /**
     * 여러 이슈를 포함하는 검증 실패 결과를 생성합니다.
     */
    public static ToolValidationResult invalid(
            List<ToolIssue> issues) {

        return builder()
                .valid(false)
                .issues(issues)
                .build();
    }

    /**
     * 검증 성공 여부를 반환합니다.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 검증 실패 여부를 반환합니다.
     */
    public boolean isInvalid() {
        return !valid;
    }

    /**
     * 검증 메시지를 반환합니다.
     */
    public String getMessage() {
        return message;
    }

    /**
     * 검증 이슈 목록을 반환합니다.
     *
     * @return 수정할 수 없는 이슈 목록
     */
    public List<ToolIssue> getIssues() {
        return issues;
    }

    /**
     * 메시지가 존재하는지 확인합니다.
     */
    public boolean hasMessage() {
        return message != null;
    }

    /**
     * 검증 이슈가 존재하는지 확인합니다.
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * 이슈 개수를 반환합니다.
     */
    public int getIssueCount() {
        return issues.size();
    }

    /**
     * 특정 심각도의 이슈가 존재하는지 확인합니다.
     */
    public boolean hasSeverity(
            ToolIssueSeverity severity) {

        Objects.requireNonNull(
                severity,
                "severity must not be null"
        );

        return issues.stream()
                .anyMatch(issue ->
                        issue.getSeverity() == severity
                );
    }

    /**
     * 오류 수준 이상의 이슈가 존재하는지 확인합니다.
     */
    public boolean hasErrors() {
        return issues.stream()
                .anyMatch(issue ->
                        issue.getSeverity().isError()
                );
    }

    /**
     * 가장 높은 심각도를 반환합니다.
     *
     * @return 이슈가 없으면 {@code null}
     */
    public ToolIssueSeverity getHighestSeverity() {
        ToolIssueSeverity highest = null;

        for (ToolIssue issue : issues) {
            ToolIssueSeverity severity =
                    issue.getSeverity();

            if (highest == null
                    || severity.getLevel()
                    > highest.getLevel()) {

                highest = severity;
            }
        }

        return highest;
    }

    /**
     * 현재 검증 결과에 이슈 하나를 추가한 새 결과를 반환합니다.
     */
    public ToolValidationResult withIssue(
            ToolIssue issue) {

        return builder(this)
                .issue(issue)
                .valid(false)
                .build();
    }

    private void validateState() {
        if (valid && hasErrors()) {
            throw new IllegalArgumentException(
                    "Valid ToolValidationResult "
                            + "must not contain error issues"
            );
        }

        if (!valid
                && message == null
                && issues.isEmpty()) {

            throw new IllegalArgumentException(
                    "Invalid ToolValidationResult must contain "
                            + "a message or at least one issue"
            );
        }
    }

    private static List<ToolIssue> immutableIssues(
            List<ToolIssue> source) {

        if (source == null || source.isEmpty()) {
            return List.of();
        }

        List<ToolIssue> copied =
                new ArrayList<>(source.size());

        for (ToolIssue issue : source) {
            copied.add(
                    Objects.requireNonNull(
                            issue,
                            "issues must not contain null"
                    )
            );
        }

        return Collections.unmodifiableList(copied);
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Override
    public String toString() {
        return "ToolValidationResult{"
                + "valid=" + valid
                + ", message='" + message + '\''
                + ", issueCount=" + issues.size()
                + '}';
    }

    /**
     * ToolValidationResult Builder입니다.
     */
    public static final class Builder {

        private boolean valid = true;
        private String message;

        private final List<ToolIssue> issues =
                new ArrayList<>();

        private Builder() {
        }

        private Builder(
                ToolValidationResult source) {

            this.valid = source.valid;
            this.message = source.message;
            this.issues.addAll(source.issues);
        }

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder issue(ToolIssue issue) {
            this.issues.add(
                    Objects.requireNonNull(
                            issue,
                            "issue must not be null"
                    )
            );

            return this;
        }

        public Builder issues(
                List<ToolIssue> issues) {

            Objects.requireNonNull(
                    issues,
                    "issues must not be null"
            );

            for (ToolIssue issue : issues) {
                issue(issue);
            }

            return this;
        }

        public Builder clearIssues() {
            this.issues.clear();
            return this;
        }

        public ToolValidationResult build() {
            return new ToolValidationResult(this);
        }
    }
}