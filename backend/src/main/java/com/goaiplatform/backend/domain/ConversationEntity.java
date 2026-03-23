package com.goaiplatform.backend.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("conversations")
public record ConversationEntity(
        @Id Long id,
        @Column("user_id") Long userId,
        String title,
        @Column("created_at") LocalDateTime createdAt
) {
}
