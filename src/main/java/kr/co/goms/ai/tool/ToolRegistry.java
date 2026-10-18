/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Agent Tool을 이름 기준으로 등록하고 조회하는 Registry입니다.
 *
 * <p>등록 순서를 유지하며, 동일한 이름의 Tool 중복 등록을
 * 허용하지 않습니다.</p>
 */
public final class ToolRegistry {

    private final Map<String, AgentTool> tools =
            new LinkedHashMap<>();

    /**
     * 빈 ToolRegistry를 생성합니다.
     */
    public ToolRegistry() {
    }

    /**
     * 초기 Tool 목록을 포함하는 Registry를 생성합니다.
     *
     * @param tools 초기 Tool 목록
     */
    public ToolRegistry(
            Collection<? extends AgentTool> tools) {

        registerAll(tools);
    }

    /**
     * Tool을 등록합니다.
     *
     * @param tool 등록할 Tool
     * @throws IllegalArgumentException 동일한 이름이 이미 등록된 경우
     */
    public synchronized void register(
            AgentTool tool) {

        Objects.requireNonNull(
                tool,
                "tool must not be null"
        );

        tool.validateDefinition();

        String toolName =
                normalizeToolName(tool.getName());

        if (tools.containsKey(toolName)) {
            throw new IllegalArgumentException(
                    "Tool is already registered: "
                            + toolName
            );
        }

        tools.put(toolName, tool);
    }

    /**
     * 여러 Tool을 등록합니다.
     *
     * <p>등록 도중 하나라도 실패하면 이전에 등록된 Tool은
     * 그대로 유지됩니다.</p>
     *
     * @param tools 등록할 Tool 목록
     */
    public synchronized void registerAll(
            Collection<? extends AgentTool> tools) {

        Objects.requireNonNull(
                tools,
                "tools must not be null"
        );

        for (AgentTool tool : tools) {
            register(tool);
        }
    }

    /**
     * 기존 Tool을 동일 이름의 새 Tool로 교체합니다.
     *
     * @param tool 등록 또는 교체할 Tool
     * @return 이전 Tool 또는 {@code null}
     */
    public synchronized AgentTool registerOrReplace(
            AgentTool tool) {

        Objects.requireNonNull(
                tool,
                "tool must not be null"
        );

        tool.validateDefinition();

        String toolName =
                normalizeToolName(tool.getName());

        return tools.put(toolName, tool);
    }

    /**
     * 이름으로 Tool을 조회합니다.
     *
     * @param toolName Tool 이름
     * @return Tool 또는 {@code null}
     */
    public synchronized AgentTool get(
            String toolName) {

        if (toolName == null || toolName.isBlank()) {
            return null;
        }

        return tools.get(toolName.trim());
    }

    /**
     * 이름으로 Tool을 Optional 형태로 조회합니다.
     */
    public synchronized Optional<AgentTool> find(
            String toolName) {

        return Optional.ofNullable(get(toolName));
    }

    /**
     * Tool이 등록되어 있는지 확인합니다.
     */
    public synchronized boolean contains(
            String toolName) {

        if (toolName == null || toolName.isBlank()) {
            return false;
        }

        return tools.containsKey(toolName.trim());
    }

    /**
     * Tool 등록을 해제합니다.
     *
     * @param toolName Tool 이름
     * @return 제거된 Tool 또는 {@code null}
     */
    public synchronized AgentTool unregister(
            String toolName) {

        if (toolName == null || toolName.isBlank()) {
            return null;
        }

        return tools.remove(toolName.trim());
    }

    /**
     * 지정한 Tool이 등록되어 있을 때만 해제합니다.
     *
     * @param tool 제거할 Tool
     * @return 제거되었으면 {@code true}
     */
    public synchronized boolean unregister(
            AgentTool tool) {

        if (tool == null) {
            return false;
        }

        String toolName =
                normalizeToolName(tool.getName());

        AgentTool registered = tools.get(toolName);

        if (registered != tool) {
            return false;
        }

        tools.remove(toolName);
        return true;
    }

    /**
     * 등록된 Tool 목록을 반환합니다.
     *
     * <p>등록 순서를 유지하며 수정할 수 없는 복사본을 반환합니다.</p>
     */
    public synchronized List<AgentTool> getTools() {
        if (tools.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(tools.values())
        );
    }

    /**
     * 등록된 Tool 이름 목록을 반환합니다.
     */
    public synchronized List<String> getToolNames() {
        if (tools.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(tools.keySet())
        );
    }

    /**
     * 등록된 Tool Map을 반환합니다.
     *
     * @return 수정할 수 없는 복사본
     */
    public synchronized Map<String, AgentTool>
            getToolMap() {

        if (tools.isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(
                new LinkedHashMap<>(tools)
        );
    }

    /**
     * 등록된 Tool 수를 반환합니다.
     */
    public synchronized int size() {
        return tools.size();
    }

    /**
     * 등록된 Tool이 없는지 확인합니다.
     */
    public synchronized boolean isEmpty() {
        return tools.isEmpty();
    }

    /**
     * 사용 가능한 Tool이 하나 이상 존재하는지 확인합니다.
     */
    public synchronized boolean hasAvailableTools() {
        for (AgentTool tool : tools.values()) {
            try {
                if (tool.isAvailable()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // 가용성 확인 실패 Tool은 사용 불가로 처리합니다.
            }
        }

        return false;
    }

    /**
     * 사용 가능한 Tool 목록을 반환합니다.
     */
    public synchronized List<AgentTool>
            getAvailableTools() {

        if (tools.isEmpty()) {
            return List.of();
        }

        List<AgentTool> available =
                new ArrayList<>();

        for (AgentTool tool : tools.values()) {
            try {
                if (tool.isAvailable()) {
                    available.add(tool);
                }
            } catch (RuntimeException ignored) {
                // 해당 Tool은 제외합니다.
            }
        }

        return Collections.unmodifiableList(available);
    }

    /**
     * 모든 Tool 등록을 제거합니다.
     */
    public synchronized void clear() {
        tools.clear();
    }

    /**
     * 모든 등록 Tool 정의를 검증합니다.
     */
    public synchronized void validateAll() {
        for (AgentTool tool : tools.values()) {
            tool.validateDefinition();

            String actualName =
                    normalizeToolName(tool.getName());

            if (!tools.containsKey(actualName)) {
                throw new IllegalStateException(
                        "Tool Registry key mismatch: "
                                + actualName
                );
            }
        }
    }

    private static String normalizeToolName(
            String toolName) {

        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException(
                    "toolName must not be blank"
            );
        }

        String normalized = toolName.trim();

        if (!normalized.matches(
                "[A-Za-z_][A-Za-z0-9_-]*")) {

            throw new IllegalArgumentException(
                    "Invalid Tool name: "
                            + normalized
            );
        }

        return normalized;
    }

    @Override
    public synchronized String toString() {
        return "ToolRegistry{"
                + "toolNames=" + tools.keySet()
                + '}';
    }
}