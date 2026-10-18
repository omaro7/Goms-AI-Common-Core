/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

import java.util.List;
import com.google.gson.annotations.SerializedName;

/**
 * Ollama Chat API {@code /api/chat} 응답 객체입니다.
 *
 * <p>일반 텍스트 응답과 Tool Calling 응답을 모두 지원합니다.</p>
 *
 * <p>Tool Call 정보는 응답 최상위가 아니라
 * {@link OllamaChatMessage#getToolCalls()}에 포함됩니다.</p>
 */
public final class OllamaChatResponse {

    private String model;

    @SerializedName("created_at")
    private String createdAt;

    private OllamaChatMessage message;

    private boolean done;

    @SerializedName("done_reason")
    private String doneReason;

    @SerializedName("total_duration")
    private long totalDuration;

    @SerializedName("load_duration")
    private long loadDuration;

    @SerializedName("prompt_eval_count")
    private int promptEvalCount;

    @SerializedName("prompt_eval_duration")
    private long promptEvalDuration;

    @SerializedName("eval_count")
    private int evalCount;

    @SerializedName("eval_duration")
    private long evalDuration;

    /**
     * Gson 역직렬화를 위한 기본 생성자입니다.
     */
    public OllamaChatResponse() {
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = normalizeOptional(model);
    }

    /**
     * Ollama 응답 생성 시각을 반환합니다.
     *
     * <p>일반적으로 RFC 3339 형식의 문자열입니다.</p>
     */
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = normalizeOptional(createdAt);
    }

    /**
     * Assistant 응답 메시지를 반환합니다.
     */
    public OllamaChatMessage getMessage() {
        return message;
    }

    public void setMessage(OllamaChatMessage message) {
        this.message = message;
    }

    /**
     * 응답 생성 완료 여부를 반환합니다.
     */
    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    /**
     * 응답 종료 사유를 반환합니다.
     */
    public String getDoneReason() {
        return doneReason;
    }

    public void setDoneReason(String doneReason) {
        this.doneReason = normalizeOptional(doneReason);
    }

    /**
     * 전체 처리 시간을 나노초 단위로 반환합니다.
     */
    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = requireNonNegative(
                totalDuration,
                "totalDuration"
        );
    }

    /**
     * 모델 로딩 시간을 나노초 단위로 반환합니다.
     */
    public long getLoadDuration() {
        return loadDuration;
    }

    public void setLoadDuration(long loadDuration) {
        this.loadDuration = requireNonNegative(
                loadDuration,
                "loadDuration"
        );
    }

    /**
     * 입력 Prompt 토큰 수를 반환합니다.
     */
    public int getPromptEvalCount() {
        return promptEvalCount;
    }

    public void setPromptEvalCount(int promptEvalCount) {
        this.promptEvalCount = requireNonNegative(
                promptEvalCount,
                "promptEvalCount"
        );
    }

    /**
     * Prompt 평가 시간을 나노초 단위로 반환합니다.
     */
    public long getPromptEvalDuration() {
        return promptEvalDuration;
    }

    public void setPromptEvalDuration(
            long promptEvalDuration) {

        this.promptEvalDuration = requireNonNegative(
                promptEvalDuration,
                "promptEvalDuration"
        );
    }

    /**
     * 생성된 출력 토큰 수를 반환합니다.
     */
    public int getEvalCount() {
        return evalCount;
    }

    public void setEvalCount(int evalCount) {
        this.evalCount = requireNonNegative(
                evalCount,
                "evalCount"
        );
    }

    /**
     * 출력 생성 시간을 나노초 단위로 반환합니다.
     */
    public long getEvalDuration() {
        return evalDuration;
    }

    public void setEvalDuration(long evalDuration) {
        this.evalDuration = requireNonNegative(
                evalDuration,
                "evalDuration"
        );
    }

    /**
     * Assistant 응답 본문을 반환합니다.
     *
     * <p>Tool Calling 응답에서는 빈 문자열일 수 있습니다.</p>
     */
    public String getContent() {
        if (message == null) {
            return "";
        }

        return message.getContent();
    }

    /**
     * Assistant 응답에 텍스트 본문이 있는지 확인합니다.
     */
    public boolean hasContent() {
        return message != null
                && message.hasContent();
    }

    /**
     * Assistant 응답에 Tool Call이 포함되어 있는지 확인합니다.
     */
    public boolean hasToolCalls() {
        return message != null
                && message.hasToolCalls();
    }

    /**
     * Assistant 응답의 Tool Call 목록을 반환합니다.
     */
    public List<OllamaToolCall> getToolCalls() {
        if (message == null) {
            return List.of();
        }

        return message.getToolCalls();
    }

    /**
     * Tool Call 개수를 반환합니다.
     */
    public int getToolCallCount() {
        return message == null
                ? 0
                : message.getToolCallCount();
    }

    /**
     * 일반적인 최종 텍스트 응답인지 확인합니다.
     */
    public boolean isFinalTextResponse() {
        return done
                && hasContent()
                && !hasToolCalls();
    }

    /**
     * Tool 실행을 요청하는 응답인지 확인합니다.
     */
    public boolean isToolCallResponse() {
        return hasToolCalls();
    }

    /**
     * 전체 입력·출력 토큰 수를 반환합니다.
     */
    public int getTotalTokenCount() {
        return promptEvalCount + evalCount;
    }

    /**
     * 초당 생성 토큰 수를 반환합니다.
     *
     * @return 초당 출력 토큰 수. 계산할 수 없으면 {@code 0.0}
     */
    public double getTokensPerSecond() {
        if (evalCount <= 0 || evalDuration <= 0) {
            return 0.0;
        }

        double seconds =
                evalDuration / 1_000_000_000.0;

        return evalCount / seconds;
    }

    /**
     * 응답 객체의 최소 유효성을 검증합니다.
     */
    public void validate() {
        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                    "Ollama response model is missing"
            );
        }

        if (message == null) {
            throw new IllegalStateException(
                    "Ollama response message is missing"
            );
        }

        message.validate();

        if (!message.isAssistant()) {
            throw new IllegalStateException(
                    "Ollama response message role must be assistant"
            );
        }

        if (!hasContent() && !hasToolCalls()) {
            throw new IllegalStateException(
                    "Ollama response must contain "
                            + "content or tool calls"
            );
        }
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static int requireNonNegative(
            int value,
            String fieldName) {

        if (value < 0) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }

        return value;
    }

    private static long requireNonNegative(
            long value,
            String fieldName) {

        if (value < 0L) {
            throw new IllegalArgumentException(
                    fieldName + " must not be negative"
            );
        }

        return value;
    }

    @Override
    public String toString() {
        return "OllamaChatResponse{"
                + "model='" + model + '\''
                + ", createdAt='" + createdAt + '\''
                + ", done=" + done
                + ", doneReason='" + doneReason + '\''
                + ", contentLength="
                + (getContent() == null
                        ? 0
                        : getContent().length())
                + ", toolCallCount="
                + getToolCallCount()
                + ", promptEvalCount="
                + promptEvalCount
                + ", evalCount="
                + evalCount
                + ", totalDuration="
                + totalDuration
                + '}';
    }
}
