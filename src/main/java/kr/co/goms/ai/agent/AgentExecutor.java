/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent;

import kr.co.goms.ai.agent.event.AgentRagEventListener;

/**
 * AI Agent 실행 진입점을 정의하는 인터페이스입니다.
 *
 * <p>구현체는 일반적으로 다음 절차를 수행합니다.</p>
 * <ol>
 *     <li>Agent 요청 검증</li>
 *     <li>AgentContext 생성</li>
 *     <li>시스템 및 사용자 메시지 구성</li>
 *     <li>LLM 호출</li>
 *     <li>Tool Calling 여부 판단</li>
 *     <li>Tool 실행 결과를 대화에 반영</li>
 *     <li>최종 AgentResponse 생성</li>
 * </ol>
 */
public interface AgentExecutor {

    /**
     * Agent 요청을 실행합니다.
     *
     * @param request Agent 실행 요청
     * @return Agent 실행 결과
     * @throws AgentException Agent 실행 중 오류가 발생한 경우
     */
    AgentResponse execute(AgentRequest request);

    /**
     * 기존 AgentContext를 사용하여 Agent를 실행합니다.
     *
     * <p>외부에서 Context를 생성하거나, 실행 상태를 관찰하거나,
     * 취소 요청을 전달해야 하는 경우 사용할 수 있습니다.</p>
     *
     * @param context Agent 실행 컨텍스트
     * @return Agent 실행 결과
     * @throws AgentException Agent 실행 중 오류가 발생한 경우
     */
    AgentResponse execute(AgentContext context);

    /**
     * AgentExecutor가 현재 실행 가능한 상태인지 확인합니다.
     *
     * <p>구현체에서는 LLM 연결 상태, ToolRegistry 구성,
     * 필수 설정값 등을 기준으로 판단할 수 있습니다.</p>
     *
     * @return 실행 가능하면 {@code true}
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 요청으로부터 AgentContext를 생성하여 실행합니다.
     *
     * <p>기본 구현은 새 Context를 생성한 후
     * {@link #execute(AgentContext)}를 호출합니다.</p>
     *
     * @param request Agent 실행 요청
     * @return Agent 실행 결과
     */
    default AgentResponse executeWithNewContext(AgentRequest request) {

        if (request == null) throw AgentException.invalidRequest("Agent request must not be null.");

        return execute(new AgentContext(request));
    }

    /**
     * Tool 실행 결과 Listener를 등록합니다.
     *
     * @param listener Tool 실행 결과 Listener
     */
    default void addToolResultListener(AgentToolResultListener listener) {
    }

    /**
     * Tool 실행 결과 Listener를 제거합니다.
     *
     * @param listener Tool 실행 결과 Listener
     */
    default void removeToolResultListener(AgentToolResultListener listener) {
    }

    /**
     * RAG 실행 이벤트 Listener를 등록합니다.
     *
     * @param listener RAG 실행 이벤트 Listener
     */
    default void addRagEventListener(AgentRagEventListener listener) {
    }

    /**
     * RAG 실행 이벤트 Listener를 제거합니다.
     *
     * @param listener RAG 실행 이벤트 Listener
     */
    default void removeRagEventListener(AgentRagEventListener listener) {
    }
}