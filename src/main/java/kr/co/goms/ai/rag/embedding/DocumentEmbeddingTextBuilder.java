/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.embedding;

import java.util.List;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.model.DocumentChunkType;

public class DocumentEmbeddingTextBuilder {

    public String build(List<DocumentChunk> chunks, int index) {

        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("chunks must not be empty.");
        }

        if (index < 0 || index >= chunks.size()) {
            throw new IllegalArgumentException("index is out of range: " + index);
        }

        DocumentChunk current = chunks.get(index);

        if (current == null) {
            throw new IllegalArgumentException("Current chunk must not be null.");
        }

        if (current.getType() != DocumentChunkType.PARAGRAPH) {
            return current.toEmbeddingText();
        }

        DocumentChunk previous = findPreviousParagraph(chunks, index);

        return buildParagraphEmbeddingText(previous, current);
    }

    private String buildParagraphEmbeddingText(DocumentChunk previous, DocumentChunk current) {

        StringBuilder builder = new StringBuilder();

        if (previous != null && previous.getContent() != null && !previous.getContent().isBlank()) {
            builder.append(previous.getContent().trim()).append('\n');
        }

        if (current.getContent() != null && !current.getContent().isBlank()) {
            builder.append(current.getContent().trim());
        }

        return builder.toString().trim();
    }

    private DocumentChunk findPreviousParagraph(List<DocumentChunk> chunks, int index) {

        for (int i = index - 1; i >= 0; i--) {

            DocumentChunk candidate = chunks.get(i);

            if (candidate == null) {
                continue;
            }

            if (candidate.getType() == DocumentChunkType.PARAGRAPH) {
                return candidate;
            }
        }

        return null;
    }

    private void append(StringBuilder builder, String label, String value) {

        if (value == null || value.isBlank()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append('\n');
        }

        builder.append(label).append(": ").append(value.trim());
    }
}