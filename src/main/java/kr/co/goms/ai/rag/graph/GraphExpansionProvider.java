/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.graph;

import java.util.Map;
import java.util.Set;

/**
 * Vector 검색 결과를 Seed로 관련 Graph 문서를 확장한다.
 */
public interface GraphExpansionProvider {

	Map<String, Double> expand(String projectId, String query, Set<String> seedSourcePaths);

	boolean isAvailable();
}