/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.expansion;

import java.util.List;

import kr.co.goms.ai.rag.retrieval.RetrievedDocument;

public interface ContextExpander {

    List<RetrievedDocument> expand( ContextExpansionRequest request);

}