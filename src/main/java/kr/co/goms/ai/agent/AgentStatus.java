/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent;

/**
 * AI Agent 실행 상태입니다.
 */
public enum AgentStatus {

    /**
     * Agent 실행이 아직 시작되지 않은 상태입니다.
     */
    PENDING,

    /**
     * Agent가 현재 실행 중인 상태입니다.
     */
    RUNNING,

    /**
     * Agent 실행이 정상적으로 완료된 상태입니다.
     */
    COMPLETED,

    /**
     * Agent 실행 중 오류가 발생한 상태입니다.
     */
    FAILED,

    /**
     * 사용자 또는 시스템에 의해 실행이 취소된 상태입니다.
     */
    CANCELLED,

    /**
     * 설정된 최대 반복 횟수에 도달하여 실행이 종료된 상태입니다.
     */
    ITERATION_LIMIT_REACHED;

    /**
     * 더 이상 실행이 진행되지 않는 최종 상태인지 확인합니다.
     *
     * @return 최종 상태이면 {@code true}
     */
    public boolean isTerminal() {
        return this == COMPLETED
                || this == FAILED
                || this == CANCELLED
                || this == ITERATION_LIMIT_REACHED;
    }

    /**
     * 정상 완료 상태인지 확인합니다.
     *
     * @return 정상 완료이면 {@code true}
     */
    public boolean isSuccess() {
        return this == COMPLETED;
    }

    /**
     * 오류 또는 비정상 종료 상태인지 확인합니다.
     *
     * @return 실패 성격의 상태이면 {@code true}
     */
    public boolean isFailure() {
        return this == FAILED
                || this == CANCELLED
                || this == ITERATION_LIMIT_REACHED;
    }

    /**
     * 실행 가능한 상태인지 확인합니다.
     *
     * @return 대기 또는 실행 중 상태이면 {@code true}
     */
    public boolean isActive() {
        return this == PENDING
                || this == RUNNING;
    }
}