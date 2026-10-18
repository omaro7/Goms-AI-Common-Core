/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * SHA-256 HashService 기본 구현입니다.
 */
public final class Sha256HashService
    implements HashService {

    public static final String
        ALGORITHM = "SHA-256";

    /**
     * MessageDigest는 Thread Safe가 아니므로
     * ThreadLocal로 재사용합니다.
     */
    private static final ThreadLocal<MessageDigest>
        DIGEST =
        ThreadLocal.withInitial(
            Sha256HashService::createDigest
        );

    private final HashFormat format;

    /**
     * 기본 생성자
     *
     * HEX_LOWERCASE 사용
     */
    public Sha256HashService() {
        this(HashFormat.HEX_LOWERCASE);
    }

    public Sha256HashService(
        HashFormat format
    ) {
        this.format =
            format == null
                ? HashFormat.HEX_LOWERCASE
                : format;
    }

    @Override
    public String hash(
        String value
    ) throws HashException {

        if (value == null) {
            throw new IllegalArgumentException(
                "value must not be null"
            );
        }

        return hash(
            value.getBytes(DEFAULT_CHARSET)
        );
    }

    @Override
    public String hash(
        byte[] value
    ) throws HashException {

        if (value == null) {
            throw new IllegalArgumentException(
                "value must not be null"
            );
        }

        try {

            MessageDigest digest =
                DIGEST.get();

            digest.reset();

            byte[] hashed =
                digest.digest(value);

            return encode(hashed);

        } catch (RuntimeException ex) {

            throw new HashException(
                "Failed to create SHA-256 hash.",
                ALGORITHM,
                HashOperation.HASH_BYTES,
                ex
            );
        }
    }

    /**
     * byte[] → String
     */
    private String encode(
        byte[] bytes
    ) {

        switch (format) {

            case BASE64:
                return Base64
                    .getEncoder()
                    .encodeToString(bytes);

            case HEX_UPPERCASE:
                return toHex(
                    bytes,
                    true
                );

            case HEX_LOWERCASE:
            default:
                return toHex(
                    bytes,
                    false
                );
        }
    }

    /**
     * byte[] → Hex
     */
    private static String toHex(
        byte[] bytes,
        boolean upperCase
    ) {

        char[] digits =
            upperCase
                ? UPPER
                : LOWER;

        char[] result =
            new char[
                bytes.length * 2
            ];

        int i = 0;

        for (byte b : bytes) {

            int value =
                b & 0xFF;

            result[i++] =
                digits[
                    value >>> 4
                ];

            result[i++] =
                digits[
                    value & 0x0F
                ];
        }

        return new String(result);
    }

    @Override
    public String getAlgorithm() {
        return ALGORITHM;
    }

    @Override
    public HashFormat getFormat() {
        return format;
    }

    /**
     * MessageDigest 생성
     */
    private static MessageDigest
        createDigest() {

        try {

            return MessageDigest.getInstance(
                ALGORITHM
            );

        } catch (
            NoSuchAlgorithmException ex
        ) {

            throw new IllegalStateException(
                "SHA-256 algorithm unavailable.",
                ex
            );
        }
    }

    private static final char[] LOWER =
        "0123456789abcdef"
            .toCharArray();

    private static final char[] UPPER =
        "0123456789ABCDEF"
            .toCharArray();

    @Override
    public String toString() {

        return "Sha256HashService{" +
            "algorithm='" + ALGORITHM + '\'' +
            ", format=" + format +
            '}';
    }
}