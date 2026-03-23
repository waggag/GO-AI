package com.goaiplatform.backend.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ConversationMemoryService {

    private final ReactiveStringRedisTemplate redisTemplate;

    public ConversationMemoryService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Flux<String> loadContext(Long conversationId) {
        return redisTemplate.opsForList().range(key(conversationId), 0, -1);
    }

    public Mono<Void> appendContext(Long conversationId, List<String> messages) {
        return redisTemplate.opsForList().rightPushAll(key(conversationId), messages)
                .then(redisTemplate.opsForList().trim(key(conversationId), -20, -1))
                .then();
    }

    private String key(Long conversationId) {
        return "conv:ctx:" + conversationId;
    }
}
