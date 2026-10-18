/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.json;

/**
 * JSON 직렬화, 역직렬화 또는 객체 변환 과정에서 발생하는 공통 예외입니다.
 */
public class JsonMappingException
        extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 오류 메시지로 예외를 생성합니다.
     *
     * @param message 오류 메시지
     */
    public JsonMappingException(
            String message
    ) {
        super(message);
    }

    /**
     * 오류 메시지와 원인 예외로 생성합니다.
     *
     * @param message 오류 메시지
     * @param cause 원인 예외
     */
    public JsonMappingException(
            String message,
            Throwable cause
    ) {
        super(
                message,
                cause
        );
    }
}