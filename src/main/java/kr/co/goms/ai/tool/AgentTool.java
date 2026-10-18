/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.util.Map;

/**
 * Agent가 호출할 수 있는 Tool의 표준 인터페이스입니다.
 *
 * <p>각 Tool 구현체는 다음 정보를 제공합니다.</p>
 *
 * <ul>
 *     <li>LLM에 공개할 Tool 이름</li>
 *     <li>Tool 설명</li>
 *     <li>JSON Schema 형식의 입력 파라미터</li>
 *     <li>요청 검증 로직</li>
 *     <li>실제 Tool 실행 로직</li>
 * </ul>
 *
 * <p>Tool 이름은 LLM 함수명으로 사용되므로 영문자 또는 밑줄로
 * 시작하고, 이후에는 영문자·숫자·밑줄·하이픈만 사용하는 것을
 * 권장합니다.</p>
 */
public interface AgentTool {

    /**
     * Tool 이름을 반환합니다.
     *
     * <p>이 이름은 ToolRegistry 조회 및 LLM Tool Calling의
     * 함수명으로 사용됩니다.</p>
     *
     * @return 공백이 아닌 고유 Tool 이름
     */
    String getName();

    /**
     * Tool의 기능과 사용 목적을 설명합니다.
     *
     * <p>이 설명은 LLM이 Tool 호출 여부를 판단할 때 사용됩니다.</p>
     *
     * @return Tool 설명. 설명이 없으면 빈 문자열 가능
     */
    default String getDescription() {
        return "";
    }

    /**
     * Tool 입력 파라미터의 JSON Schema를 반환합니다.
     *
     * <p>반환값은 일반적으로 다음 구조를 가집니다.</p>
     *
     * <pre>
     * {
     *   "type": "object",
     *   "properties": {
     *     "xhtml": {
     *       "type": "string",
     *       "description": "검증할 XHTML 문서"
     *     }
     *   },
     *   "required": ["xhtml"]
     * }
     * </pre>
     *
     * <p>인자가 없는 Tool은 기본 빈 Object Schema를 반환할 수 있습니다.</p>
     *
     * @return JSON Schema 형식 입력 파라미터
     */
    default Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of()
        );
    }

    /**
     * Tool 요청을 검증합니다.
     *
     * <p>기본 구현은 모든 요청을 유효한 것으로 처리합니다.</p>
     *
     * @param request Tool 실행 요청
     * @param context Tool 실행 컨텍스트
     * @return Tool 요청 검증 결과
     */
    default ToolValidationResult validate(
            ToolRequest request,
            ToolContext context) {

        return ToolValidationResult.valid();
    }

    /**
     * Tool을 실행합니다.
     *
     * @param request Tool 실행 요청
     * @param context Tool 실행 컨텍스트
     * @return Tool 실행 결과
     * @throws RuntimeException Tool 실행 중 복구할 수 없는 오류가 발생한 경우
     */
    ToolResult execute(
            ToolRequest request,
            ToolContext context
    );

    /**
     * 현재 Tool을 사용할 수 있는지 확인합니다.
     *
     * <p>외부 프로그램 설치 여부, 현재 프로젝트 상태, 파일 접근 가능 여부
     * 등을 기준으로 판단할 수 있습니다.</p>
     *
     * @return 사용 가능하면 {@code true}
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 현재 요청과 컨텍스트에서 Tool을 실행할 수 있는지 확인합니다.
     *
     * <p>기본 구현은 Tool 자체 가용성과 요청 검증 결과를 함께 확인합니다.</p>
     *
     * @param request Tool 실행 요청
     * @param context Tool 실행 컨텍스트
     * @return 실행 가능하면 {@code true}
     */
    default boolean canExecute(
            ToolRequest request,
            ToolContext context) {

        if (!isAvailable()) {
            return false;
        }

        if (request == null) {
            return false;
        }

        if (!getName().equals(request.getToolName())) {
            return false;
        }

        ToolValidationResult validationResult =
                validate(request, context);

        return validationResult == null
                || validationResult.isValid();
    }

    /**
     * Tool 이름과 기본 메타데이터의 유효성을 검증합니다.
     *
     * <p>ToolRegistry 등록 시 호출할 수 있습니다.</p>
     */
    default void validateDefinition() {
        String name = getName();

        if (name == null || name.isBlank()) {
            throw new IllegalStateException(
                    "Agent Tool name must not be blank"
            );
        }

        String normalizedName = name.trim();

        if (!normalizedName.matches(
                "[A-Za-z_][A-Za-z0-9_-]*")) {

            throw new IllegalStateException(
                    "Invalid Agent Tool name: "
                            + normalizedName
            );
        }

        Map<String, Object> schema =
                getInputSchema();

        if (schema == null || schema.isEmpty()) {
            throw new IllegalStateException(
                    "Agent Tool input schema must not be empty. "
                            + "tool=" + normalizedName
            );
        }

        Object schemaType =
                schema.get("type");

        if (!(schemaType instanceof String type)
                || !"object".equals(type)) {

            throw new IllegalStateException(
                    "Agent Tool input schema must have type=object. "
                            + "tool=" + normalizedName
            );
        }

        Object properties =
                schema.get("properties");

        if (properties != null
                && !(properties instanceof Map<?, ?>)) {

            throw new IllegalStateException(
                    "Agent Tool input schema properties "
                            + "must be an object. tool="
                            + normalizedName
            );
        }
    }
}