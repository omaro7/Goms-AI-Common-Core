/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)1
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

/**
 * Tool 실행 진입점을 정의하는 인터페이스입니다.
 *
 * <p>Agent가 전달한 {@link ToolRequest}를 검증하고,
 * {@link ToolRegistry}에 등록된 Tool을 찾아 실행한 뒤
 * {@link ToolResult}를 반환합니다.</p>
 *
 * <pre>
 * ToolRequest
 *     ↓
 * ToolExecutor
 *     ↓
 * ToolRegistry
 *     ↓
 * AgentTool
 *     ↓
 * ToolResult
 * </pre>
 */
public interface ToolExecutor {

    /**
     * Tool 요청을 실행합니다.
     *
     * @param request Tool 실행 요청
     * @param context Tool 실행 컨텍스트
     * @return Tool 실행 결과
     * @throws ToolExecutionException Tool을 찾지 못하거나 실행에 실패한 경우
     */
    ToolResult execute(
            ToolRequest request,
            ToolContext context
    );

    /**
     * 별도의 Context 없이 Tool 요청을 실행합니다.
     *
     * <p>기본적으로 빈 {@link ToolContext}를 생성하여 실행합니다.</p>
     *
     * @param request Tool 실행 요청
     * @return Tool 실행 결과
     */
    default ToolResult execute(ToolRequest request) {
        return execute(
                request,
                ToolContext.builder().build()
        );
    }

    /**
     * Tool 이름과 인자를 이용해 Tool을 실행합니다.
     *
     * @param toolName Tool 이름
     * @param arguments Tool 실행 인자
     * @return Tool 실행 결과
     */
    default ToolResult execute(
            String toolName,
            java.util.Map<String, Object> arguments) {

        ToolRequest request =
                ToolRequest.builder()
                        .toolName(toolName)
                        .arguments(arguments)
                        .build();

        return execute(request);
    }

    /**
     * 인자가 없는 Tool을 실행합니다.
     *
     * @param toolName Tool 이름
     * @return Tool 실행 결과
     */
    default ToolResult execute(String toolName) {
        return execute(
                ToolRequest.builder()
                        .toolName(toolName)
                        .build()
        );
    }

    /**
     * 지정한 Tool을 실행할 수 있는지 확인합니다.
     *
     * <p>구현체에서는 ToolRegistry 등록 여부와 Tool 활성 상태 등을
     * 기준으로 판단할 수 있습니다.</p>
     *
     * @param toolName Tool 이름
     * @return 실행 가능하면 {@code true}
     */
    boolean canExecute(String toolName);

    /**
     * Tool 실행기가 현재 사용 가능한지 확인합니다.
     *
     * @return 실행 가능하면 {@code true}
     */
    default boolean isAvailable() {
        return true;
    }
}