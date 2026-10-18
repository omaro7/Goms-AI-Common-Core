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
 * 생성된 답변이 검색된 Context에 얼마나 충실한지 평가한다.
 *
 * 답변에 포함된 주장들이 retrieved context에 의해
 * 뒷받침될수록 높은 점수를 부여한다.
 */
public final class FaithfulnessMetric implements RagMetric {

    public static final String NAME = "faithfulness";

    private final RagJudge judge;

    public FaithfulnessMetric(RagJudge judge) {
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

        if (context.getRetrievedContexts().isEmpty()) {
            return new RagMetricResult(
                    NAME,
                    0.0,
                    "No retrieved contexts are available.");
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
        return "You are a RAG faithfulness evaluator.\n\n"
                + "Evaluate whether the generated answer is supported by "
                + "the retrieved contexts.\n\n"
                + "Consider only information contained in the retrieved contexts.\n\n"
                + "Do not use external knowledge.\n\n"
                + "A score of 1.0 means every factual claim in the answer "
                + "is fully supported by the contexts.\n\n"
                + "A score of 0.0 means none of the factual claims are supported.\n\n"
                + "Return a score between 0.0 and 1.0.";
    }

    private String buildEvaluationPrompt(RagEvaluationContext context) {
        StringBuilder builder = new StringBuilder();

        builder.append("QUESTION:\n");
        builder.append(context.getQuestion());
        builder.append("\n\n");

        builder.append("RETRIEVED CONTEXTS:\n");
        appendContexts(builder, context.getRetrievedContexts());

        builder.append("\nANSWER:\n");
        builder.append(context.getAnswer());

        builder.append("\n\n");
        builder.append("Evaluate the faithfulness of the answer.");

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