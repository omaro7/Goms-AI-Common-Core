/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.conversation.repository.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import kr.co.goms.ai.conversation.model.AiConversation;
import kr.co.goms.ai.conversation.repository.AiConversationRepository;

@Repository
public class JdbcAiConversationRepository implements AiConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(AiConversation conversation) {

        jdbcTemplate.update(
                """
                INSERT INTO ai_conversation (
                    conversation_id,
                    project_id,
                    title,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                conversation.getConversationId(),
                conversation.getProjectId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    @Override
    public Optional<AiConversation> findById(String conversationId) {

        List<AiConversation> results =
                jdbcTemplate.query(
                        """
                        SELECT
                            conversation_id,
                            project_id,
                            title,
                            status,
                            created_at,
                            updated_at
                        FROM ai_conversation
                        WHERE conversation_id = ?
                        """,
                        this::mapConversation,
                        conversationId
                );

        return results.stream().findFirst();
    }

    @Override
    public List<AiConversation> findByProjectId(String projectId) {

        return jdbcTemplate.query(
                """
                SELECT
                    conversation_id,
                    project_id,
                    title,
                    status,
                    created_at,
                    updated_at
                FROM ai_conversation
                WHERE project_id = ?
                ORDER BY updated_at DESC
                """,
                this::mapConversation,
                projectId
        );
    }

    @Override
    public void update(AiConversation conversation) {

        jdbcTemplate.update(
                """
                UPDATE ai_conversation
                SET
                    title = ?,
                    status = ?,
                    updated_at = ?
                WHERE conversation_id = ?
                """,
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getUpdatedAt(),
                conversation.getConversationId()
        );
    }
    
    @Override
    public void updateProjectId(
            String conversationId,
            String projectId) {

        jdbcTemplate.update(
                """
                UPDATE ai_conversation
                SET
                    project_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ?
                """,
                projectId,
                conversationId
        );
    }

    private AiConversation mapConversation(
            ResultSet resultSet,
            int rowNum) throws SQLException {

        return new AiConversation(
                resultSet.getString("conversation_id"),
                resultSet.getString("project_id"),
                resultSet.getString("title"),
                resultSet.getString("status"),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}