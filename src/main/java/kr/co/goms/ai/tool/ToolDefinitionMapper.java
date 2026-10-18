/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import kr.co.goms.ai.llm.LlmToolDefinition;

/**
 * Agent Tool 정의를 LLM Tool Calling 형식으로 변환합니다.
 *
 * <p>Tool 실행 계층의 {@link AgentTool}이 LLM 공급자별 DTO에
 * 직접 의존하지 않도록 중간 변환 책임을 제공합니다.</p>
 *
 * <p>변환 흐름:</p>
 *
 * <pre>
 * AgentTool
 *     ↓
 * ToolDefinitionMapper
 *     ↓
 * LlmToolDefinition
 *     ↓
 * OllamaToolDefinition
 * </pre>
 */
public interface ToolDefinitionMapper {

    /**
     * 하나의 Agent Tool을 LLM Tool 정의로 변환합니다.
     *
     * @param tool 변환할 Agent Tool
     * @return LLM Tool 정의
     * @throws ToolDefinitionMappingException 변환할 수 없는 경우
     */
    LlmToolDefinition map(AgentTool tool);

    /**
     * 여러 Agent Tool을 LLM Tool 정의 목록으로 변환합니다.
     *
     * <p>입력 Collection의 순서를 유지합니다.</p>
     *
     * @param tools 변환할 Agent Tool 목록
     * @return 수정할 수 없는 LLM Tool 정의 목록
     * @throws ToolDefinitionMappingException 하나 이상의 변환에 실패한 경우
     */
    default List<LlmToolDefinition> mapAll(
            Collection<? extends AgentTool> tools) {

        Objects.requireNonNull(
                tools,
                "tools must not be null"
        );

        if (tools.isEmpty()) {
            return List.of();
        }

        List<LlmToolDefinition> definitions =
                new ArrayList<>(tools.size());

        for (AgentTool tool : tools) {
            if (tool == null) {
                throw new ToolDefinitionMappingException(
                        "Tool collection must not contain null."
                );
            }

            LlmToolDefinition definition = map(tool);

            if (definition == null) {
                throw new ToolDefinitionMappingException(
                        "Mapped LLM tool definition must not be null."
                );
            }

            definitions.add(definition);
        }

        return Collections.unmodifiableList(definitions);
    }
}