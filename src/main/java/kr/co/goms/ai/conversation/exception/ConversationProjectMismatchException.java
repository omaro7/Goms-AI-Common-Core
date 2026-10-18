/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.conversation.exception;

public final class ConversationProjectMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConversationProjectMismatchException(String message) {
        super(message);
    }
}