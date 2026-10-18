/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.judge;

/**
 * RAG Judge의 평가 결과.
 *
 * 평가 점수와 판정 사유, 필요할 경우
 * LLM이 반환한 원본 응답을 함께 보관한다.
 */
public final class RagJudgeResult {

    private final double score;
    private final String reason;
    private final String rawResponse;

    public RagJudgeResult(double score, String reason, String rawResponse) {
        this.score = validateScore(score);
        this.reason = normalize(reason);
        this.rawResponse = normalize(rawResponse);
    }

    public double getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    private static double validateScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            throw new IllegalArgumentException("score must be a finite number");
        }

        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(
                    "score must be between 0.0 and 1.0: " + score);
        }

        return score;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "RagJudgeResult{" +
                "score=" + score +
                ", reason='" + reason + '\'' +
                '}';
    }
}