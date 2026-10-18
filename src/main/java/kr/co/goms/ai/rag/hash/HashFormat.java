/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.hash;

/**
 * 해시 결과 출력 형식입니다.
 */
public enum HashFormat {

    /**
     * 소문자 16진수 문자열입니다.
     *
     * 예:
     * 9f86d081884c7d65...
     */
    HEX_LOWERCASE,

    /**
     * 대문자 16진수 문자열입니다.
     */
    HEX_UPPERCASE,

    /**
     * Base64 문자열입니다.
     */
    BASE64
}