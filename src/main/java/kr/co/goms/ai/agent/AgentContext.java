/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import kr.co.goms.ai.llm.LlmMessage;
import kr.co.goms.ai.tool.ToolResult;

/**
 * AI Agent 실행 중 공유되는 상태와 데이터를 관리합니다.
 *
 * <p>AgentContext는 하나의 Agent 요청이 실행되는 동안 다음 정보를
 * 누적하고 관리합니다.</p>
 *
 * <ul>
 *     <li>요청 정보</li>
 *     <li>LLM 대화 메시지</li>
 *     <li>Tool 실행 결과</li>
 *     <li>현재 반복 횟수</li>
 *     <li>확장 속성</li>
 *     <li>실행 취소 상태</li>
 *     <li>시작 및 종료 시각</li>
 * </ul>
 *
 * <p>하나의 AgentContext 인스턴스를 여러 Agent 실행에서
 * 재사용하지 않아야 합니다.</p>
 */
public final class AgentContext {

    private final AgentRequest request;

    private final List<LlmMessage> messages;
    private final List<ToolResult> toolResults;
    private final Map<String, Object> attributes;

    private final AtomicBoolean cancellationRequested;

    private AgentStatus status;
    private int iteration;

    private final Instant startedAt;
    private Instant completedAt;

    /**
     * Agent 요청으로 실행 컨텍스트를 생성합니다.
     *
     * @param request Agent 실행 요청
     */
    public AgentContext(AgentRequest request) {
        this.request = Objects.requireNonNull(
                request,
                "request must not be null"
        );

        this.messages = new ArrayList<>();
        this.toolResults = new ArrayList<>();
        this.attributes = new LinkedHashMap<>();
        this.cancellationRequested = new AtomicBoolean(false);

        this.status = AgentStatus.PENDING;
        this.iteration = 0;
        this.startedAt = Instant.now();

        initializeFromRequest(request);
    }

    /**
     * AgentRequest의 초기 데이터를 Context에 복사합니다.
     */
    private void initializeFromRequest(AgentRequest request) {
        this.messages.addAll(request.getMessages());
        this.attributes.putAll(request.getAttributes());
    }

    /**
     * 원본 Agent 요청을 반환합니다.
     */
    public AgentRequest getRequest() {
        return request;
    }

    /**
     * 요청 식별자를 반환합니다.
     */
    public String getRequestId() {
        return request.getRequestId();
    }

    /**
     * 세션 식별자를 반환합니다.
     */
    public String getSessionId() {
        return request.getSessionId();
    }

    /**
     * 사용자 실행 지시사항을 반환합니다.
     */
    public String getInstruction() {
        return request.getInstruction();
    }

    /**
     * 현재 Agent 실행 상태를 반환합니다.
     */
    public synchronized AgentStatus getStatus() {
        return status;
    }

    /**
     * Agent 실행 상태를 변경합니다.
     *
     * @param status 변경할 상태
     */
    public synchronized void setStatus(AgentStatus status) {
        Objects.requireNonNull(
                status,
                "status must not be null"
        );

        validateStatusTransition(this.status, status);

        this.status = status;

        if (status.isTerminal() && completedAt == null) {
            completedAt = Instant.now();
        }
    }

    /**
     * Agent 실행을 시작 상태로 변경합니다.
     */
    public void markRunning() {
        setStatus(AgentStatus.RUNNING);
    }

    /**
     * Agent 실행을 정상 완료 상태로 변경합니다.
     */
    public void markCompleted() {
        setStatus(AgentStatus.COMPLETED);
    }

    /**
     * Agent 실행을 실패 상태로 변경합니다.
     */
    public void markFailed() {
        setStatus(AgentStatus.FAILED);
    }

    /**
     * Agent 실행을 반복 제한 도달 상태로 변경합니다.
     */
    public void markIterationLimitReached() {
        setStatus(AgentStatus.ITERATION_LIMIT_REACHED);
    }

    /**
     * Agent 실행 취소를 요청합니다.
     *
     * <p>실제 실행 중단 여부는 AgentExecutor가
     * {@link #isCancellationRequested()}를 확인하여 결정합니다.</p>
     */
    public void requestCancellation() {
        cancellationRequested.set(true);
    }

    /**
     * 취소 요청 여부를 반환합니다.
     */
    public boolean isCancellationRequested() {
        return cancellationRequested.get();
    }

    /**
     * 취소 요청을 확인하고 현재 상태를 CANCELLED로 변경합니다.
     *
     * @return 취소가 처리되었으면 {@code true}
     */
    public synchronized boolean cancelIfRequested() {
        if (!cancellationRequested.get()) {
            return false;
        }

        if (!status.isTerminal()) {
            setStatus(AgentStatus.CANCELLED);
        }

        return true;
    }

    /**
     * 현재 Agent 반복 횟수를 반환합니다.
     */
    public synchronized int getIteration() {
        return iteration;
    }

    /**
     * 최대 반복 횟수를 반환합니다.
     */
    public int getMaxIterations() {
        return request.getMaxIterations();
    }

    /**
     * 다음 반복으로 이동합니다.
     *
     * @return 증가된 반복 횟수
     * @throws AgentException 최대 반복 횟수를 초과한 경우
     */
    public synchronized int nextIteration() {
        if (iteration >= request.getMaxIterations()) {
            throw new AgentException(
                    "Agent maximum iteration limit reached: "
                            + request.getMaxIterations()
            );
        }

        iteration++;

        return iteration;
    }

    /**
     * 추가 실행이 가능한지 확인합니다.
     */
    public synchronized boolean canContinue() {
        return !status.isTerminal()
                && !cancellationRequested.get()
                && iteration < request.getMaxIterations();
    }

    /**
     * 최대 반복 횟수에 도달했는지 확인합니다.
     */
    public synchronized boolean isIterationLimitReached() {
        return iteration >= request.getMaxIterations();
    }

    /**
     * LLM 메시지를 추가합니다.
     */
    public synchronized void addMessage(LlmMessage message) {
        messages.add(
                Objects.requireNonNull(
                        message,
                        "message must not be null"
                )
        );
    }

    /**
     * 여러 LLM 메시지를 추가합니다.
     */
    public synchronized void addMessages(
            List<LlmMessage> messages) {

        Objects.requireNonNull(
                messages,
                "messages must not be null"
        );

        for (LlmMessage message : messages) {
            addMessage(message);
        }
    }

    /**
     * 현재까지 누적된 LLM 메시지 목록을 반환합니다.
     *
     * @return 수정할 수 없는 복사본
     */
    public synchronized List<LlmMessage> getMessages() {
        return Collections.unmodifiableList(
                new ArrayList<>(messages)
        );
    }

    /**
     * 마지막 LLM 메시지를 반환합니다.
     *
     * @return 마지막 메시지 또는 {@code null}
     */
    public synchronized LlmMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }

        return messages.get(messages.size() - 1);
    }

    /**
     * LLM 메시지가 존재하는지 확인합니다.
     */
    public synchronized boolean hasMessages() {
        return !messages.isEmpty();
    }

    /**
     * 현재 메시지 개수를 반환합니다.
     */
    public synchronized int getMessageCount() {
        return messages.size();
    }

    /**
     * Tool 실행 결과를 추가합니다.
     */
    public synchronized void addToolResult(
            ToolResult toolResult) {

        toolResults.add(
                Objects.requireNonNull(
                        toolResult,
                        "toolResult must not be null"
                )
        );
    }

    /**
     * 여러 Tool 실행 결과를 추가합니다.
     */
    public synchronized void addToolResults(
            List<ToolResult> toolResults) {

        Objects.requireNonNull(
                toolResults,
                "toolResults must not be null"
        );

        for (ToolResult toolResult : toolResults) {
            addToolResult(toolResult);
        }
    }

    /**
     * 현재까지 누적된 Tool 결과를 반환합니다.
     *
     * @return 수정할 수 없는 복사본
     */
    public synchronized List<ToolResult> getToolResults() {
        return Collections.unmodifiableList(
                new ArrayList<>(toolResults)
        );
    }

    /**
     * 마지막 Tool 실행 결과를 반환합니다.
     *
     * @return 마지막 Tool 결과 또는 {@code null}
     */
    public synchronized ToolResult getLastToolResult() {
        if (toolResults.isEmpty()) {
            return null;
        }

        return toolResults.get(toolResults.size() - 1);
    }

    /**
     * Tool 실행 결과가 존재하는지 확인합니다.
     */
    public synchronized boolean hasToolResults() {
        return !toolResults.isEmpty();
    }

    /**
     * Tool 실행 횟수를 반환합니다.
     */
    public synchronized int getToolResultCount() {
        return toolResults.size();
    }

    /**
     * 확장 속성을 설정합니다.
     */
    public synchronized void setAttribute(
            String name,
            Object value) {

        validateAttributeName(name);
        attributes.put(name, value);
    }

    /**
     * 확장 속성을 반환합니다.
     */
    public synchronized Object getAttribute(String name) {
        if (name == null) {
            return null;
        }

        return attributes.get(name);
    }

    /**
     * 확장 속성을 지정한 타입으로 반환합니다.
     */
    public synchronized <T> T getAttribute(
            String name,
            Class<T> type) {

        Objects.requireNonNull(
                type,
                "type must not be null"
        );

        Object value = getAttribute(name);

        if (value == null) {
            return null;
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Agent context attribute type mismatch. "
                            + "name=" + name
                            + ", expected=" + type.getName()
                            + ", actual="
                            + value.getClass().getName()
            );
        }

        return type.cast(value);
    }

    /**
     * 기본값을 포함하여 속성을 반환합니다.
     */
    public synchronized <T> T getAttributeOrDefault(
            String name,
            Class<T> type,
            T defaultValue) {

        T value = getAttribute(name, type);

        return value != null
                ? value
                : defaultValue;
    }

    /**
     * 속성이 존재하는지 확인합니다.
     */
    public synchronized boolean hasAttribute(String name) {
        return name != null
                && attributes.containsKey(name);
    }

    /**
     * 속성을 제거합니다.
     */
    public synchronized Object removeAttribute(String name) {
        validateAttributeName(name);
        return attributes.remove(name);
    }

    /**
     * 전체 속성 목록을 반환합니다.
     *
     * @return 수정할 수 없는 복사본
     */
    public synchronized Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(attributes)
        );
    }

    /**
     * Agent 실행 시작 시각을 반환합니다.
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Agent 실행 완료 시각을 반환합니다.
     */
    public synchronized Instant getCompletedAt() {
        return completedAt;
    }

    /**
     * 현재 Context를 기반으로 AgentResponse Builder를 생성합니다.
     */
    public synchronized AgentResponse.Builder toResponseBuilder() {
        return AgentResponse.builder()
                .requestId(getRequestId())
                .sessionId(getSessionId())
                .status(status)
                .messages(messages)
                .toolResults(toolResults)
                .attributes(attributes)
                .iterations(iteration)
                .startedAt(startedAt)
                .completedAt(completedAt);
    }

    /**
     * 상태 전이를 검증합니다.
     */
    private static void validateStatusTransition(
            AgentStatus current,
            AgentStatus next) {

        if (current == next) {
            return;
        }

        if (current.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot change terminal Agent status. "
                            + "current=" + current
                            + ", next=" + next
            );
        }

        if (current == AgentStatus.PENDING
                && next != AgentStatus.RUNNING
                && next != AgentStatus.CANCELLED
                && next != AgentStatus.FAILED) {

            throw new IllegalStateException(
                    "Invalid Agent status transition. "
                            + "current=" + current
                            + ", next=" + next
            );
        }

        if (current == AgentStatus.RUNNING
                && next == AgentStatus.PENDING) {

            throw new IllegalStateException(
                    "Running Agent cannot return to PENDING"
            );
        }
    }

    private static void validateAttributeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "attribute name must not be blank"
            );
        }
    }
}
