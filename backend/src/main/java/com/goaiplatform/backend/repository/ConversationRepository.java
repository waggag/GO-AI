package com.goaiplatform.backend.repository;

import com.goaiplatform.backend.domain.ConversationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ConversationRepository extends ReactiveCrudRepository<ConversationEntity, Long> {
    Flux<ConversationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
