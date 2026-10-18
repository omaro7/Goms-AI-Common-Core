/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.model;

/**
 * Agent 및 일반 대화에 사용할 LLM 모델명을 제공합니다.
 */
public interface ChatModelProvider {

    /**
     * 현재 사용할 Chat/Agent 모델명을 반환합니다.
     *
     * @return 모델명
     */
    String getModel();
}