package com.goaiplatform.backend.repository;

import com.goaiplatform.backend.domain.ApiKeyEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApiKeyRepository extends ReactiveCrudRepository<ApiKeyEntity, Long> {
    Mono<ApiKeyEntity> findFirstByUserIdAndProviderAndActiveTrueOrderByVersionDesc(Long userId, String provider);

    Flux<ApiKeyEntity> findByUserIdAndProviderOrderByVersionDesc(Long userId, String provider);
}
