/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.constant;

public final class RagConstant {

    private RagConstant() {
    }
    
    public static final String DEFAULT_EMBEDDING_MODEL = "nomic-embed-text";

    public static final int DEFAULT_RAG_TOP_K = 5;

    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.0;

    public static final String XHTML_EXTENSION = ".xhtml";

    public static final String NAV_FILE_NAME = "nav.xhtml";

    public static final String OPF_EXTENSION = ".opf";
}