package kr.co.goms.ai.json;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * GomsBook AI Agent에서 공통으로 사용하는 Gson Factory입니다.
 *
 * <p>
 * Gson 2.8.9 호환 버전입니다.
 * </p>
 */
public final class GsonFactory {

    private static final Gson GSON =
            createInternal(false);

    private static final Gson PRETTY_GSON =
            createInternal(true);

    private GsonFactory() {
    }

    /**
     * 일반 JSON 직렬화용 Gson을 반환합니다.
     *
     * @return 공통 Gson 인스턴스
     */
    public static Gson create() {
        return GSON;
    }

    /**
     * Pretty JSON 출력용 Gson을 반환합니다.
     *
     * @return Pretty Gson 인스턴스
     */
    public static Gson createPretty() {
        return PRETTY_GSON;
    }

    private static Gson createInternal(
            boolean prettyPrinting
    ) {
        GsonBuilder builder =
                new GsonBuilder()

                /*
                 * XHTML/XML 문자열의
                 * < > & = 문자가 Unicode escape로
                 * 변환되는 것을 방지합니다.
                 */
                .disableHtmlEscaping()

                /*
                 * Instant 지원
                 */
                .registerTypeAdapter(
                        Instant.class,
                        new InstantAdapter()
                )

                /*
                 * Duration 지원
                 */
                .registerTypeAdapter(
                        Duration.class,
                        new DurationAdapter()
                );

        if (prettyPrinting) {
            builder.setPrettyPrinting();
        }

        return builder.create();
    }

    /**
     * Instant를 ISO-8601 문자열로 처리합니다.
     */
    private static final class InstantAdapter
            implements JsonSerializer<Instant>,
                       JsonDeserializer<Instant> {

        @Override
        public JsonElement serialize(
                Instant source,
                Type typeOfSource,
                JsonSerializationContext context
        ) {
            if (source == null) {
                return null;
            }

            return new JsonPrimitive(
                    source.toString()
            );
        }

        @Override
        public Instant deserialize(
                JsonElement json,
                Type typeOfTarget,
                JsonDeserializationContext context
        ) throws JsonParseException {

            if (json == null
                    || json.isJsonNull()) {
                return null;
            }

            try {
                return Instant.parse(
                        json.getAsString()
                );

            } catch (RuntimeException exception) {

                throw new JsonParseException(
                        "Invalid Instant value: "
                                + json,
                        exception
                );
            }
        }
    }

    /**
     * Duration을 ISO-8601 문자열로 처리합니다.
     *
     * 예:
     *
     * PT30S
     * PT5M
     */
    private static final class DurationAdapter
            implements JsonSerializer<Duration>,
                       JsonDeserializer<Duration> {

        @Override
        public JsonElement serialize(
                Duration source,
                Type typeOfSource,
                JsonSerializationContext context
        ) {
            if (source == null) {
                return null;
            }

            return new JsonPrimitive(
                    source.toString()
            );
        }

        @Override
        public Duration deserialize(
                JsonElement json,
                Type typeOfTarget,
                JsonDeserializationContext context
        ) throws JsonParseException {

            if (json == null
                    || json.isJsonNull()) {
                return null;
            }

            try {
                return Duration.parse(
                        json.getAsString()
                );

            } catch (RuntimeException exception) {

                throw new JsonParseException(
                        "Invalid Duration value: "
                                + json,
                        exception
                );
            }
        }
    }
}