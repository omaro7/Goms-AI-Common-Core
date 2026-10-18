/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)1
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

/**
 * Tool 실행 상태입니다.
 */
public enum ToolStatus {

    /**
     * Tool 실행 전 대기 상태입니다.
     */
    PENDING,

    /**
     * Tool이 현재 실행 중인 상태입니다.
     */
    RUNNING,

    /**
     * Tool 실행이 정상적으로 완료된 상태입니다.
     */
    SUCCESS,

    /**
     * Tool 요청 검증에 실패한 상태입니다.
     */
    VALIDATION_FAILED,

    /**
     * Tool 실행 중 오류가 발생한 상태입니다.
     */
    FAILED,

    /**
     * Tool 실행이 취소된 상태입니다.
     */
    CANCELLED,

    /**
     * Tool 실행 제한시간을 초과한 상태입니다.
     */
    TIMEOUT;

    /**
     * Tool 실행이 종료된 최종 상태인지 확인합니다.
     *
     * @return 최종 상태이면 {@code true}
     */
    public boolean isTerminal() {
        return this == SUCCESS
                || this == VALIDATION_FAILED
                || this == FAILED
                || this == CANCELLED
                || this == TIMEOUT;
    }

    /**
     * Tool 실행이 정상적으로 성공했는지 확인합니다.
     *
     * @return 성공 상태이면 {@code true}
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * Tool 실행이 실패 성격의 상태인지 확인합니다.
     *
     * @return 실패, 검증 실패, 취소 또는 시간 초과이면 {@code true}
     */
    public boolean isFailure() {
        return this == VALIDATION_FAILED
                || this == FAILED
                || this == CANCELLED
                || this == TIMEOUT;
    }

    /**
     * Tool 실행이 아직 진행 가능한 상태인지 확인합니다.
     *
     * @return 대기 또는 실행 중이면 {@code true}
     */
    public boolean isActive() {
        return this == PENDING
                || this == RUNNING;
    }

    /**
     * Tool 요청 검증에 실패한 상태인지 확인합니다.
     *
     * @return 검증 실패이면 {@code true}
     */
    public boolean isValidationFailure() {
        return this == VALIDATION_FAILED;
    }

    /**
     * Tool 실행이 취소되었는지 확인합니다.
     *
     * @return 취소 상태이면 {@code true}
     */
    public boolean isCancelled() {
        return this == CANCELLED;
    }

    /**
     * Tool 실행 제한시간을 초과했는지 확인합니다.
     *
     * @return 시간 초과 상태이면 {@code true}
     */
    public boolean isTimeout() {
        return this == TIMEOUT;
    }
}