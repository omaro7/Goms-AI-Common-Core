/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GomsStringUtil
{

	/**
	 * AI 로그 답변에 대해서 긴 본문을 로그에 전부 찍지 않도록 처리
	 * @param text
	 * @param maximumLength
	 * @return
	 */
    public static String abbreviate(String text, int maximumLength) {
        if (text == null) {
            return "";
        }

        String normalized =
                text
                        .replace('\r', ' ')
                        .replace('\n', ' ')
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        if (normalized.length() <= maximumLength) {
            return normalized;
        }

        return normalized.substring(
                0,
                maximumLength
        ) + "...";
    }
    
    /**
     * 문자열 정규화
     * @param value
     * @return
     */
    public static String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        java.util.Locale.ROOT
                );
    }
    
    /**
     * path 정규화
     * @param path
     * @return
     */
    public static String normalizePath(String path) {

        if (path == null) {
            return "";
        }

        return path
                .trim()
                .replace(
                        '\\',
                        '/'
                );
    }
    
    /**
     * 한국어 조사를 일부 제거하여 keyword matching 정확도를 높입니다.
     */
    public static String normalizeKeyword(String token) {
        if (token == null) {
            return "";
        }

        String keyword = token.trim();

        if (keyword.isEmpty()) {
            return "";
        }

        String[] suffixes = {
                "에서는",
                "에게는",
                "으로는",
                "이라는",
                "에서",
                "에게",
                "으로",
                "라고",
                "이라는",
                "은",
                "는",
                "이",
                "가",
                "을",
                "를",
                "의",
                "에",
                "와",
                "과",
                "도"
        };

        for (String suffix : suffixes) {
            if (keyword.length() > suffix.length() + 1
                    && keyword.endsWith(suffix)) {

                keyword = keyword.substring( 0, keyword.length() - suffix.length());

                break;
            }
        }

        return keyword.trim();
    }

    /**
     * 검색 품질에 도움이 되지 않는 일반 질문 표현을 제거합니다.
     */
    public static boolean isStopWord(String keyword) {
        return "어떤".equals(keyword)
                || "어떻게".equals(keyword)
                || "무엇".equals(keyword)
                || "무엇인가요".equals(keyword)
                || "인가요".equals(keyword)
                || "있나요".equals(keyword)
                || "하나요".equals(keyword)
                || "소개되나요".equals(keyword)
                || "소개".equals(keyword)
                || "저자".equals(keyword)
                || "예전".equals(keyword)
                || "책".equals(keyword);
    }
    
    /**
     * 질문에서 Rerank에 사용할 핵심 키워드를 추출합니다. 한글어 조사를 제거하여 단어 키워드 추출
     */
    public static List<String> extractKeywords(String query) {
        String normalized = normalizeForTokenize(query);

        if (normalized.isBlank()) {
            return List.of();
        }

        String[] tokens = normalized.split("\\s+");
        Set<String> keywords = new LinkedHashSet<>();

        for (String token : tokens) {
            String keyword = GomsStringUtil.normalizeKeyword(token);

            if (keyword.isBlank()) {
                continue;
            }

            if (keyword.length() < 2) {
                continue;
            }

            if (GomsStringUtil.isStopWord(keyword)) {
                continue;
            }

            keywords.add(keyword);
        }

        return List.copyOf(keywords);
    }

    public static boolean containsKeyword(List<String> keywords, String expected) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }

        for (String keyword : keywords) {
            if (expected.equals(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 『...』, 「...」 등 제목 표현 여부를 판단합니다.
     */
    public static boolean containsTitlePattern(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        return (content.contains("『") && content.contains("』"))
                || (content.contains("「") && content.contains("」"))
                || (content.contains("《") && content.contains("》"));
    }

    /**
     * Keyword 추출용 정규화.
     */
    public static String normalizeForTokenize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = GomsStringUtil.normalize(value);

        if (normalized == null || normalized.isBlank()) {
            return "";
        }

        return normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}·ㆍ『』「」《》“”‘’]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 문자열 비교용 정규화.
     *
     * 공백과 주요 문장부호를 제거하여
     * "덕수궁 돌담길"과 "덕수궁 돌담길은" 등의
     * 부분 비교를 쉽게 합니다.
     */
    public static String normalizeForMatch(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = GomsStringUtil.normalize(value);

        if (normalized == null || normalized.isBlank()) {
            return "";
        }

        return normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}·ㆍ『』「」《》“”‘’]", "")
                .replaceAll("\\s+", "")
                .trim();
    }
    
    /**
     * Heading의 번호를 제거하고 검색 비교용 문자열로 정규화합니다.
     */
    public static String normalizeHeading(String value) {
        String normalized = normalizeForMatch(value);

        if (normalized.isBlank()) {
            return "";
        }

        normalized = normalized.replaceFirst("^\\d+\\s*\\.\\s*", "");

        return normalized.trim();
    }

    public static String normalizeContent(String content) {

        if (content == null) {
            return "";
        }

        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        normalized = removeCodeFence(normalized);
        normalized = normalized.replaceAll("[\\t ]+\\n", "\n").replaceAll("\\n[\\t ]+", "\n").replaceAll("\\n{3,}", "\n\n").trim();

        return normalized;
    }


    private static String removeCodeFence(String content) {

        if (content == null || content.isBlank()) {
            return "";
        }

        String normalized = content.trim();

        if (!normalized.startsWith("```")) {
            return normalized;
        }

        int firstLineEnd = normalized.indexOf('\n');

        if (firstLineEnd < 0) {
            return normalized.replace("```", "").trim();
        }

        normalized = normalized.substring(firstLineEnd + 1);

        int lastFence = normalized.lastIndexOf( "```");

        if (lastFence >= 0) {
            normalized = normalized.substring( 0, lastFence);
        }

        return normalized.trim();
    }
    
    public static String safeMessage( Throwable throwable) {

        if (throwable == null) {
            return "Unknown error";
        }

        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable .getClass() .getSimpleName();
        }

        return message.trim();
    }
    
}
