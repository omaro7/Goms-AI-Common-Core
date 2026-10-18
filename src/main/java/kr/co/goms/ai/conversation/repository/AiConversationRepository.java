/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.conversation.repository;

import java.util.List;
import java.util.Optional;

import kr.co.goms.ai.conversation.model.AiConversation;

public interface AiConversationRepository {

    void save(AiConversation conversation);

    Optional<AiConversation> findById(String conversationId);

    List<AiConversation> findByProjectId(String projectId);

    void update(AiConversation conversation);
    
    void updateProjectId(String conversationId, String projectId);
}