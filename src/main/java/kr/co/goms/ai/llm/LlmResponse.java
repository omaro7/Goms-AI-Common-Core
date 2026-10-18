/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LLM 호출 결과를 표준화한 응답 객체입니다.
 *
 * <p>일반 텍스트 응답과 Tool Calling 응답을 모두 지원합니다.</p>
 */
public final class LlmResponse {

    private final String model;
    private final String content;
    private final boolean done;
    private final String doneReason;

    private final int promptTokenCount;
    private final int completionTokenCount;

    private final List<LlmToolCall> toolCalls;

    /**
     * 일반 텍스트 응답을 생성합니다.
     *
     * @param model                응답을 생성한 모델명
     * @param content              응답 본문
     * @param done                 응답 완료 여부
     * @param doneReason           응답 완료 사유
     * @param promptTokenCount     입력 토큰 수
     * @param completionTokenCount 출력 토큰 수
     */
    public LlmResponse(
            String model,
            String content,
            boolean done,
            String doneReason,
            int promptTokenCount,
            int completionTokenCount) {

        this(
                model,
                content,
                done,
                doneReason,
                promptTokenCount,
                completionTokenCount,
                List.of()
        );
    }

    /**
     * Tool Calling을 포함하는 LLM 응답을 생성합니다.
     *
     * @param model                응답을 생성한 모델명
     * @param content              응답 본문
     * @param done                 응답 완료 여부
     * @param doneReason           응답 완료 사유
     * @param promptTokenCount     입력 토큰 수
     * @param completionTokenCount 출력 토큰 수
     * @param toolCalls            LLM이 요청한 Tool 호출 목록
     */
    public LlmResponse(
            String model,
            String content,
            boolean done,
            String doneReason,
            int promptTokenCount,
            int completionTokenCount,
            List<LlmToolCall> toolCalls) {

        this.model = normalizeOptional(model);
        this.content = content == null ? "" : content;
        this.done = done;
        this.doneReason = normalizeOptional(doneReason);

        this.promptTokenCount =
                validateTokenCount(
                        promptTokenCount,
                        "promptTokenCount"
                );

        this.completionTokenCount =
                validateTokenCount(
                        completionTokenCount,
                        "completionTokenCount"
                );

        this.toolCalls = immutableToolCalls(toolCalls);
    }

    /**
     * 최소 정보로 일반 텍스트 응답을 생성합니다.
     *
     * @param model   모델명
     * @param content 응답 본문
     */
    public LlmResponse(
            String model,
            String content) {

        this(
                model,
                content,
                true,
                null,
                0,
                0,
                List.of()
        );
    }

    /**
     * 최소 정보로 Tool Calling 응답을 생성합니다.
     *
     * @param model     모델명
     * @param content   응답 본문
     * @param toolCalls Tool 호출 목록
     */
    public LlmResponse(
            String model,
            String content,
            List<LlmToolCall> toolCalls) {

        this(
                model,
                content,
                true,
                null,
                0,
                0,
                toolCalls
        );
    }

    /**
     * 응답을 생성한 모델명을 반환합니다.
     */
    public String getModel() {
        return model;
    }

    /**
     * LLM의 텍스트 응답을 반환합니다.
     *
     * <p>Tool Calling 응답에서는 비어 있을 수 있습니다.</p>
     */
    public String getContent() {
        return content;
    }

    /**
     * 응답 생성이 완료되었는지 확인합니다.
     */
    public boolean isDone() {
        return done;
    }

    /**
     * 응답 완료 사유를 반환합니다.
     */
    public String getDoneReason() {
        return doneReason;
    }

    /**
     * 입력 Prompt 토큰 수를 반환합니다.
     */
    public int getPromptTokenCount() {
        return promptTokenCount;
    }

    /**
     * 생성된 응답 토큰 수를 반환합니다.
     */
    public int getCompletionTokenCount() {
        return completionTokenCount;
    }

    /**
     * 전체 토큰 수를 반환합니다.
     */
    public int getTotalTokenCount() {
        return promptTokenCount
                + completionTokenCount;
    }

    /**
     * LLM이 요청한 Tool Call 목록을 반환합니다.
     *
     * @return 수정할 수 없는 Tool Call 목록
     */
    public List<LlmToolCall> getToolCalls() {
        return toolCalls;
    }

    /**
     * Tool Call이 포함되어 있는지 확인합니다.
     */
    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    /**
     * Tool Call 개수를 반환합니다.
     */
    public int getToolCallCount() {
        return toolCalls.size();
    }

    /**
     * 첫 번째 Tool Call을 반환합니다.
     *
     * @return 첫 번째 Tool Call 또는 {@code null}
     */
    public LlmToolCall getFirstToolCall() {
        if (toolCalls.isEmpty()) {
            return null;
        }

        return toolCalls.get(0);
    }

    /**
     * 지정한 인덱스의 Tool Call을 반환합니다.
     */
    public LlmToolCall getToolCall(int index) {
        return toolCalls.get(index);
    }

    /**
     * 텍스트 응답이 존재하는지 확인합니다.
     */
    public boolean hasContent() {
        return content != null
                && !content.isBlank();
    }

    /**
     * 정상적인 최종 텍스트 응답인지 확인합니다.
     *
     * <p>Tool Call이 없고 텍스트 응답이 존재하면 최종 응답으로
     * 판단합니다.</p>
     */
    public boolean isFinalResponse() {
        return hasContent()
                && !hasToolCalls();
    }

    /**
     * Tool 호출을 요청하는 중간 응답인지 확인합니다.
     */
    public boolean isToolCallResponse() {
        return hasToolCalls();
    }

    private static List<LlmToolCall> immutableToolCalls(
            List<LlmToolCall> toolCalls) {

        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }

        List<LlmToolCall> copied =
                new ArrayList<>(toolCalls.size());

        for (LlmToolCall toolCall : toolCalls) {
            if (toolCall == null) {
                throw new IllegalArgumentException(
                        "toolCalls must not contain null"
                );
            }

            copied.add(toolCall);
        }

        return Collections.unmodifiableList(copied);
    }

    private static int validateTokenCount(
            int value,
            String name) {

        if (value < 0) {
            throw new IllegalArgumentException(
                    name + " must not be negative"
            );
        }

        return value;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Override
    public String toString() {
        return "LlmResponse{"
                + "model='" + model + '\''
                + ", contentLength="
                + (content == null ? 0 : content.length())
                + ", done=" + done
                + ", doneReason='" + doneReason + '\''
                + ", promptTokenCount=" + promptTokenCount
                + ", completionTokenCount="
                + completionTokenCount
                + ", toolCallCount=" + toolCalls.size()
                + '}';
    }
}
