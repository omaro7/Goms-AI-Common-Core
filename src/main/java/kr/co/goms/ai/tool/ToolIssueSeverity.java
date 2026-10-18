/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)1
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

/**
 * Tool 검증 또는 실행 과정에서 발견된 이슈의 심각도입니다.
 *
 * <p>심각도 비교를 위해 각 상태는 숫자 레벨을 가집니다.
 * 숫자가 클수록 심각한 이슈입니다.</p>
 */
public enum ToolIssueSeverity {

    /**
     * 실행에 영향을 주지 않는 참고 정보입니다.
     */
    INFO(10),

    /**
     * 실행은 가능하지만 확인이 필요한 경고입니다.
     */
    WARNING(20),

    /**
     * 요청 검증 또는 Tool 실행에 영향을 주는 오류입니다.
     */
    ERROR(30),

    /**
     * Tool 실행을 계속할 수 없는 치명적 오류입니다.
     */
    FATAL(40);

    private final int level;

    ToolIssueSeverity(int level) {
        this.level = level;
    }

    /**
     * 심각도 비교용 숫자 레벨을 반환합니다.
     *
     * @return 심각도 레벨
     */
    public int getLevel() {
        return level;
    }

    /**
     * 정보 수준인지 확인합니다.
     *
     * @return INFO이면 {@code true}
     */
    public boolean isInfo() {
        return this == INFO;
    }

    /**
     * 경고 수준인지 확인합니다.
     *
     * @return WARNING이면 {@code true}
     */
    public boolean isWarning() {
        return this == WARNING;
    }

    /**
     * 오류 수준인지 확인합니다.
     *
     * <p>ERROR와 FATAL을 모두 오류로 판단합니다.</p>
     *
     * @return 오류 수준이면 {@code true}
     */
    public boolean isError() {
        return this == ERROR || this == FATAL;
    }

    /**
     * 치명적 오류인지 확인합니다.
     *
     * @return FATAL이면 {@code true}
     */
    public boolean isFatal() {
        return this == FATAL;
    }

    /**
     * 다른 심각도보다 높은지 확인합니다.
     *
     * @param other 비교할 심각도
     * @return 현재 심각도가 더 높으면 {@code true}
     */
    public boolean isHigherThan(
            ToolIssueSeverity other) {

        if (other == null) {
            return true;
        }

        return this.level > other.level;
    }

    /**
     * 다른 심각도 이상인지 확인합니다.
     *
     * @param other 비교할 심각도
     * @return 현재 심각도가 같거나 더 높으면 {@code true}
     */
    public boolean isAtLeast(
            ToolIssueSeverity other) {

        if (other == null) {
            return true;
        }

        return this.level >= other.level;
    }

    /**
     * 두 심각도 중 더 높은 값을 반환합니다.
     *
     * @param first  첫 번째 심각도
     * @param second 두 번째 심각도
     * @return 더 높은 심각도
     */
    public static ToolIssueSeverity max(
            ToolIssueSeverity first,
            ToolIssueSeverity second) {

        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        return first.level >= second.level
                ? first
                : second;
    }
}