/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.json;

import java.util.List;
import java.util.Map;

/**
 * Java 객체와 JSON 문자열 사이의 변환을 담당하는 공통 인터페이스입니다.
 *
 * <p>
 * GomsBook AI Agent의 Core 계층은 Jackson, Gson 등 특정 JSON
 * 라이브러리에 직접 의존하지 않고 이 인터페이스를 사용합니다.
 * </p>
 *
 * <p>
 * 실제 JSON 라이브러리 연동은 다음과 같은 구현체가 담당합니다.
 * </p>
 *
 * <pre>
 * JsonMapper
 * ├── JacksonJsonMapper
 * ├── GsonJsonMapper
 * └── SimpleJsonMapper
 * </pre>
 * 
 * Map<String, Object> payload = ollamaRequest.toPayload();

	String requestJson = jsonMapper.toJson(payload);
   
        OllamaChatResponse response =
        jsonMapper.fromJson(
                responseBody,
                OllamaChatResponse.class
        );
        
 */
public interface JsonMapper {

    /**
     * Java 객체를 JSON 문자열로 직렬화합니다.
     *
     * @param value 직렬화할 객체
     * @return JSON 문자열
     * @throws JsonMappingException 직렬화에 실패한 경우
     */
    String toJson(Object value);

    /**
     * Java 객체를 보기 좋은 형식의 JSON 문자열로 직렬화합니다.
     *
     * <p>
     * 로그, 디버깅, 사용자 Preview에 사용할 수 있습니다.
     * HTTP API 요청에는 일반적으로 {@link #toJson(Object)}을 사용합니다.
     * </p>
     *
     * @param value 직렬화할 객체
     * @return 들여쓰기된 JSON 문자열
     * @throws JsonMappingException 직렬화에 실패한 경우
     */
    String toPrettyJson(Object value);

    /**
     * JSON 문자열을 지정한 Java 타입으로 역직렬화합니다.
     *
     * @param json JSON 문자열
     * @param targetType 대상 타입
     * @param <T> 반환 타입
     * @return 역직렬화된 객체
     * @throws JsonMappingException 역직렬화에 실패한 경우
     */
    <T> T fromJson(
            String json,
            Class<T> targetType
    );

    /**
     * JSON 문자열을 문자열 Key 기반 Map으로 변환합니다.
     *
     * @param json JSON 문자열
     * @return JSON 객체를 나타내는 Map
     * @throws JsonMappingException 변환에 실패한 경우
     */
    Map<String, Object> toMap(String json);

    /**
     * JSON 배열 문자열을 Map 목록으로 변환합니다.
     *
     * @param json JSON 배열 문자열
     * @return JSON 객체 목록
     * @throws JsonMappingException 변환에 실패한 경우
     */
    List<Map<String, Object>> toMapList(String json);

    /**
     * Java 객체를 지정한 Java 타입으로 변환합니다.
     *
     * <p>
     * 다음과 같은 변환에 사용할 수 있습니다.
     * </p>
     *
     * <pre>
     * Map → OllamaChatResponse
     * Map → LlmToolCall
     * DTO → Map
     * </pre>
     *
     * @param value 변환할 값
     * @param targetType 대상 타입
     * @param <T> 반환 타입
     * @return 변환된 객체
     * @throws JsonMappingException 변환에 실패한 경우
     */
    <T> T convert(
            Object value,
            Class<T> targetType
    );

    /**
     * 문자열이 유효한 JSON인지 확인합니다.
     *
     * @param json 검사할 문자열
     * @return 유효한 JSON이면 true
     */
    boolean isValidJson(String json);

    /**
     * 문자열이 JSON 객체인지 확인합니다.
     *
     * @param json 검사할 문자열
     * @return JSON 객체이면 true
     */
    default boolean isJsonObject(
            String json
    ) {
        if (json == null) {
            return false;
        }

        String normalized = json.trim();

        return normalized.startsWith("{")
                && normalized.endsWith("}")
                && isValidJson(normalized);
    }

    /**
     * 문자열이 JSON 배열인지 확인합니다.
     *
     * @param json 검사할 문자열
     * @return JSON 배열이면 true
     */
    default boolean isJsonArray(
            String json
    ) {
        if (json == null) {
            return false;
        }

        String normalized = json.trim();

        return normalized.startsWith("[")
                && normalized.endsWith("]")
                && isValidJson(normalized);
    }

    /**
     * JSON 문자열이 null이거나 비어 있지 않은지 검사합니다.
     *
     * @param json 검사할 JSON 문자열
     * @throws JsonMappingException JSON 문자열이 비어 있는 경우
     */
    default void requireJson(
            String json
    ) {
        if (json == null || json.isBlank()) {
            throw new JsonMappingException(
                    "JSON content must not be blank."
            );
        }
    }

    /**
     * 대상 타입이 null이 아닌지 검사합니다.
     *
     * @param targetType 대상 타입
     * @throws JsonMappingException 대상 타입이 null인 경우
     */
    default void requireTargetType(
            Class<?> targetType
    ) {
        if (targetType == null) {
            throw new JsonMappingException(
                    "targetType must not be null."
            );
        }
    }
}