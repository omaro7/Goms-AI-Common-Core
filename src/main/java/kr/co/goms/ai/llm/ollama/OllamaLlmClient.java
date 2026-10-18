/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.llm.ollama;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.llm.LlmClient;
import kr.co.goms.ai.json.JsonMapper;
import kr.co.goms.ai.llm.LlmMessage;
import kr.co.goms.ai.llm.LlmRequest;
import kr.co.goms.ai.llm.LlmResponse;
import kr.co.goms.ai.llm.LlmRole;
import kr.co.goms.ai.llm.LlmToolCall;
import kr.co.goms.ai.llm.LlmToolCallFunction;
import kr.co.goms.ai.llm.LlmToolDefinition;


/**
 * Ollama Chat API를 사용하는 LLM 클라이언트입니다.
 *
 * <p>다음 기능을 지원합니다.</p>
 *
 * <ul>
 *     <li>일반 Chat 요청</li>
 *     <li>Tool 정의 전달</li>
 *     <li>Assistant Tool Call 응답 처리</li>
 *     <li>Tool 실행 결과 메시지 전달</li>
 *     <li>temperature 및 maxTokens 옵션 변환</li>
 *     <li>비스트리밍 응답</li>
 * </ul>
 */
public final class OllamaLlmClient implements LlmClient {

    private static final String CHAT_API_PATH = "/api/chat";

    private static final Duration DEFAULT_CONNECT_TIMEOUT =
            Duration.ofSeconds(10);

    private static final Duration DEFAULT_REQUEST_TIMEOUT =
            Duration.ofMinutes(5);

    private static final int MAX_ERROR_BODY_LENGTH = 2_000;

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final OllamaConfiguration configuration;

    /**
     * 기본 HttpClient를 사용하는 생성자입니다.
     */
    public OllamaLlmClient(
            OllamaConfiguration configuration,
            JsonMapper jsonMapper) {

        this(
                HttpClient.newBuilder()
                        .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                        .build(),
                configuration,
                jsonMapper
        );
    }

    /**
     * 외부에서 생성한 HttpClient를 사용하는 생성자입니다.
     */
    public OllamaLlmClient(
            HttpClient httpClient,
            OllamaConfiguration configuration,
            JsonMapper jsonMapper) {

        this.httpClient = Objects.requireNonNull(
                httpClient,
                "httpClient must not be null"
        );

        this.configuration = Objects.requireNonNull(
                configuration,
                "configuration must not be null"
        );

        this.jsonMapper = Objects.requireNonNull(
                jsonMapper,
                "jsonMapper must not be null"
        );
    }

    /**
     * LLM 요청을 Ollama Chat API로 전송합니다.
     */
    @Override
    public LlmResponse chat(LlmRequest request) {
        Objects.requireNonNull(
                request,
                "request must not be null"
        );

        validateRequest(request);

        OllamaChatRequest ollamaRequest =
                toOllamaRequest(request);

        String requestBody =
                serializeRequest(ollamaRequest);

        HttpRequest httpRequest =
                createHttpRequest(requestBody);

        try {
            HttpResponse<String> response =
                    httpClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            validateHttpResponse(response);

            OllamaChatResponse ollamaResponse =
                    deserializeResponse(
                            response.body()
                    );

            return toLlmResponse(ollamaResponse);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new OllamaLlmException(
                    "Ollama 요청 중 스레드가 중단되었습니다.",
                    exception
            );

        } catch (IOException exception) {
            throw new OllamaLlmException(
                    "Ollama 서버와 통신하지 못했습니다. uri="
                            + resolveChatUri(),
                    exception
            );
        }
    }

    /**
     * 단일 사용자 Prompt를 전송합니다.
     */
    public LlmResponse chat(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "prompt must not be blank"
            );
        }

        return chat(
                LlmRequest.builder()
                        .model(configuration.getModel())
                        .userMessage(prompt)
                        .stream(false)
                        .build()
        );
    }

    /**
     * 시스템 Prompt와 사용자 Prompt를 함께 전송합니다.
     */
    public LlmResponse chat(
            String systemPrompt,
            String userPrompt) {

        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException(
                    "userPrompt must not be blank"
            );
        }

        LlmRequest.Builder builder =
                LlmRequest.builder()
                        .model(configuration.getModel())
                        .stream(false);

        if (systemPrompt != null
                && !systemPrompt.isBlank()) {

            builder.systemMessage(systemPrompt);
        }

        builder.userMessage(userPrompt);

        return chat(builder.build());
    }

    /**
     * 공통 LLM 요청을 Ollama 요청으로 변환합니다.
     */
    private OllamaChatRequest toOllamaRequest(
            LlmRequest request) {

        List<OllamaChatMessage> messages =
                request.getMessages()
                        .stream()
                        .map(this::toOllamaMessage)
                        .toList();

        String model = resolveModel(request);

        OllamaChatRequest ollamaRequest =
                new OllamaChatRequest();

        ollamaRequest.setModel(model);
        ollamaRequest.setMessages(messages);

        /*
         * 현재 HttpClient 구현은 단일 JSON 응답만 처리하므로
         * 비스트리밍으로 고정합니다.
         */
        ollamaRequest.setStream(false);

        if (request.hasTools()) {
            List<OllamaToolDefinition> tools =
                    request.getTools()
                            .stream()
                            .map(this::toOllamaToolDefinition)
                            .toList();

            ollamaRequest.setTools(tools);
        }

        if (request.hasTemperature()) {
            ollamaRequest.setOption(
                    "temperature",
                    request.getTemperature()
            );
        }

        /*
         * 공통 maxTokens는 Ollama의 num_predict 옵션으로 변환합니다.
         */
        if (request.hasMaxTokens()) {
            ollamaRequest.setOption(
                    "num_predict",
                    request.getMaxTokens()
            );
        }

        ollamaRequest.validate();

        return ollamaRequest;
    }

    /**
     * 공통 LLM 메시지를 Ollama 메시지로 변환합니다.
     */
    private OllamaChatMessage toOllamaMessage(
            LlmMessage message) {

        Objects.requireNonNull(
                message,
                "message must not be null"
        );

        OllamaChatMessage ollamaMessage =
                new OllamaChatMessage();

        ollamaMessage.setRole(
                toOllamaRole(message.getRole())
        );

        ollamaMessage.setContent(
                message.getContent()
        );

        /*
         * Tool 실행 결과 메시지입니다.
         *
         * Ollama에서는 role=tool 및 tool_name으로
         * 어떤 Tool의 결과인지 전달합니다.
         */
        if (message.isTool()) {
            String toolName =
                    message.getToolName();

            if (toolName == null
                    || toolName.isBlank()) {

                throw new OllamaLlmException(
                        "Tool 메시지에 toolName이 없습니다."
                );
            }

            ollamaMessage.setToolName(toolName);
        }

        /*
         * Assistant Tool Call 메시지를 후속 요청에 다시 포함합니다.
         */
        if (message.hasToolCalls()) {
            List<OllamaToolCall> toolCalls =
                    message.getToolCalls()
                            .stream()
                            .map(this::toOllamaToolCall)
                            .toList();

            ollamaMessage.setToolCalls(toolCalls);
        }

        ollamaMessage.validate();

        return ollamaMessage;
    }

    /**
     * 공통 Tool 정의를 Ollama Tool 정의로 변환합니다.
     */
    private OllamaToolDefinition toOllamaToolDefinition(
            LlmToolDefinition definition) {

        Objects.requireNonNull(
                definition,
                "definition must not be null"
        );

        definition.validate();

        OllamaToolDefinition.FunctionDefinition function =
                new OllamaToolDefinition.FunctionDefinition(
                        definition.getName(),
                        definition.getDescription(),
                        deepCopyMap(
                                definition.getParameters()
                        )
                );

        OllamaToolDefinition result =
                new OllamaToolDefinition(
                        definition.getType(),
                        function
                );

        result.validate();

        return result;
    }

    /**
     * 공통 Tool Call을 Ollama Tool Call로 변환합니다.
     *
     * <p>Assistant가 요청했던 Tool Call을 Tool 결과와 함께
     * 다음 Chat 요청에 다시 전달할 때 사용합니다.</p>
     */
    private OllamaToolCall toOllamaToolCall(
            LlmToolCall toolCall) {

        Objects.requireNonNull(
                toolCall,
                "toolCall must not be null"
        );

        if (!toolCall.isFunctionCall()) {
            throw new OllamaLlmException(
                    "지원하지 않는 Tool Call 유형입니다: "
                            + toolCall.getType()
            );
        }

        Map<String, Object> arguments =
                deepCopyMap(
                        toolCall.getArguments()
                );

        OllamaToolFunction function =
                new OllamaToolFunction();

        function.setName(
                toolCall.getToolName()
        );

        function.setArguments(arguments);

        OllamaToolCall ollamaToolCall =
                new OllamaToolCall();

        ollamaToolCall.setType(
                OllamaToolCall.TYPE_FUNCTION
        );

        ollamaToolCall.setFunction(function);

        Integer index =
                extractToolCallIndex(
                        toolCall.getId()
                );

        if (index != null) {
            ollamaToolCall.setIndex(index);
        }

        ollamaToolCall.validate();

        return ollamaToolCall;
    }

    /**
     * Ollama 역할 문자열을 반환합니다.
     */
    private String toOllamaRole(LlmRole role) {

        if (role == null) {
            throw new IllegalArgumentException(
                    "message role must not be null"
            );
        }

        if (role == LlmRole.SYSTEM) {
            return OllamaChatMessage.ROLE_SYSTEM;
        }

        if (role == LlmRole.USER) {
            return OllamaChatMessage.ROLE_USER;
        }

        if (role == LlmRole.ASSISTANT) {
            return OllamaChatMessage.ROLE_ASSISTANT;
        }

        if (role == LlmRole.TOOL) {
            return OllamaChatMessage.ROLE_TOOL;
        }

        throw new IllegalArgumentException(
                "Unsupported LLM role: " + role
        );
    }

    /**
     * Ollama 응답을 공통 LLM 응답으로 변환합니다.
     */
    private LlmResponse toLlmResponse(
            OllamaChatResponse response) {

        if (response == null) {
            throw new OllamaLlmException(
                    "Ollama 응답 객체가 null입니다."
            );
        }

        try {
            response.validate();

        } catch (RuntimeException exception) {
            throw new OllamaLlmException(
                    "Ollama 응답이 올바르지 않습니다.",
                    exception
            );
        }

        List<LlmToolCall> toolCalls =
                new ArrayList<>();

        List<OllamaToolCall> ollamaToolCalls =
                response.getToolCalls();

        for (int index = 0;
                index < ollamaToolCalls.size();
                index++) {

            OllamaToolCall toolCall =
                    ollamaToolCalls.get(index);

            toolCalls.add(
                    toLlmToolCall(
                            toolCall,
                            index
                    )
            );
        }

        String content =
                response.getContent();

        /*
         * Tool Call 응답에서는 content가 빈 문자열이어도 정상입니다.
         */
        if (content == null) {
            content = "";
        }

        /*
         * 일부 Ollama/Gemma 모델은 native tool_calls 대신
         * content에 call:<tool>{json} 형식으로 Tool Call을 반환합니다.
         *
         * native tool_calls가 존재하면 반드시 기존 응답을 우선하고,
         * native Tool Call이 하나도 없을 때만 fallback parser를 사용합니다.
         */
        if (toolCalls.isEmpty()) {
            List<LlmToolCall> fallbackToolCalls =
                    parseFallbackToolCalls(
                            content
                    );

            if (!fallbackToolCalls.isEmpty()) {
                toolCalls.addAll(
                        fallbackToolCalls
                );

                /*
                 * fallback 문자열을 일반 Assistant 답변으로
                 * 화면에 표시하지 않도록 비웁니다.
                 */
                content = "";
            }
        }

        return new LlmResponse(
                response.getModel(),
                content,
                response.isDone(),
                response.getDoneReason(),
                response.getPromptEvalCount(),
                response.getEvalCount(),
                toolCalls
        );
    }

    /**
     * content에 포함된 call:<tool>{json} 형식의 Tool Call을 변환합니다.
     *
     * <p>일반 Assistant 답변을 Tool Call로 오인하지 않도록
     * 전체 content가 하나 이상의 call: 표현식으로만 구성된 경우에만
     * fallback Tool Call로 인정합니다.</p>
     */
    private List<LlmToolCall> parseFallbackToolCalls(
            String content) {

        if (content == null || content.isBlank()) return List.of();

        String text = content.trim();

        if (!text.startsWith("call:")) return List.of();

        List<LlmToolCall> toolCalls = new ArrayList<>();
        int position = 0;

        while (position < text.length()) {
            position = skipWhitespace(text, position);

            if (position >= text.length()) break;

            if (!text.startsWith("call:", position)) return List.of();

            int toolNameStart = position + "call:".length();
            int argumentsStart = findArgumentsStart(text, toolNameStart);

            if (argumentsStart < 0) return List.of();

            String toolName = text.substring(toolNameStart, argumentsStart).trim();

            if (!isValidFallbackToolName(toolName)) return List.of();

            int argumentsEnd = findJsonObjectEnd(text, argumentsStart);

            if (argumentsEnd < 0) return List.of();

            String argumentsJson = text.substring(argumentsStart, argumentsEnd + 1);
            Map<String, Object> arguments = parseFallbackArguments(argumentsJson);

            LlmToolCallFunction function =
                    new LlmToolCallFunction(
                            toolName,
                            arguments
                    );

            toolCalls.add(
                    new LlmToolCall(
                            "ollama-tool-call-" + toolCalls.size(),
                            LlmToolCall.TYPE_FUNCTION,
                            function
                    )
            );

            position = argumentsEnd + 1;
        }

        return toolCalls;
    }

    /**
     * fallback Tool Call의 JSON arguments를 Map으로 변환합니다.
     */
    private Map<String, Object> parseFallbackArguments(
            String argumentsJson) {

        try {
            Map<?, ?> parsed =
                    jsonMapper.fromJson(
                            argumentsJson,
                            Map.class
                    );

            if (parsed == null) return Map.of();

            return convertMap(parsed);

        } catch (RuntimeException exception) {
            throw new OllamaLlmException(
                    "Ollama fallback Tool arguments JSON 변환에 실패했습니다. arguments="
                            + abbreviate(argumentsJson, 500),
                    exception
            );
        }
    }

    /**
     * Tool 이름 뒤의 JSON object 시작 위치를 찾습니다.
     */
    private int findArgumentsStart(
            String text,
            int startIndex) {

        for (int index = startIndex; index < text.length(); index++) {
            char ch = text.charAt(index);

            if (ch == '{') return index;

            if (Character.isWhitespace(ch)) continue;

            if (!isFallbackToolNameCharacter(ch)) return -1;
        }

        return -1;
    }

    /**
     * 문자열 및 escape를 고려하여 JSON object의 마지막 '}' 위치를 찾습니다.
     */
    private int findJsonObjectEnd(
            String text,
            int objectStart) {

        if (objectStart < 0 || objectStart >= text.length() || text.charAt(objectStart) != '{') return -1;

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = objectStart; index < text.length(); index++) {
            char ch = text.charAt(index);

            if (inString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (ch == '\\') {
                    escaped = true;
                    continue;
                }

                if (ch == '"') inString = false;

                continue;
            }

            if (ch == '"') {
                inString = true;
                continue;
            }

            if (ch == '{') {
                depth++;
                continue;
            }

            if (ch == '}') {
                depth--;

                if (depth == 0) return index;

                if (depth < 0) return -1;
            }
        }

        return -1;
    }

    /**
     * fallback Tool 이름의 허용 문자를 검증합니다.
     */
    private boolean isValidFallbackToolName(
            String toolName) {

        if (toolName == null || toolName.isBlank()) return false;

        for (int index = 0; index < toolName.length(); index++) {
            if (!isFallbackToolNameCharacter(toolName.charAt(index))) return false;
        }

        return true;
    }

    private boolean isFallbackToolNameCharacter(
            char ch) {

        return Character.isLetterOrDigit(ch)
                || ch == '_'
                || ch == '-'
                || ch == '.';
    }

    private int skipWhitespace(
            String text,
            int startIndex) {

        int index = startIndex;

        while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;

        return index;
    }


    /**
     * Ollama Tool Call을 공통 Tool Call로 변환합니다.
     */
    private LlmToolCall toLlmToolCall(
            OllamaToolCall toolCall,
            int fallbackIndex) {

        Objects.requireNonNull(
                toolCall,
                "toolCall must not be null"
        );

        try {
            toolCall.validate();

        } catch (RuntimeException exception) {
            throw new OllamaLlmException(
                    "Ollama Tool Call이 올바르지 않습니다.",
                    exception
            );
        }

        OllamaToolFunction ollamaFunction =
                toolCall.getFunction();

        Map<String, Object> arguments =
                toArgumentMap(
                        ollamaFunction.getArguments()
                );

        LlmToolCallFunction function =
                new LlmToolCallFunction(
                        ollamaFunction.getName(),
                        arguments
                );

        String toolCallId =
                createToolCallId(
                        toolCall,
                        fallbackIndex
                );

        return new LlmToolCall(
                toolCallId,
                LlmToolCall.TYPE_FUNCTION,
                function
        );
    }

    /**
     * Ollama Tool 인자를 Map으로 변환합니다.
     */
    private Map<String, Object> toArgumentMap(
            Object arguments) {

        if (arguments == null) {
            return Map.of();
        }

        if (arguments instanceof Map<?, ?> map) {
            return convertMap(map);
        }

        /*
         * 일부 모델 또는 API 변형이 arguments를 JSON 문자열로
         * 반환하는 경우도 처리합니다.
         */
        if (arguments instanceof String json) {
            if (json.isBlank()) {
                return Map.of();
            }

            try {
                Map<?, ?> parsed =
                        jsonMapper.fromJson(
                                json,
                                Map.class
                        );

                if (parsed == null) {
                    return Map.of();
                }

                return convertMap(parsed);

            } catch (RuntimeException exception) {
                throw new OllamaLlmException(
                        "Ollama Tool arguments JSON 변환에 실패했습니다.",
                        exception
                );
            }
        }

        throw new OllamaLlmException(
                "지원하지 않는 Ollama Tool arguments 형식입니다: "
                        + arguments.getClass().getName()
        );
    }

    /**
     * Tool Call ID를 생성합니다.
     *
     * <p>Ollama Tool Call에 별도 ID가 없으므로 index를 기반으로
     * Agent 내부 식별자를 생성합니다.</p>
     */
    private String createToolCallId(
            OllamaToolCall toolCall,
            int fallbackIndex) {

        int index =
                toolCall.hasIndex()
                        ? toolCall.getIndex()
                        : fallbackIndex;

        return "ollama-tool-call-" + index;
    }

    /**
     * 내부 Tool Call ID에서 Ollama index를 복원합니다.
     */
    private Integer extractToolCallIndex(
            String toolCallId) {

        if (toolCallId == null
                || toolCallId.isBlank()) {

            return null;
        }

        String prefix =
                "ollama-tool-call-";

        if (!toolCallId.startsWith(prefix)) {
            return null;
        }

        String indexText =
                toolCallId.substring(
                        prefix.length()
                );

        try {
            int index =
                    Integer.parseInt(indexText);

            return index >= 0
                    ? index
                    : null;

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * HTTP 요청을 생성합니다.
     */
    private HttpRequest createHttpRequest(
            String requestBody) {

        return HttpRequest.newBuilder()
                .uri(resolveChatUri())
                .timeout(resolveRequestTimeout())
                .header(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                )
                .header(
                        "Accept",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                requestBody,
                                StandardCharsets.UTF_8
                        )
                )
                .build();
    }

    /**
     * 요청 객체를 JSON으로 직렬화합니다.
     */
    private String serializeRequest(
            OllamaChatRequest request) {

        try {
            String json =
                    jsonMapper.toJson(request);

            if (json == null || json.isBlank()) {
                throw new OllamaLlmException(
                        "직렬화된 Ollama 요청 JSON이 비어 있습니다."
                );
            }

            return json;

        } catch (OllamaLlmException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new OllamaLlmException(
                    "Ollama 요청 JSON 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    /**
     * 응답 JSON을 객체로 역직렬화합니다.
     */
    private OllamaChatResponse deserializeResponse(
            String responseBody) {

        if (responseBody == null
                || responseBody.isBlank()) {

            throw new OllamaLlmException(
                    "Ollama 응답 본문이 비어 있습니다."
            );
        }

        try {
            OllamaChatResponse response =
                    jsonMapper.fromJson(
                            responseBody,
                            OllamaChatResponse.class
                    );

            if (response == null) {
                throw new OllamaLlmException(
                        "역직렬화된 Ollama 응답이 null입니다."
                );
            }

            return response;

        } catch (OllamaLlmException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new OllamaLlmException(
                    "Ollama 응답 JSON 역직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    /**
     * HTTP 응답 상태를 검증합니다.
     */
    private void validateHttpResponse(
            HttpResponse<String> response) {

        int statusCode =
                response.statusCode();

        if (statusCode >= 200
                && statusCode < 300) {

            return;
        }

        throw new OllamaLlmException(
                "Ollama API 요청에 실패했습니다. "
                        + "statusCode=" + statusCode
                        + ", responseBody="
                        + abbreviate(
                                response.body(),
                                MAX_ERROR_BODY_LENGTH
                        )
        );
    }

    /**
     * LLM 요청을 검증합니다.
     */
    private void validateRequest(
            LlmRequest request) {

        if (!request.hasMessages()) {
            throw new IllegalArgumentException(
                    "request messages must not be empty"
            );
        }

        for (LlmMessage message
                : request.getMessages()) {

            if (message == null) {
                throw new IllegalArgumentException(
                        "request messages must not contain null"
                );
            }

            if (message.getRole() == null) {
                throw new IllegalArgumentException(
                        "message role must not be null"
                );
            }

            if (message.isTool()) {
                if (!message.hasContent()) {
                    throw new IllegalArgumentException(
                            "tool message content must not be blank"
                    );
                }

                if (!message.hasName()) {
                    throw new IllegalArgumentException(
                            "tool message toolName must not be blank"
                    );
                }
            }

            if (message.hasToolCalls()
                    && !message.isAssistant()) {

                throw new IllegalArgumentException(
                        "toolCalls are only allowed "
                                + "for assistant messages"
                );
            }
        }

        if (request.hasTools()) {
            for (LlmToolDefinition tool
                    : request.getTools()) {

                tool.validate();
            }
        }
    }

    /**
     * 요청 모델명을 결정합니다.
     */
    private String resolveModel(
            LlmRequest request) {

        if (request.hasModel()) {
            return request.getModel();
        }

        String configuredModel =
                configuration.getModel();

        if (configuredModel == null
                || configuredModel.isBlank()) {

            throw new IllegalStateException(
                    "Ollama model is not configured"
            );
        }

        return configuredModel.trim();
    }

    /**
     * Chat API URI를 생성합니다.
     */
    private URI resolveChatUri() {
        String baseUrl =
                configuration.getBaseUrl();

        if (baseUrl == null
                || baseUrl.isBlank()) {

            throw new IllegalStateException(
                    "Ollama baseUrl is not configured"
            );
        }

        String normalized =
                baseUrl.trim();

        while (normalized.endsWith("/")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        return URI.create(
                normalized + CHAT_API_PATH
        );
    }

    /**
     * 요청 제한시간을 반환합니다.
     */
    private Duration resolveRequestTimeout() {
        Duration configured =
                configuration.getRequestTimeout();

        if (configured == null
                || configured.isZero()
                || configured.isNegative()) {

            return DEFAULT_REQUEST_TIMEOUT;
        }

        return configured;
    }

    /**
     * Map 키를 문자열로 정규화하여 복사합니다.
     */
    private Map<String, Object> convertMap(
            Map<?, ?> source) {

        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> converted =
                new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry
                : source.entrySet()) {

            if (entry.getKey() == null) {
                continue;
            }

            converted.put(
                    String.valueOf(entry.getKey()),
                    deepCopyValue(entry.getValue())
            );
        }

        return converted;
    }

    /**
     * 문자열 키 Map을 깊은 복사합니다.
     */
    private Map<String, Object> deepCopyMap(
            Map<String, Object> source) {

        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> copied =
                new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry
                : source.entrySet()) {

            if (entry.getKey() == null) {
                continue;
            }

            copied.put(
                    entry.getKey(),
                    deepCopyValue(entry.getValue())
            );
        }

        return copied;
    }

    /**
     * 중첩 Map과 List를 재귀적으로 복사합니다.
     */
    private Object deepCopyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return convertMap(map);
        }

        if (value instanceof Iterable<?> iterable) {
            List<Object> copied =
                    new ArrayList<>();

            for (Object item : iterable) {
                copied.add(
                        deepCopyValue(item)
                );
            }

            return copied;
        }

        return value;
    }

    private static String abbreviate(
            String value,
            int maxLength) {

        if (value == null) {
            return "";
        }

        if (maxLength < 4) {
            throw new IllegalArgumentException(
                    "maxLength must be greater than 3"
            );
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(
                0,
                maxLength - 3
        ) + "...";
    }
}