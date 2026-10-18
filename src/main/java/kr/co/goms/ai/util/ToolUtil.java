package kr.co.goms.ai.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kr.co.goms.ai.tool.ToolContext;
import kr.co.goms.ai.tool.ToolResult;
import kr.co.goms.ai.tool.ToolStatus;


public final class ToolUtil {

    private static final String XHTML_EXTENSION = ".xhtml";
    
    public static ToolResult validationFailed(String requestId, String toolName, String message) {
        return ToolResult.builder()
                .requestId(requestId)
                .toolName(toolName)
                .status(ToolStatus.VALIDATION_FAILED)
                .message(message)
                .errorMessage(message)
                .build();
    }

    public static ToolResult failed(String requestId, String toolName, String errorCode, String message, Throwable throwable) {
        return ToolResult.builder()
                .requestId(requestId)
                .toolName(toolName)
                .status(ToolStatus.FAILED)
                .message(message)
                .errorCode(errorCode)
                .errorMessage(message)
                .cause(throwable)
                .build();
    }
    
    public static String safeMessage(Throwable throwable) {
        if (throwable == null) return "Unknown tool error.";
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message.trim();
    }    
    
    
	/**
	 * AS-IS : deepCopyValue(value)
	 * TO-BE : ToolUtil.deepCopy(value)
	 */
    public static Object deepCopy(Object value) {

        if (value == null) return null;

        if (value instanceof Path path) return path.normalize();

        if (value instanceof Map<?, ?> map) return copyMap(map);

        if (value instanceof Iterable<?> iterable) return copyIterable(iterable);

        return value;
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {

        Map<String, Object> copied = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : source.entrySet()) {

            if (entry.getKey() == null) throw new IllegalArgumentException("Tool value Map must not contain null keys");

            copied.put(String.valueOf(entry.getKey()), deepCopy(entry.getValue()));
        }

        return Collections.unmodifiableMap(copied);
    }

    private static List<Object> copyIterable(Iterable<?> source) {

        List<Object> copied = new ArrayList<>();

        for (Object item : source) copied.add(deepCopy(item));

        return Collections.unmodifiableList(copied);
    }
    
    public static Path validateProjectRoot(Path value) {

        Path projectRoot = value.toAbsolutePath().normalize();

        if (!Files.exists(projectRoot)) throw new IllegalArgumentException("프로젝트 루트가 존재하지 않습니다: " + projectRoot);
        if (!Files.isDirectory(projectRoot)) throw new IllegalArgumentException("프로젝트 루트가 디렉터리가 아닙니다: " + projectRoot);

        return projectRoot;
    }
    
    public static String getContextString( ToolContext context, String key) {

        if (context == null || key == null || key.isBlank()) {

            return null;
        }

        String value =
                context.getAttribute(
                        key,
                        String.class);

        return trimToNull(value);
    }

    public static boolean hasXhtmlExtension(String fileName) {

        return fileName != null
                && fileName.toLowerCase(Locale.ROOT)
                        .endsWith(XHTML_EXTENSION);
    }

    public static String normalizeSeparator(String path) {

        return path.replace('\\', '/');
    }

    public static int lastIndexOfIgnoreCase( String source, String target) {

        if (source == null || target == null) {
            return -1;
        }

        return source.toLowerCase(Locale.ROOT)
                .lastIndexOf(
                        target.toLowerCase(Locale.ROOT));
    }
    
    
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    public static String trimToNull(String value) {

        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }

    public static String defaultIfBlank(
            String value,
            String defaultValue) {

        return isBlank(value)
                ? defaultValue
                : value.trim();
    }
   
    public static boolean containsHtmlElement(String xhtml) {

        return containsIgnoreCase(
                xhtml,
                "<html");
    }
    
    public static boolean containsIgnoreCase(
            String source,
            String target) {

        return indexOfIgnoreCase(
                source,
                target) >= 0;
    }

    public static int indexOfIgnoreCase(
            String source,
            String target) {

        if (source == null || target == null) {
            return -1;
        }

        return source.toLowerCase(Locale.ROOT)
                .indexOf(
                        target.toLowerCase(Locale.ROOT));
    }
    
    private ToolUtil() {}
}