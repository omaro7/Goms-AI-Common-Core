/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.judge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kr.co.goms.ai.llm.LlmClient;
import kr.co.goms.ai.llm.LlmRequest;
import kr.co.goms.ai.llm.LlmResponse;

/**
 * GomsBook LlmClient를 이용하여 RAG 평가를 수행한다.
 * 
 * RagJudge judge = new LlmRagJudge(llmClient, "gemma4:31b-cloud");
 * 
 * 왜 temperature = 0.0인가? RAGAS 계열 평가에서는 동일 입력의 평가 점수가 매 실행마다 크게 변하면 regression test가 불안정해집니다.
 * temperature = 0.0으로 두는 것이 적절합니다.
 */
public final class LlmRagJudge implements RagJudge {

    private static final double DEFAULT_TEMPERATURE = 0.0;
    private static final int DEFAULT_MAX_TOKENS = 1024;

    private final LlmClient llmClient;
    private final String model;

    public LlmRagJudge(LlmClient llmClient, String model) {
        if (llmClient == null) {
            throw new NullPointerException("llmClient must not be null");
        }

        this.llmClient = llmClient;
        this.model = normalizeModel(model);
    }

    @Override
    public RagJudgeResult judge(String systemPrompt, String evaluationPrompt) {
        String normalizedSystemPrompt = requireText(systemPrompt, "systemPrompt");
        String normalizedEvaluationPrompt = requireText(evaluationPrompt, "evaluationPrompt");

        LlmRequest request = createRequest(normalizedSystemPrompt, normalizedEvaluationPrompt);

        llmClient.requireAvailable();

        LlmResponse response = llmClient.chat(request);

        if (response == null) {
            throw new IllegalStateException("LLM judge returned null response");
        }

        if (!response.hasContent()) {
            throw new IllegalStateException("LLM judge returned empty response");
        }

        return parseResult(response.getContent());
    }

    private LlmRequest createRequest(String systemPrompt, String evaluationPrompt) {
        return LlmRequest.builder()
                .model(model)
                .systemMessage(buildSystemPrompt(systemPrompt))
                .userMessage(evaluationPrompt)
                .temperature(DEFAULT_TEMPERATURE)
                .maxTokens(DEFAULT_MAX_TOKENS)
                .stream(false)
                .build();
    }

    private String buildSystemPrompt(String systemPrompt) {
        return systemPrompt
                + "\n\n"
                + "Return only valid JSON in the following format:\n"
                + "{\n"
                + "  \"score\": 0.0,\n"
                + "  \"reason\": \"evaluation reason\"\n"
                + "}\n\n"
                + "The score must be a number between 0.0 and 1.0."
                + "\nDo not include markdown code fences or additional text.";
    }

    private RagJudgeResult parseResult(String response) {
        String normalizedResponse = requireText(response, "response");

        try {
            String jsonText = extractJson(normalizedResponse);
            JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();

            if (!json.has("score") || json.get("score").isJsonNull()) {
                throw new IllegalArgumentException("Judge response does not contain score");
            }

            double score = json.get("score").getAsDouble();
            String reason = "";

            if (json.has("reason") && !json.get("reason").isJsonNull()) {
                reason = json.get("reason").getAsString();
            }

            return new RagJudgeResult(score, reason, normalizedResponse);

        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Failed to parse RAG judge response: " + normalizedResponse,
                    exception);
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start < 0 || end < 0 || end <= start) {
            throw new IllegalArgumentException(
                    "Judge response does not contain JSON object");
        }

        return response.substring(start, end + 1);
    }

    private static String normalizeModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return null;
        }

        return model.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null) {
            throw new NullPointerException(fieldName + " must not be null");
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return normalized;
    }
}