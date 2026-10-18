/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent.approval;

public enum AgentApprovalStatus {

	// XHTML 미리보기 생성 후 사용자 승인 대기
    PENDING,

    // 사용자가 승인 버튼 클릭
    APPROVED,

    // 사용자가 취소 또는 거절 버튼 클릭
    REJECTED,

    // 승인 요청이 더 이상 유효하지 않음
    EXPIRED
}