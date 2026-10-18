/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.embedding;

import java.util.Objects;

/**
 * 고정된 임베딩 모델명을 제공하는 기본 구현체입니다.
 */
public final class DefaultEmbeddingModelProvider
    implements EmbeddingModelProvider {

    private final String model;

    public DefaultEmbeddingModelProvider(String model) {
        this.model = requireText(model, "model");
    }

    @Override
    public String getModel() {
        return model;
    }

    private static String requireText(
        String value,
        String fieldName
    ) {
        String normalized =
            Objects.requireNonNullElse(value, "").trim();

        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank"
            );
        }

        return normalized;
    }
}