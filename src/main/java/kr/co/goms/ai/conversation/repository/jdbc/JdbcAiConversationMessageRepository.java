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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import kr.co.goms.ai.conversation.model.AiConversationMessage;
import kr.co.goms.ai.conversation.model.AiConversationMessageRole;
import kr.co.goms.ai.conversation.repository.AiConversationMessageRepository;

@Repository
public class JdbcAiConversationMessageRepository implements AiConversationMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAiConversationMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(AiConversationMessage message) {

        jdbcTemplate.update(
                """
                INSERT INTO ai_conversation_message (
                    message_id,
                    conversation_id,
                    run_id,
                    role,
                    content,
                    sequence_no,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                message.getMessageId(),
                message.getConversationId(),
                message.getRunId(),
                message.getRole().name(),
                message.getContent(),
                message.getSequenceNo(),
                message.getCreatedAt()
        );
    }

    @Override
    public List<AiConversationMessage> findByConversationId(String conversationId) {

        return jdbcTemplate.query(
                """
                SELECT
                    message_id,
                    conversation_id,
                    run_id,
                    role,
                    content,
                    sequence_no,
                    created_at
                FROM ai_conversation_message
                WHERE conversation_id = ?
                ORDER BY sequence_no ASC
                """,
                this::mapMessage,
                conversationId
        );
    }

    @Override
    public long findNextSequenceNo(String conversationId) {

        Long sequenceNo =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COALESCE(MAX(sequence_no), 0) + 1
                        FROM ai_conversation_message
                        WHERE conversation_id = ?
                        """,
                        Long.class,
                        conversationId
                );

        return sequenceNo == null ? 1L : sequenceNo;
    }

    private AiConversationMessage mapMessage(
            ResultSet resultSet,
            int rowNum) throws SQLException {

        return new AiConversationMessage(
                resultSet.getString("message_id"),
                resultSet.getString("conversation_id"),
                resultSet.getString("run_id"),
                AiConversationMessageRole.valueOf(resultSet.getString("role")),
                resultSet.getString("content"),
                resultSet.getLong("sequence_no"),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        );
    }
}