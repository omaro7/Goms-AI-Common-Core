/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * LLM 입력에 포함되는 첨부 파일입니다.
 *
 * <p>
 * 이미지, PDF, EPUB, DOCX, Markdown 등
 * 다양한 입력 데이터를 표현합니다.
 *
 * Provider(OpenAI, Ollama, Gemini 등)는
 * 이 객체를 Provider별 형식으로 변환합니다.
 * </p>
 * 
 * LlmAttachment image = LlmAttachment.image(Path.of("cover.png"));
 * LlmMessage message = LlmMessage
 * 							.user("이미지를 분석해주세요.")
 * 							.withAttachment(image);
 */
public record LlmAttachment(

        /**
         * 첨부파일 종류
         */
        LlmAttachmentType type,

        /**
         * 실제 파일 경로
         */
        Path file,

        /**
         * MIME Type
         *
         * image/png
         * application/pdf
         * ...
         */
        String mimeType,

        /**
         * 사용자 표시 이름
         */
        String fileName,

        /**
         * 추가 메타데이터
         */
        Map<String, Object> metadata

) implements Serializable {

    public LlmAttachment {

        Objects.requireNonNull(
                type,
                "type"
        );

        Objects.requireNonNull(
                file,
                "file"
        );

        file = file
                .toAbsolutePath()
                .normalize();

        mimeType = normalize(mimeType);

        fileName = normalize(fileName);

        metadata =
                metadata == null
                        ? Map.of()
                        : Map.copyOf(metadata);
    }

    /**
     * 이미지 Attachment 생성
     */
    public static LlmAttachment image(
            Path file
    ) {

        return new LlmAttachment(

                LlmAttachmentType.IMAGE,

                file,

                "image/png",

                file.getFileName().toString(),

                Map.of()
        );
    }

    /**
     * PDF Attachment 생성
     */
    public static LlmAttachment pdf(
            Path file
    ) {

        return new LlmAttachment(

                LlmAttachmentType.PDF,

                file,

                "application/pdf",

                file.getFileName().toString(),

                Map.of()
        );
    }

    /**
     * EPUB Attachment 생성
     */
    public static LlmAttachment epub(
            Path file
    ) {

        return new LlmAttachment(

                LlmAttachmentType.EPUB,

                file,

                "application/epub+zip",

                file.getFileName().toString(),

                Map.of()
        );
    }

    /**
     * Text Attachment 생성
     */
    public static LlmAttachment text(
            Path file
    ) {

        return new LlmAttachment(

                LlmAttachmentType.TEXT,

                file,

                "text/plain",

                file.getFileName().toString(),

                Map.of()
        );
    }

    /**
     * Markdown Attachment 생성
     */
    public static LlmAttachment markdown(
            Path file
    ) {

        return new LlmAttachment(

                LlmAttachmentType.MARKDOWN,

                file,

                "text/markdown",

                file.getFileName().toString(),

                Map.of()
        );
    }

    /**
     * 이미지 여부
     */
    public boolean isImage() {
        return type == LlmAttachmentType.IMAGE;
    }

    /**
     * PDF 여부
     */
    public boolean isPdf() {
        return type == LlmAttachmentType.PDF;
    }

    /**
     * EPUB 여부
     */
    public boolean isEpub() {
        return type == LlmAttachmentType.EPUB;
    }

    /**
     * 파일 존재 여부
     */
    public boolean exists() {
        return java.nio.file.Files.exists(file);
    }

    /**
     * 파일 크기(Byte)
     */
    public long size() {

        try {
            return java.nio.file.Files.size(file);
        } catch (Exception e) {
            return -1L;
        }

    }

    /**
     * Metadata 추가
     */
    public LlmAttachment withMetadata(
            String key,
            Object value
    ) {

        Map<String, Object> map =
                new java.util.LinkedHashMap<>(metadata);

        map.put(
                key,
                value
        );

        return new LlmAttachment(

                type,

                file,

                mimeType,

                fileName,

                map
        );

    }

    private static String normalize(
            String value
    ) {

        return value == null || value.isBlank()
                ? null
                : value.trim();

    }

}