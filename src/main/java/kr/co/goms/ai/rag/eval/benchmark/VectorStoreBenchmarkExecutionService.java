/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.benchmark;

import java.io.IOException;

import kr.co.goms.ai.rag.embedding.EmbeddingException;
import kr.co.goms.ai.rag.vector.VectorStoreException;

/**
 * 현재 프로젝트를 기준으로 Vector Store Benchmark를 실행합니다.
 */
public interface VectorStoreBenchmarkExecutionService {

    VectorStoreBenchmarkExecutionResult execute()
            throws IOException, VectorStoreException, EmbeddingException;
}