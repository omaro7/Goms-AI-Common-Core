/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.agent.event.payload;

public final class RagContextPayload {

    private final String title;

    private final String text;

    private final String sourcePath;

    private final Double score;

    public RagContextPayload( String title, String text, String sourcePath, Double score) {

        this.title = title;
        this.text = text;
        this.sourcePath = sourcePath;
        this.score = score;
    }

    public String getTitle() {

        return title;
    }

    public String getText() {

        return text;
    }

    public String getSourcePath() {

        return sourcePath;
    }

    public Double getScore() {

        return score;
    }
}