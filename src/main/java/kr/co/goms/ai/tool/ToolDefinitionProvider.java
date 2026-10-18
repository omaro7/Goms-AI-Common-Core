/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;


import java.util.List;

import kr.co.goms.ai.llm.LlmToolDefinition;

/**
 * 현재 Agent가 사용할 수 있는 LLM Tool 정의를 제공합니다.
 *
 * <p>구현체는 일반적으로 {@link ToolRegistry}에 등록된
 * {@link AgentTool} 목록을 조회한 뒤 {@link ToolDefinitionMapper}를
 * 사용하여 {@link LlmToolDefinition} 목록으로 변환합니다.</p>
 *
 * <pre>
 * ToolRegistry
 *     ↓
 * AgentTool 목록
 *     ↓
 * ToolDefinitionMapper
 *     ↓
 * LlmToolDefinition 목록
 * </pre>
 */
public interface ToolDefinitionProvider {

    /**
     * 현재 사용 가능한 전체 Tool 정의를 반환합니다.
     *
     * @return 수정할 수 없는 LLM Tool 정의 목록
     * @throws ToolDefinitionMappingException Tool 정의 변환에 실패한 경우
     */
    List<LlmToolDefinition> getToolDefinitions();

    /**
     * 지정한 Tool 이름만 포함하는 Tool 정의 목록을 반환합니다.
     *
     * <p>기본 구현은 전체 Tool 정의에서 이름이 일치하는 항목을
     * 필터링합니다.</p>
     *
     * @param toolNames 포함할 Tool 이름 목록
     * @return 지정된 Tool 이름에 해당하는 정의 목록
     */
    default List<LlmToolDefinition> getToolDefinitions(
            List<String> toolNames) {

        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }

        return getToolDefinitions()
                .stream()
                .filter(definition ->
                        toolNames.contains(
                                definition.getName()
                        )
                )
                .toList();
    }

    /**
     * 지정한 이름의 Tool 정의를 반환합니다.
     *
     * @param toolName Tool 이름
     * @return Tool 정의 또는 {@code null}
     */
    default LlmToolDefinition getToolDefinition(
            String toolName) {

        if (toolName == null || toolName.isBlank()) {
            return null;
        }

        String normalizedName = toolName.trim();

        return getToolDefinitions()
                .stream()
                .filter(definition ->
                        normalizedName.equals(
                                definition.getName()
                        )
                )
                .findFirst()
                .orElse(null);
    }

    /**
     * 지정한 이름의 Tool 정의가 존재하는지 확인합니다.
     *
     * @param toolName Tool 이름
     * @return Tool 정의가 존재하면 {@code true}
     */
    default boolean containsTool(String toolName) {
        return getToolDefinition(toolName) != null;
    }

    /**
     * 현재 제공되는 Tool 개수를 반환합니다.
     */
    default int getToolDefinitionCount() {
        return getToolDefinitions().size();
    }

    /**
     * 사용 가능한 Tool 정의가 존재하는지 확인합니다.
     */
    default boolean hasToolDefinitions() {
        return !getToolDefinitions().isEmpty();
    }

    /**
     * Tool 정의 목록을 새로 조회해야 하는 경우 호출합니다.
     *
     * <p>기본 구현은 아무 작업도 하지 않습니다. 구현체가 Tool 정의를
     * 캐싱한다면 이 메서드에서 캐시를 초기화할 수 있습니다.</p>
     */
    default void refresh() {
        // 기본 구현은 캐시를 사용하지 않습니다.
    }
}