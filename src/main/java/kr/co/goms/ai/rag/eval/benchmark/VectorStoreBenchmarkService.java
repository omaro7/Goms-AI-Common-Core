/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.benchmark;

import java.io.IOException;
import java.nio.file.Path;

import kr.co.goms.ai.rag.embedding.EmbeddingException;
import kr.co.goms.ai.rag.vector.VectorStoreException;

/**
 * Vector Store Benchmark 서비스입니다.
 */
public interface VectorStoreBenchmarkService {

    VectorStoreBenchmarkReport benchmark(Path datasetPath) throws IOException, VectorStoreException, EmbeddingException;
}