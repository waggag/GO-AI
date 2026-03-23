package com.goaiplatform.backend.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("messages")
public record MessageEntity(
        @Id Long id,
        @Column("conversation_id") Long conversationId,
        String role,
        String content,
        String model,
        @Column("created_at") LocalDateTime createdAt
) {
}
