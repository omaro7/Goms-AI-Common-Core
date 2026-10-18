/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.conversation.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.goms.ai.conversation.exception.ConversationProjectMismatchException;
import kr.co.goms.ai.conversation.model.AiConversation;
import kr.co.goms.ai.conversation.model.AiConversationMessage;
import kr.co.goms.ai.conversation.model.AiConversationMessageRole;
import kr.co.goms.ai.conversation.model.ConversationHistoryMessage;
import kr.co.goms.ai.conversation.repository.AiConversationMessageRepository;
import kr.co.goms.ai.conversation.repository.AiConversationRepository;

@Service
public class ConversationService {

    private static final String WORKSPACE_PROJECT_ID = "__workspace__";

    private final AiConversationRepository conversationRepository;

    private final AiConversationMessageRepository messageRepository;

    public ConversationService(
            AiConversationRepository conversationRepository,
            AiConversationMessageRepository messageRepository) {

        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public AiConversation getOrCreateConversation(
            String projectId,
            String conversationId) {

        validateProjectId(projectId);
        validateConversationId(conversationId);

        AiConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElse(null);

        if (conversation != null) {

            validateConversationProject(
                    conversation,
                    projectId
            );

            conversation.setUpdatedAt(
                    LocalDateTime.now()
            );

            conversationRepository.updateProjectId(conversationId, projectId);

            return conversation;
        }

        LocalDateTime now =
                LocalDateTime.now();

        AiConversation newConversation =
                new AiConversation(
                        conversationId,
                        projectId,
                        null,
                        "ACTIVE",
                        now,
                        now
                );

        conversationRepository.save(
                newConversation
        );

        return newConversation;
    }

    @Transactional
    public AiConversation promoteWorkspaceConversation(
            String conversationId,
            String projectId) {

        validateConversationId(conversationId);
        validateProjectId(projectId);

        String normalizedConversationId =
                conversationId.trim();

        String normalizedProjectId =
                projectId.trim();

        if (WORKSPACE_PROJECT_ID.equals(normalizedProjectId)) {

            throw new IllegalArgumentException(
                    "Target projectId must not be __workspace__."
            );
        }

        AiConversation conversation =
                conversationRepository
                        .findById(normalizedConversationId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Conversation not found: "
                                                + normalizedConversationId
                                )
                        );

        String currentProjectId =
                conversation.getProjectId();

        if (Objects.equals(
                currentProjectId,
                normalizedProjectId)) {

            return conversation;
        }

        if (!WORKSPACE_PROJECT_ID.equals(
                currentProjectId)) {

            throw new ConversationProjectMismatchException(
                    "Conversation project promotion is allowed only from __workspace__."
                            + " conversationId="
                            + normalizedConversationId
                            + ", conversationProjectId="
                            + currentProjectId
                            + ", targetProjectId="
                            + normalizedProjectId
            );
        }

        conversationRepository.updateProjectId(normalizedConversationId, normalizedProjectId);

        AiConversation updatedConversation =
                conversationRepository
                        .findById(normalizedConversationId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Conversation disappeared after project promotion: "
                                                + normalizedConversationId
                                )
                        );

        if (!Objects.equals(
                updatedConversation.getProjectId(),
                normalizedProjectId)) {

            throw new IllegalStateException(
                    "Conversation project promotion was not persisted."
                            + " conversationId="
                            + normalizedConversationId
                            + ", expectedProjectId="
                            + normalizedProjectId
                            + ", actualProjectId="
                            + updatedConversation.getProjectId()
            );
        }

        return updatedConversation;
    }

    @Transactional
    public AiConversationMessage addUserMessage(
            String conversationId,
            String runId,
            String content) {

        return addMessage(
                conversationId,
                runId,
                AiConversationMessageRole.USER,
                content
        );
    }

    @Transactional
    public AiConversationMessage addAssistantMessage(
            String conversationId,
            String runId,
            String content) {

        return addMessage(
                conversationId,
                runId,
                AiConversationMessageRole.ASSISTANT,
                content
        );
    }

    @Transactional
    public AiConversationMessage addSystemMessage(
            String conversationId,
            String runId,
            String content) {

        return addMessage(
                conversationId,
                runId,
                AiConversationMessageRole.SYSTEM,
                content
        );
    }

    @Transactional
    public AiConversationMessage addToolMessage(
            String conversationId,
            String runId,
            String content) {

        return addMessage(
                conversationId,
                runId,
                AiConversationMessageRole.TOOL,
                content
        );
    }

    public List<ConversationHistoryMessage> getConversationHistory(
            String conversationId,
            String currentRunId) {

        validateConversationId(conversationId);

        return messageRepository
                .findByConversationId(conversationId)
                .stream()
                .filter(message -> !isCurrentRunMessage(message, currentRunId))
                .filter(this::isLlmHistoryMessage)
                .map(message -> new ConversationHistoryMessage(
                        message.getRole(),
                        message.getContent()
                ))
                .toList();
    }

    private boolean isCurrentRunMessage(
            AiConversationMessage message,
            String currentRunId) {

        if (currentRunId == null || currentRunId.isBlank()) return false;

        return currentRunId.equals(message.getRunId());
    }

    private boolean isLlmHistoryMessage(
            AiConversationMessage message) {

        return message.getRole() == AiConversationMessageRole.USER
                || message.getRole() == AiConversationMessageRole.ASSISTANT;
    }
    
    private AiConversationMessage addMessage(
            String conversationId,
            String runId,
            AiConversationMessageRole role,
            String content) {

        validateConversationId(
                conversationId
        );

        validateContent(
                content
        );

        long sequenceNo =
                messageRepository
                        .findNextSequenceNo(
                                conversationId
                        );

        AiConversationMessage message =
                new AiConversationMessage(
                        UUID.randomUUID().toString(),
                        conversationId,
                        normalizeRunId(runId),
                        role,
                        content.trim(),
                        sequenceNo,
                        LocalDateTime.now()
                );

        messageRepository.save(
                message
        );

        touchConversation(
                conversationId
        );

        return message;
    }

    private void touchConversation(
            String conversationId) {

        AiConversation conversation =
                conversationRepository
                        .findById(
                                conversationId
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Conversation not found: "
                                                + conversationId
                                )
                        );

        conversation.setUpdatedAt(
                LocalDateTime.now()
        );

        conversationRepository.update(
                conversation
        );
    }

    private void validateConversationProject(
            AiConversation conversation,
            String projectId) {

        if (Objects.equals(conversation.getProjectId(), projectId)) {
            return;
        }

        throw new ConversationProjectMismatchException(
                "Conversation project does not match."
                        + " conversationId="
                        + conversation.getConversationId()
                        + ", conversationProjectId="
                        + conversation.getProjectId()
                        + ", requestProjectId="
                        + projectId
        );
    }

    private void validateProjectId(
            String projectId) {

        if (
                projectId == null
                        || projectId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "projectId must not be blank."
            );
        }
    }

    private void validateConversationId(
            String conversationId) {

        if (
                conversationId == null
                        || conversationId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "conversationId must not be blank."
            );
        }
    }

    private void validateContent(
            String content) {

        if (
                content == null
                        || content.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "message content must not be blank."
            );
        }
    }

    private String normalizeRunId(
            String runId) {

        if (
                runId == null
                        || runId.isBlank()
        ) {
            return null;
        }

        return runId.trim();
    }
}