/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ollama Embedding API 요청 모델입니다.
 *
 * POST /api/embed
 *
 * {
 *   "model":"bge-m3",
 *   "input":[
 *      "text1",
 *      "text2"
 *   ],
 *   "truncate":true,
 *   "options":{}
 * }
 */

public final class OllamaEmbeddingRequest {

    private final String model;

    private final List<String> input;

    private final Boolean truncate;

    private OllamaEmbeddingRequest(
        Builder builder
    ) {
        this.model =
            requireText(
                builder.model,
                "model"
            );

        this.input =
            immutableInputs(
                builder.input
            );

        this.truncate =
            builder.truncate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getModel() {
        return model;
    }

    public List<String> getInput() {
        return input;
    }

    public Boolean getTruncate() {
        return truncate;
    }

    private static List<String> immutableInputs(
        List<String> values
    ) {
        if (values == null
            || values.isEmpty()) {

            throw new IllegalArgumentException(
                "input must not be empty"
            );
        }

        List<String> copy =
            new ArrayList<>();

        for (String value : values) {
            String normalized =
                value == null
                    ? ""
                    : value.trim();

            if (!normalized.isBlank()) {
                copy.add(normalized);
            }
        }

        if (copy.isEmpty()) {
            throw new IllegalArgumentException(
                "input must contain at least one non-blank value"
            );
        }

        return Collections.unmodifiableList(
            copy
        );
    }

    private static String requireText(
        String value,
        String fieldName
    ) {
        String normalized =
            value == null
                ? ""
                : value.trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName
                    + " must not be blank"
            );
        }

        return normalized;
    }

    public static final class Builder {

        private String model;

        private final List<String> input =
            new ArrayList<>();

        private Boolean truncate;

        private Builder() {
        }

        public Builder model(
            String model
        ) {
            this.model = model;
            return this;
        }

        public Builder input(
            String value
        ) {
            if (value != null
                && !value.isBlank()) {

                this.input.add(
                    value
                );
            }

            return this;
        }

        public Builder input(
            List<String> values
        ) {
            this.input.clear();

            if (values != null) {
                for (String value : values) {
                    input(value);
                }
            }

            return this;
        }

        public Builder truncate(
            Boolean truncate
        ) {
            this.truncate = truncate;
            return this;
        }

        public OllamaEmbeddingRequest build() {
            return new OllamaEmbeddingRequest(
                this
            );
        }
    }
}