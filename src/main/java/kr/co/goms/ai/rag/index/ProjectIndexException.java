/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

public class ProjectIndexException
        extends Exception {

    private static final long serialVersionUID =
            1L;


    public ProjectIndexException(
            String message) {

        super(
                message
        );
    }


    public ProjectIndexException(
            String message,
            Throwable cause) {

        super(
                message,
                cause
        );
    }
}