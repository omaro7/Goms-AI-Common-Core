/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.metric;

import kr.co.goms.ai.rag.eval.RagEvaluationContext;
import kr.co.goms.ai.rag.eval.RagMetricResult;
import kr.co.goms.ai.rag.eval.judge.RagJudge;
import kr.co.goms.ai.rag.eval.judge.RagJudgeResult;

/**
 * 문서에 정답이 없는 질문에 대해
 * LLM이 적절하게 No-Answer 응답을 생성했는지 평가한다.
 */
public final class NoAnswerDetectionMetric implements RagMetric {

    public static final String NAME = "no_answer_detection";

    private final RagJudge judge;

    public NoAnswerDetectionMetric(RagJudge judge) {
        if (judge == null) {
            throw new NullPointerException("judge must not be null");
        }

        this.judge = judge;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public RagMetricResult evaluate(RagEvaluationContext context) {
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }

        if (!context.isNoAnswerExpected()) {
            return RagMetricResult.notApplicable(
                    NAME,
                    "No-Answer Detection is applicable only to NO_ANSWER cases.");
        }

        String systemPrompt =
                "You are a RAG no-answer detection evaluator.\n\n"
                + "Evaluate whether the generated answer correctly recognizes "
                + "that the requested information is not available in the project documents.\n\n"
                + "The answer must not invent or provide unsupported information.\n\n"
                + "A score of 1.0 means the answer clearly states that the information "
                + "cannot be found in the provided project context.\n\n"
                + "A score of 0.0 means the answer hallucinates or incorrectly claims "
                + "that the information exists.";

        String evaluationPrompt =
                "QUESTION:\n"
                + context.getQuestion()
                + "\n\nANSWER:\n"
                + context.getAnswer()
                + "\n\nREFERENCE:\n"
                + context.getReferenceAnswer();

        RagJudgeResult judgeResult = judge.judge(systemPrompt, evaluationPrompt);

        return new RagMetricResult(
                NAME,
                judgeResult.getScore(),
                judgeResult.getReason());
    }
}