/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.llm.LlmToolDefinition;

/**
 * {@link ToolDefinitionProvider}의 기본 구현체입니다.
 *
 * <p>{@link ToolRegistry}에 등록된 {@link AgentTool} 목록을 조회하고,
 * {@link ToolDefinitionMapper}를 사용하여 LLM Tool 정의 목록으로
 * 변환합니다.</p>
 *
 * <p>캐시를 활성화하면 최초 조회 시 Tool 정의를 생성한 후
 * {@link #refresh()}가 호출될 때까지 동일한 정의 목록을 반환합니다.</p>
 */
public final class DefaultToolDefinitionProvider
        implements ToolDefinitionProvider {

    private final ToolRegistry toolRegistry;
    private final ToolDefinitionMapper definitionMapper;
    private final boolean cacheEnabled;

    private volatile List<LlmToolDefinition> cachedDefinitions;

    /**
     * 캐시를 사용하지 않는 Provider를 생성합니다.
     *
     * @param toolRegistry     Tool Registry
     * @param definitionMapper Tool 정의 변환기
     */
    public DefaultToolDefinitionProvider(
            ToolRegistry toolRegistry,
            ToolDefinitionMapper definitionMapper) {

        this(
                toolRegistry,
                definitionMapper,
                false
        );
    }

    /**
     * Provider를 생성합니다.
     *
     * @param toolRegistry     Tool Registry
     * @param definitionMapper Tool 정의 변환기
     * @param cacheEnabled     Tool 정의 캐시 사용 여부
     */
    public DefaultToolDefinitionProvider(
            ToolRegistry toolRegistry,
            ToolDefinitionMapper definitionMapper,
            boolean cacheEnabled) {

        this.toolRegistry = Objects.requireNonNull(
                toolRegistry,
                "toolRegistry must not be null"
        );

        this.definitionMapper = Objects.requireNonNull(
                definitionMapper,
                "definitionMapper must not be null"
        );

        this.cacheEnabled = cacheEnabled;
    }

    /**
     * 현재 등록된 전체 Tool의 LLM 정의 목록을 반환합니다.
     *
     * @return 수정할 수 없는 Tool 정의 목록
     */
    @Override
    public List<LlmToolDefinition> getToolDefinitions() {
        if (!cacheEnabled) {
            return loadDefinitions();
        }

        List<LlmToolDefinition> definitions =
                cachedDefinitions;

        if (definitions != null) {
            return definitions;
        }

        synchronized (this) {
            definitions = cachedDefinitions;

            if (definitions == null) {
                definitions = loadDefinitions();
                cachedDefinitions = definitions;
            }

            return definitions;
        }
    }

    /**
     * 지정한 이름의 Tool 정의만 반환합니다.
     *
     * <p>요청된 이름의 순서를 유지하며 중복 이름은 제거합니다.
     * 등록되지 않은 Tool 이름은 결과에서 제외합니다.</p>
     *
     * @param toolNames 포함할 Tool 이름 목록
     * @return 수정할 수 없는 Tool 정의 목록
     */
    @Override
    public List<LlmToolDefinition> getToolDefinitions(
            List<String> toolNames) {

        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }

        Map<String, LlmToolDefinition> definitionsByName =
                new LinkedHashMap<>();

        for (LlmToolDefinition definition
                : getToolDefinitions()) {

            definitionsByName.put(
                    definition.getName(),
                    definition
            );
        }

        List<LlmToolDefinition> selected =
                new ArrayList<>();

        Map<String, Boolean> addedNames =
                new LinkedHashMap<>();

        for (String toolName : toolNames) {
            if (toolName == null || toolName.isBlank()) {
                continue;
            }

            String normalizedName = toolName.trim();

            if (addedNames.containsKey(normalizedName)) {
                continue;
            }

            LlmToolDefinition definition =
                    definitionsByName.get(normalizedName);

            if (definition != null) {
                selected.add(definition);
                addedNames.put(normalizedName, Boolean.TRUE);
            }
        }

        return Collections.unmodifiableList(selected);
    }

    /**
     * Tool 정의 캐시를 초기화합니다.
     *
     * <p>캐시를 사용하지 않는 경우에는 아무 작업도 하지 않습니다.</p>
     */
    @Override
    public synchronized void refresh() {
        cachedDefinitions = null;
    }

    /**
     * 캐시 사용 여부를 반환합니다.
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * Tool 정의가 현재 캐시되어 있는지 확인합니다.
     */
    public boolean isCached() {
        return cachedDefinitions != null;
    }

    /**
     * Tool Registry에서 Tool 목록을 조회하고 LLM 정의로 변환합니다.
     */
    private List<LlmToolDefinition> loadDefinitions() {
        List<AgentTool> tools = resolveTools();

        if (tools.isEmpty()) {
            return List.of();
        }

        validateDuplicateToolNames(tools);

        try {
            return definitionMapper.mapAll(tools);

        } catch (ToolDefinitionMappingException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw new ToolDefinitionMappingException(
                    "Failed to create LLM Tool definitions.",
                    exception
            );
        }
    }

    /**
     * Tool Registry에서 등록된 Tool 목록을 조회합니다.
     */
    private List<AgentTool> resolveTools() {
        /*
         * 현재 구현은 ToolRegistry#getTools()가
         * List<AgentTool> 또는 Collection<AgentTool>을 반환한다고
         * 가정합니다.
         */
        var registeredTools = toolRegistry.getTools();

        if (registeredTools == null
                || registeredTools.isEmpty()) {

            return List.of();
        }

        List<AgentTool> tools =
                new ArrayList<>(registeredTools.size());

        for (AgentTool tool : registeredTools) {
            tools.add(
                    Objects.requireNonNull(
                            tool,
                            "ToolRegistry must not contain null tools"
                    )
            );
        }

        return Collections.unmodifiableList(tools);
    }

    /**
     * 중복된 Tool 이름이 등록되어 있는지 검증합니다.
     */
    private void validateDuplicateToolNames(
            List<AgentTool> tools) {

        Map<String, AgentTool> toolsByName =
                new LinkedHashMap<>();

        for (AgentTool tool : tools) {
            String name = resolveToolName(tool);

            AgentTool previous =
                    toolsByName.putIfAbsent(name, tool);

            if (previous != null) {
                throw ToolDefinitionMappingException.invalidTool(
                        name,
                        "Duplicate Tool name is registered."
                );
            }
        }
    }

    /**
     * Tool 이름을 검증하고 반환합니다.
     */
    private String resolveToolName(AgentTool tool) {
        String name = tool.getName();

        if (name == null || name.isBlank()) {
            throw ToolDefinitionMappingException.invalidTool(
                    "unknown",
                    "Tool name must not be blank."
            );
        }

        return name.trim();
    }
}