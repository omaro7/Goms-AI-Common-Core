/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.retrieval;

/**
 * 사용자 검색 질의를 분석하여 {@link RagQueryIntent}를 결정합니다.
 *
 * <p>Golden Dataset Case ID 또는 특정 질문 전체 문자열에 의존하지 않고
 * 실제 서비스 질의에도 적용할 수 있는 일반적인 질의 표현을 기준으로
 * 검색 의도를 분류합니다.</p>
 */
public final class RagQueryIntentResolver {

    /**
     * 질의 의도를 분석합니다.
     *
     * @param query 사용자 검색 질의
     * @return 검색 질의 의도
     */
    public RagQueryIntent resolve(String query) {

        if (query == null || query.isBlank()) return RagQueryIntent.DEFAULT;

        String normalized = query.trim();

        if (isLocationQuery(normalized)) return RagQueryIntent.LOCATION;

        return RagQueryIntent.DEFAULT;
    }

    /**
     * 장소 또는 위치 식별 질의 여부를 반환합니다.
     */
    private boolean isLocationQuery(String query) {

        return query.contains("어디")
                || query.contains("어느 곳")
                || query.contains("어떤 곳")
                || query.contains("어느 장소")
                || query.contains("어떤 장소")
                || query.contains("장소는")
                || query.contains("장소가")
                || query.contains("곳은 어디")
                || query.contains("곳이 어디");
    }
}