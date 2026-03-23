package com.goaiplatform.backend.repository;

import com.goaiplatform.backend.domain.MessageEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends ReactiveCrudRepository<MessageEntity, Long> {
    Flux<MessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    
    /**
     * 批量查询多个会话的消息，避免 N+1 查询问题
     */
    Flux<MessageEntity> findByConversationIdInOrderByConversationIdAndCreatedAtAsc(List<Long> conversationIds);
    
    /**
     * 统计指定会话中指定角色的消息数量
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id IN (:conversationIds) AND role = :role")
    Mono<Long> countByConversationIdInAndRole(List<Long> conversationIds, String role);
    
    /**
     * 统计指定会话中指定角色的消息数量（在指定时间之后）
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id IN (:conversationIds) AND role = :role AND created_at >= :startTime")
    Mono<Long> countByConversationIdInAndRoleAndCreatedAtAfter(List<Long> conversationIds, String role, LocalDateTime startTime);
    
    /**
     * 统计指定会话中的消息数量
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id IN (:conversationIds)")
    Mono<Long> countByConversationIdIn(List<Long> conversationIds);
    
    /**
     * 统计指定会话中的消息数量（在指定时间之后）
     */
    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id IN (:conversationIds) AND created_at >= :startTime")
    Mono<Long> countByConversationIdInAndCreatedAtAfter(List<Long> conversationIds, LocalDateTime startTime);
}
