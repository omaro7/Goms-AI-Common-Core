/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.tool;

import java.util.Objects;

import kr.co.goms.ai.logging.ExecutionLogger;
import kr.co.goms.ai.logging.ExecutionLogContext;

/**
 * {@link ToolExecutor}의 기본 구현체입니다.
 *
 * <p>다음 순서로 Tool 요청을 처리합니다.</p>
 *
 * <ol>
 *     <li>ToolRequest 및 ToolContext 검증</li>
 *     <li>ToolRegistry에서 AgentTool 조회</li>
 *     <li>Tool 실행 가능 여부 확인</li>
 *     <li>Tool 요청 검증</li>
 *     <li>AgentTool 실행</li>
 *     <li>실행 결과 검증 및 반환</li>
 * </ol>
 */
public final class DefaultToolExecutor implements ToolExecutor {

    private final ToolRegistry toolRegistry;
    private final ExecutionLogger executionLogger;
    
    /**
     * Tool 실행기를 생성합니다.
     *
     * @param toolRegistry 	  Tool Registry
     * @param executionLogger Logger 
     */
    public DefaultToolExecutor( ToolRegistry toolRegistry, ExecutionLogger executionLogger) {
        this.toolRegistry = Objects.requireNonNull( toolRegistry, "toolRegistry must not be null");
        this.executionLogger = Objects.requireNonNull( executionLogger, "executionLogger must not be null");
    }
    
    /**
     * Tool 요청을 실행합니다.
     *
     * @param request Tool 실행 요청
     * @param context Tool 실행 컨텍스트
     * @return Tool 실행 결과
     * @throws ToolExecutionException Tool 조회, 검증 또는 실행에 실패한 경우
     */
	@Override
	public ToolResult execute(
	        ToolRequest request,
	        ToolContext context) {

	    validateRequest(request);

	    ToolContext resolvedContext =
	            context == null
	                    ? ToolContext.builder().build()
	                    : context;

	    String toolName = request.getToolName();
	    AgentTool tool = resolveTool(toolName);

	    ExecutionLogContext logContext =
	            executionLogger.start(
	                    resolvedContext.getRunId(),
	                    request.getRequestId(),
	                    resolvedContext.getProjectId(),
	                    toolName
	            );

	    try {

	        ToolValidationResult validationResult =
	                validateToolRequest(
	                        tool,
	                        request,
	                        resolvedContext
	                );

	        if (!isValidationSuccessful(validationResult)) {

	            ToolResult result =
	                    createValidationFailureResult(
	                            request,
	                            validationResult
	                    );

	            executionLogger.failure(
	                    logContext,
	                    new IllegalStateException(result.getMessage())
	            );

	            return result;
	        }

	        ToolResult result =
	                executeTool(
	                        tool,
	                        request,
	                        resolvedContext
	                );

	        result =
	                validateResult(
	                        request,
	                        result
	                );

	        if (result.getStatus() == ToolStatus.SUCCESS) {
	            executionLogger.success(logContext);
	        } else {
	            executionLogger.failure(logContext, new IllegalStateException(result.getMessage()));
	        }

	        return result;

	    } catch (ToolExecutionException exception) {

	        executionLogger.failure(
	                logContext,
	                exception
	        );

	        throw exception;

	    } catch (RuntimeException exception) {

	        executionLogger.failure(
	                logContext,
	                exception
	        );

	        throw ToolExecutionException.executionFailed(
	                toolName,
	                exception
	        );
	    }
	}
	
	/**
     * 지정한 Tool을 실행할 수 있는지 확인합니다.
     *
     * @param toolName Tool 이름
     * @return 실행 가능하면 {@code true}
     */
    @Override
    public boolean canExecute(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return false;
        }

        AgentTool tool = findTool(toolName.trim());

        if (tool == null) {
            return false;
        }

        try {
            return tool.isAvailable();

        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * ToolExecutor 사용 가능 여부를 반환합니다.
     */
    @Override
    public boolean isAvailable() {
        return toolRegistry != null;
    }

    /**
     * ToolRegistry에서 Tool을 조회합니다.
     */
    private AgentTool resolveTool(String toolName) {
        AgentTool tool = findTool(toolName);

        if (tool == null) {
            throw ToolExecutionException.toolNotFound(
                    toolName
            );
        }

        if (!tool.isAvailable()) {
            throw ToolExecutionException.toolUnavailable(
                    toolName
            );
        }

        return tool;
    }

    /**
     * ToolRegistry에서 Tool을 조회합니다.
     *
     * <p>현재 구현은 {@code ToolRegistry#get(String)}이
     * {@code AgentTool} 또는 {@code null}을 반환한다고 가정합니다.</p>
     */
    private AgentTool findTool(String toolName) {
        try {
            return toolRegistry.get(toolName);

        } catch (RuntimeException exception) {
            throw ToolExecutionException.registryFailed(
                    toolName,
                    exception
            );
        }
    }

    /**
     * Tool 요청을 검증합니다.
     */
    private ToolValidationResult validateToolRequest(
            AgentTool tool,
            ToolRequest request,
            ToolContext context) {

        try {
            ToolValidationResult result =
                    tool.validate(
                            request,
                            context
                    );

            /*
             * Tool 구현체가 별도 검증이 필요하지 않아 null을
             * 반환하는 경우 성공으로 처리합니다.
             */
            if (result == null) {
                return ToolValidationResult.valid();
            }

            return result;

        } catch (RuntimeException exception) {
            throw ToolExecutionException.validationFailed(
                    request.getToolName(),
                    exception
            );
        }
    }

    /**
     * Tool을 실행합니다.
     */
    private ToolResult executeTool(
            AgentTool tool,
            ToolRequest request,
            ToolContext context) {

        try {
            ToolResult result =
                    tool.execute(
                            request,
                            context
                    );

            if (result == null) {
                throw ToolExecutionException.invalidResult(
                        request.getToolName(),
                        "Tool returned null."
                );
            }

            return result;

        } catch (ToolExecutionException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            throw ToolExecutionException.executionFailed(
                    request.getToolName(),
                    exception
            );
        }
    }

    /**
     * ToolResult의 최소 유효성을 검증합니다.
     */
    private ToolResult validateResult(
            ToolRequest request,
            ToolResult result) {

        if (result.getStatus() == null) {
            throw ToolExecutionException.invalidResult(
                    request.getToolName(),
                    "Tool result status is missing."
            );
        }

        return result;
    }

    /**
     * Tool 요청 기본값을 검증합니다.
     */
    private void validateRequest(ToolRequest request) {
        if (request == null) {
            throw ToolExecutionException.invalidRequest(
                    "Tool request must not be null."
            );
        }

        String toolName = request.getToolName();

        if (toolName == null || toolName.isBlank()) {
            throw ToolExecutionException.invalidRequest(
                    "Tool name must not be blank."
            );
        }
    }

    /**
     * Tool 검증 성공 여부를 반환합니다.
     */
    private boolean isValidationSuccessful(
            ToolValidationResult result) {

        return result == null || result.isValid();
    }

    /**
     * 요청 검증 실패 결과를 생성합니다.
     */
    private ToolResult createValidationFailureResult(
            ToolRequest request,
            ToolValidationResult validationResult) {

        String message =
                validationResult != null && validationResult.hasMessage()
                        ? validationResult.getMessage()
                        : "Tool request validation failed.";

        return ToolResult.builder()
                .requestId(request.getRequestId())
                .toolCallId(request.getToolCallId())
                .toolName(request.getToolName())
                .status(ToolStatus.VALIDATION_FAILED)
                .validationResult(validationResult)
                .message(message)
                .build();
    }
}