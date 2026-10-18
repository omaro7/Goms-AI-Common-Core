/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.metric;

import java.util.List;

import kr.co.goms.ai.rag.eval.RagEvaluationContext;
import kr.co.goms.ai.rag.eval.RagMetricResult;
import kr.co.goms.ai.rag.eval.judge.RagJudge;
import kr.co.goms.ai.rag.eval.judge.RagJudgeResult;

/**
 * 검색된 Context의 정밀도를 평가한다.
 *
 * 질문과 기준 답변에 실제로 도움이 되는 Context가
 * 검색 결과에 얼마나 잘 포함되어 있는지 평가한다.
 */
public final class ContextPrecisionMetric implements RagMetric {

    public static final String NAME = "context_precision";

    private final RagJudge judge;

    public ContextPrecisionMetric(RagJudge judge) {
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

        if (context.isNoAnswerExpected()) {
            return RagMetricResult.notApplicable(
                    NAME,
                    "Context Precision is not applicable to NO_ANSWER cases.");
        }

        if (context.getRetrievedContexts().isEmpty()) {
            return new RagMetricResult(
                    NAME,
                    0.0,
                    "No retrieved contexts are available.");
        }

        String systemPrompt = buildSystemPrompt();
        String evaluationPrompt = buildEvaluationPrompt(context);

        RagJudgeResult judgeResult = judge.judge(systemPrompt, evaluationPrompt);

        return new RagMetricResult(
                NAME,
                judgeResult.getScore(),
                judgeResult.getReason());
    }

    private String buildSystemPrompt() {
        return "You are a RAG context precision evaluator.\n\n"
                + "Evaluate how relevant and useful the retrieved contexts are "
                + "for answering the user's question correctly.\n\n"
                + "Use the reference answer as the expected answer.\n\n"
                + "Contexts that directly support the expected answer "
                + "should be considered relevant.\n\n"
                + "Contexts that are unrelated, redundant, or do not help "
                + "answer the question should reduce the score.\n\n"
                + "Higher-ranked contexts should ideally contain "
                + "the most relevant information.\n\n"
                + "A score of 1.0 means the retrieved contexts are highly "
                + "relevant and focused on the information needed.\n\n"
                + "A score of 0.0 means the retrieved contexts are not useful "
                + "for answering the question.\n\n"
                + "Return a score between 0.0 and 1.0.";
    }

    private String buildEvaluationPrompt(RagEvaluationContext context) {
        StringBuilder builder = new StringBuilder();

        builder.append("QUESTION:\n");
        builder.append(context.getQuestion());

        builder.append("\n\n");

        builder.append("REFERENCE ANSWER:\n");
        builder.append(context.getReferenceAnswer());

        builder.append("\n\n");

        builder.append("RETRIEVED CONTEXTS:\n");
        appendContexts(builder, context.getRetrievedContexts());

        builder.append("\n");
        builder.append(
                "Evaluate the precision of the retrieved contexts.");

        return builder.toString();
    }

    private void appendContexts(StringBuilder builder, List<String> contexts) {
        for (int i = 0; i < contexts.size(); i++) {
            builder.append("[Context ");
            builder.append(i + 1);
            builder.append("]\n");

            builder.append(contexts.get(i));
            builder.append("\n\n");
        }
    }
}