/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.runner;

/**
 * 실제 RAG 실행 계층과 Evaluation Runner를 연결하기 위한 Adapter.
 */
public interface RagExecutionAdapter {
    void validateProject(String expectedProjectId);
    RagExecutionResult execute(String question);
}