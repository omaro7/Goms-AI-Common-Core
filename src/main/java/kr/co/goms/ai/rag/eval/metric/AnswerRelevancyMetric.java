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
 * 생성된 답변이 사용자 질문에 얼마나 적절하게 응답하는지 평가한다.
 *
 * 질문에 직접 답하고 핵심 내용을 충분히 포함할수록
 * 높은 점수를 부여한다.
 * 
 * 의도적으로 retrievedContexts와 referenceAnswer를 사용하지 않습니다.
 * 
 * 질문:
 * 덕수궁 돌담길은 어떤 장소인가요?
 * 답변 A:
 * 덕수궁 돌담길은 정동을 대표하는 산책길입니다.
 * → 높은 점수
 * 답변 B:
 * 서울에는 다양한 궁궐과 박물관이 있습니다.
 * → 낮은 점수
 * 
 * Metric				핵심 질문
 * ----------------------------------------------------
 * Faithfulness			답변이 Context에 근거했는가
 * Answer Relevancy		답변이 질문에 제대로 답했는가
 * Context Precision	검색된 Context들이 얼마나 관련 있는가
 * Context Recall		필요한 근거를 충분히 검색했는가
 */
public final class AnswerRelevancyMetric implements RagMetric {

    public static final String NAME = "answer_relevancy";

    private final RagJudge judge;

    public AnswerRelevancyMetric(RagJudge judge) {
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

        String systemPrompt = buildSystemPrompt();
        String evaluationPrompt = buildEvaluationPrompt(context);

        RagJudgeResult judgeResult =
                judge.judge(systemPrompt, evaluationPrompt);

        return new RagMetricResult(
                NAME,
                judgeResult.getScore(),
                judgeResult.getReason());
    }

    private String buildSystemPrompt() {
        return "You are a RAG answer relevancy evaluator.\n\n"
                + "Evaluate how relevant the generated answer is "
                + "to the user's question.\n\n"
                + "Consider whether the answer directly addresses "
                + "the question and provides the information requested.\n\n"
                + "Penalize answers that are unrelated, evasive, "
                + "incomplete, or contain excessive irrelevant information.\n\n"
                + "Do not evaluate factual correctness or faithfulness "
                + "to retrieved contexts in this metric.\n\n"
                + "A score of 1.0 means the answer directly and completely "
                + "addresses the question.\n\n"
                + "A score of 0.0 means the answer is unrelated "
                + "to the question.\n\n"
                + "Return a score between 0.0 and 1.0.";
    }

    private String buildEvaluationPrompt(RagEvaluationContext context) {
        StringBuilder builder = new StringBuilder();

        builder.append("QUESTION:\n");
        builder.append(context.getQuestion());

        builder.append("\n\n");

        builder.append("ANSWER:\n");
        builder.append(context.getAnswer());

        builder.append("\n\n");

        builder.append(
                "Evaluate how relevant the answer is to the question.");

        return builder.toString();
    }
}