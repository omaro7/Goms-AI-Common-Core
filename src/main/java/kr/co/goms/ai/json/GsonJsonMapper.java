/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.json;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Gson 기반 {@link JsonMapper} 구현체입니다.
 */
public final class GsonJsonMapper
        implements JsonMapper {

    private static final Type MAP_TYPE =
            new TypeToken<Map<String, Object>>() {
            }.getType();

    private static final Type MAP_LIST_TYPE =
            new TypeToken<List<Map<String, Object>>>() {
            }.getType();

    private final Gson gson;
    private final Gson prettyGson;

    /**
     * 공통 Gson 설정으로 Mapper를 생성합니다.
     */
    public GsonJsonMapper() {
        this(
                GsonFactory.create(),
                GsonFactory.createPretty()
        );
    }

    /**
     * 사용자 정의 Gson 인스턴스로 Mapper를 생성합니다.
     *
     * @param gson 일반 직렬화 Gson
     * @param prettyGson Pretty 출력 Gson
     */
    public GsonJsonMapper(
            Gson gson,
            Gson prettyGson
    ) {
        this.gson = Objects.requireNonNull(
                gson,
                "gson must not be null."
        );

        this.prettyGson = Objects.requireNonNull(
                prettyGson,
                "prettyGson must not be null."
        );
    }

    @Override
    public String toJson(
            Object value
    ) {
        try {
            return gson.toJson(value);
        } catch (RuntimeException exception) {
            throw new JsonMappingException(
                    "Failed to serialize object to JSON.",
                    exception
            );
        }
    }

    @Override
    public String toPrettyJson(
            Object value
    ) {
        try {
            return prettyGson.toJson(value);
        } catch (RuntimeException exception) {
            throw new JsonMappingException(
                    "Failed to serialize object to pretty JSON.",
                    exception
            );
        }
    }

    @Override
    public <T> T fromJson(
            String json,
            Class<T> targetType
    ) {
        requireJson(json);
        requireTargetType(targetType);

        try {
            return gson.fromJson(
                    json,
                    targetType
            );
        } catch (JsonSyntaxException exception) {
            throw new JsonMappingException(
                    "Failed to deserialize JSON to "
                            + targetType.getName()
                            + ".",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new JsonMappingException(
                    "Failed to deserialize JSON.",
                    exception
            );
        }
    }

    /**
     * Generic 타입 역직렬화가 필요한 경우 사용합니다.
     *
     * @param json JSON 문자열
     * @param targetType 대상 Type
     * @param <T> 반환 타입
     * @return 역직렬화 결과
     */
    public <T> T fromJson(
            String json,
            Type targetType
    ) {
        requireJson(json);

        if (targetType == null) {
            throw new JsonMappingException(
                    "targetType must not be null."
            );
        }

        try {
            return gson.fromJson(
                    json,
                    targetType
            );
        } catch (RuntimeException exception) {
            throw new JsonMappingException(
                    "Failed to deserialize JSON.",
                    exception
            );
        }
    }

    @Override
    public Map<String, Object> toMap(
            String json
    ) {
        requireJson(json);

        try {
            Map<String, Object> result =
                    gson.fromJson(
                            json,
                            MAP_TYPE
                    );

            return result == null
                    ? Map.of()
                    : Map.copyOf(result);

        } catch (RuntimeException exception) {
            throw new JsonMappingException(
                    "Failed to convert JSON to Map.",
                    exception
            );
        }
    }

    @Override
    public List<Map<String, Object>> toMapList(
            String json
    ) {
        requireJson(json);

        try {
            List<Map<String, Object>> result =
                    gson.fromJson(
                            json,
                            MAP_LIST_TYPE
                    );

            return result == null
                    ? List.of()
                    : List.copyOf(result);

        } catch (RuntimeException exception) {
            throw new JsonMappingException(
                    "Failed to convert JSON to Map list.",
                    exception
            );
        }
    }

    @Override
    public <T> T convert(
            Object value,
            Class<T> targetType
    ) {
        requireTargetType(targetType);

        if (value == null) {
            return null;
        }

        try {
            JsonElement jsonTree =
                    gson.toJsonTree(value);

            return gson.fromJson(
                    jsonTree,
                    targetType
            );

        } catch (RuntimeException exception) {
            throw new JsonMappingException(
                    "Failed to convert object to "
                            + targetType.getName()
                            + ".",
                    exception
            );
        }
    }

    @Override
    public boolean isValidJson(
            String json
    ) {
        if (json == null || json.isBlank()) {
            return false;
        }

        try {
            JsonElement element =
                    JsonParser.parseString(json);

            return element != null;
        } catch (JsonSyntaxException exception) {
            return false;
        }
    }

    /**
     * 내부 Gson 객체를 반환합니다.
     *
     * @return Gson
     */
    public Gson gson() {
        return gson;
    }

    /**
     * Pretty 출력용 Gson 객체를 반환합니다.
     *
     * @return Pretty Gson
     */
    public Gson prettyGson() {
        return prettyGson;
    }
}