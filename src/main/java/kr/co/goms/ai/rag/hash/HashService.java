/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.hash;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import kr.co.goms.ai.rag.model.DocumentChunk;

/**
 * RAG 증분 인덱싱에 사용하는 해시 생성 서비스입니다.
 *
 * <p>주요 사용 목적은 다음과 같습니다.</p>
 *
 * <ul>
 *     <li>DocumentSource 전체 내용 변경 감지</li>
 *     <li>DocumentChunk 임베딩 대상 텍스트 변경 감지</li>
 *     <li>기존 VectorRecord 재사용 여부 판단</li>
 * </ul>
 */
public interface HashService {

    /**
     * 기본 문자 인코딩입니다.
     */
    Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 문자열의 해시를 생성합니다.
     *
     * @param value 해시를 생성할 문자열
     * @return 16진수 해시 문자열
     * @throws HashException 해시 생성 실패 시
     */
    String hash(
        String value
    ) throws HashException;

    /**
     * 지정한 문자 인코딩으로 문자열의 해시를 생성합니다.
     *
     * @param value 해시를 생성할 문자열
     * @param charset 문자 인코딩
     * @return 16진수 해시 문자열
     * @throws HashException 해시 생성 실패 시
     */
    default String hash(
        String value,
        Charset charset
    ) throws HashException {

        if (value == null) {
            throw new IllegalArgumentException(
                "value must not be null"
            );
        }

        Charset resolvedCharset =
            Objects.requireNonNullElse(
                charset,
                DEFAULT_CHARSET
            );

        return hash(
            value.getBytes(resolvedCharset)
        );
    }

    /**
     * 바이트 배열의 해시를 생성합니다.
     *
     * @param value 해시를 생성할 바이트 배열
     * @return 16진수 해시 문자열
     * @throws HashException 해시 생성 실패 시
     */
    String hash(
        byte[] value
    ) throws HashException;

    /**
     * DocumentChunk의 실제 임베딩 입력 문자열을 기준으로
     * 콘텐츠 해시를 생성합니다.
     *
     * <p>본문만이 아니라 제목, Chunk 유형 및 EPUB 유형이 포함된
     * {@link DocumentChunk#toEmbeddingText()}를 사용합니다.</p>
     *
     * @param chunk 문서 Chunk
     * @return Chunk 콘텐츠 해시
     * @throws HashException 해시 생성 실패 시
     */
    default String hashChunk(
        DocumentChunk chunk
    ) throws HashException {

        Objects.requireNonNull(
            chunk,
            "chunk must not be null"
        );

        return hash(
            chunk.toEmbeddingText()
        );
    }

    /**
     * 두 문자열의 해시가 동일한지 비교합니다.
     *
     * <p>대소문자를 구분하지 않습니다.</p>
     *
     * @param left 첫 번째 해시
     * @param right 두 번째 해시
     * @return 동일 여부
     */
    default boolean matches(
        String left,
        String right
    ) {
        String normalizedLeft =
            normalizeHash(left);

        String normalizedRight =
            normalizeHash(right);

        return !normalizedLeft.isBlank()
            && normalizedLeft.equalsIgnoreCase(
                normalizedRight
            );
    }

    /**
     * 새 문자열이 기존 해시와 동일한지 확인합니다.
     *
     * @param value 현재 문자열
     * @param expectedHash 기존 해시
     * @return 해시 일치 여부
     * @throws HashException 해시 생성 실패 시
     */
    default boolean matchesValue(
        String value,
        String expectedHash
    ) throws HashException {

        if (expectedHash == null
            || expectedHash.isBlank()) {

            return false;
        }

        return matches(
            hash(value),
            expectedHash
        );
    }

    /**
     * DocumentChunk가 기존 콘텐츠 해시와 동일한지 확인합니다.
     *
     * @param chunk 현재 Chunk
     * @param expectedHash 기존 VectorRecord의 콘텐츠 해시
     * @return 변경되지 않았으면 true
     * @throws HashException 해시 생성 실패 시
     */
    default boolean matchesChunk(
        DocumentChunk chunk,
        String expectedHash
    ) throws HashException {

        if (expectedHash == null
            || expectedHash.isBlank()) {

            return false;
        }

        return matches(
            hashChunk(chunk),
            expectedHash
        );
    }

    /**
     * 현재 사용하는 해시 알고리즘명을 반환합니다.
     *
     * @return 해시 알고리즘명
     */
    String getAlgorithm();

    /**
     * 해시 문자열 형식을 반환합니다.
     *
     * @return 해시 출력 형식
     */
    default HashFormat getFormat() {
        return HashFormat.HEX_LOWERCASE;
    }

    /**
     * 해시 서비스 사용 가능 여부를 확인합니다.
     *
     * @return 사용 가능 여부
     */
    default boolean isAvailable() {
        try {
            return getAlgorithm() != null
                && !getAlgorithm().isBlank();

        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String normalizeHash(
        String value
    ) {
        return value == null
            ? ""
            : value.trim();
    }
}