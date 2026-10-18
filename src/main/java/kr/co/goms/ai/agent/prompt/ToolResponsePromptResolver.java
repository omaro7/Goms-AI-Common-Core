/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.agent.prompt;

/**
 * Tool 실행 후 추가로 적용할 Prompt를 조회합니다.
 */
public interface ToolResponsePromptResolver {

    /**
     * Tool 이름에 해당하는 추가 Prompt를 반환합니다.
     *
     * @param toolName Tool 이름
     * @return 추가 Prompt, 없으면 null
     */
    String resolve(String toolName);
}