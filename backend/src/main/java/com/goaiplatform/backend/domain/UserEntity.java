package com.goaiplatform.backend.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("users")
public record UserEntity(
        @Id Long id,
        String username,
        @Column("password_hash") String passwordHash,
        String role,
        @Column("created_at") LocalDateTime createdAt
) {
}
