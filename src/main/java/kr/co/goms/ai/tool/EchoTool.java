/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.tool;

import java.util.List;
import java.util.Map;

/**
 * Tool Calling 동작 검증을 위한 Echo Tool입니다.
 *
 * <p>
 * 입력된 message 값을 그대로 반환합니다.
 * </p>
 *
 * <p>
 * 이 Tool은 GomsBook AI Agent의
 * LLM Tool Calling 파이프라인을 검증하기 위한
 * 테스트용 Tool입니다.
 * </p>
 */
public final class EchoTool implements AgentTool {

    private static final String TOOL_NAME = "echo";

    private static final String ARG_MESSAGE = "message";


    @Override
    public String getName() {

        return TOOL_NAME;
    }


    @Override
    public String getDescription() {

        return "입력된 문자열을 그대로 반환하는 "
                + "Tool Calling 테스트용 도구입니다.";
    }


    @Override
    public Map<String, Object> getInputSchema() {

        return Map.of(
                "type",
                "object",

                "properties",
                Map.of(
                        ARG_MESSAGE,
                        Map.of(
                                "type",
                                "string",
                                "description",
                                "그대로 반환할 문자열"
                        )
                ),

                "required",
                List.of(
                        ARG_MESSAGE
                )
        );
    }


    @Override
    public ToolValidationResult validate(
            ToolRequest request,
            ToolContext context) {

        if (request == null) {

            return ToolValidationResult.invalid(
                    "Tool request must not be null."
            );
        }


        try {

            request.requireStringArgument(
                    ARG_MESSAGE
            );

            return ToolValidationResult.valid();

        } catch (IllegalArgumentException exception) {

            return ToolValidationResult.invalid(
                    exception.getMessage()
            );
        }
    }


    @Override
    public ToolResult execute(
            ToolRequest request,
            ToolContext context) {

        ToolValidationResult validation =
                validate(
                        request,
                        context
                );


        if (validation.isInvalid()) {

            String errorMessage =
                    validation.hasMessage()
                            ? validation.getMessage()
                            : "Echo Tool validation failed.";


            return ToolResult.failure(
                    getName(),
                    errorMessage
            )
            .requestId(
                    request != null
                            ? request.getRequestId()
                            : null
            )
            .toolCallId(
                    request != null
                            ? request.getToolCallId()
                            : null
            )
            .validationResult(
                    validation
            )
            .build();
        }


        String message =
                request.requireStringArgument(
                        ARG_MESSAGE
                );


        return ToolResult.success(
                    getName()
            )
            .requestId(
                    request.getRequestId()
            )
            .toolCallId(
                    request.getToolCallId()
            )
            .message(
                    "Echo Tool executed successfully."
            )
            .data(
                    "message",
                    message
            )
            .build();
    }
}