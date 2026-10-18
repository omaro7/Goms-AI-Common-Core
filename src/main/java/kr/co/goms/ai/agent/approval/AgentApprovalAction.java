/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.agent.approval;

/**
 * Agent 승인 Action 생성 및 해석 유틸리티. Tool Name을 기반으로 Approval Action으로 명칭한다.
 * Tool Name                  Approval Action
 * --------------------------------------------------
 * create_basic_xhtml         approve_create_basic_xhtml
 * create_epub_project        approve_create_epub_project
 * delete_epub_xhtml          approve_delete_epub_xhtml
 * update_epub_metadata       approve_update_epub_metadata
 * 
 */
public final class AgentApprovalAction {

    public static final String PREFIX =
            "approve_";


    private AgentApprovalAction() {
    }


    public static String of(
            String toolName) {

        String value =
                requireText(
                        toolName,
                        "toolName");

        return PREFIX
                + value;
    }


    public static String getToolName(
            String action) {

        String value =
                requireText(
                        action,
                        "action");


        if (!value.startsWith(
                PREFIX)) {

            throw new IllegalArgumentException(
                    "Invalid approval action: "
                            + value);
        }


        String toolName =
                value.substring(
                        PREFIX.length());


        if (toolName.isBlank()) {

            throw new IllegalArgumentException(
                    "Approval action does not contain tool name: "
                            + value);
        }


        return toolName;
    }


    public static boolean isApprovalAction(
            String action) {

        if (action == null
                || action.isBlank()) {

            return false;
        }


        String value =
                action.trim();


        return value.startsWith(
                PREFIX)
                && value.length()
                        > PREFIX.length();
    }


    private static String requireText(
            String value,
            String name) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    name
                            + " must not be blank.");
        }

        return value.trim();
    }
}