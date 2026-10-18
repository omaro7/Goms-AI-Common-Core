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
 * 검색된 Context의 재현율을 평가한다.
 *
 * 기준 답변에 필요한 정보가 검색된 Context에 얼마나 충분히 포함되어 있는지 평가한다.
 * Reference Answer
    ├─ Claim 1 → Context에 있음(O)
    ├─ Claim 2 → Context에 있음(O)
    └─ Claim 3 → Context에 없음(X)
    Context Recall = 2 / 3 = 0.6667
 */
public final class ContextRecallMetric implements RagMetric {

    public static final String NAME = "context_recall";

    private final RagJudge judge;

    public ContextRecallMetric(RagJudge judge) {
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
                    "Context Recall is not applicable to NO_ANSWER cases.");
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
        return "You are a RAG context recall evaluator.\n\n"
                + "Evaluate whether the retrieved contexts contain all "
                + "information necessary to support the reference answer.\n\n"
                + "Use the reference answer as the expected ground truth.\n\n"
                + "Identify the important factual claims in the reference answer "
                + "and determine whether each claim is supported by at least one "
                + "retrieved context.\n\n"
                + "Do not use external knowledge.\n\n"
                + "A score of 1.0 means all important information in the "
                + "reference answer is covered by the retrieved contexts.\n\n"
                + "A score of 0.0 means none of the important information in the "
                + "reference answer is covered by the retrieved contexts.\n\n"
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
                "Evaluate how completely the retrieved contexts cover "
                + "the reference answer.");

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