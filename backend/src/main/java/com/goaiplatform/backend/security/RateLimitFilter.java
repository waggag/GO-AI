package com.goaiplatform.backend.security;

import com.goaiplatform.backend.config.RateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
@Order(-2)
public class RateLimitFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    private final RateLimitProperties rateLimitProperties;
    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitFilter(RateLimitProperties rateLimitProperties, ReactiveStringRedisTemplate redisTemplate) {
        this.rateLimitProperties = rateLimitProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        boolean streamPath = path.contains("/api/chat/stream");
        
        String key = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "anonymous";
        
        String scopedKey = RATE_LIMIT_KEY_PREFIX + key + "|" + (streamPath ? "stream" : "common");
        
        long capacity = streamPath ? rateLimitProperties.getStreamCapacity() : rateLimitProperties.getCapacity();
        long refillTokens = streamPath ? rateLimitProperties.getStreamRefillTokens() : rateLimitProperties.getRefillTokens();
        Duration refillDuration = Duration.ofSeconds(streamPath ? rateLimitProperties.getStreamRefillDurationSeconds() : rateLimitProperties.getRefillDurationSeconds());
        
        return checkAndConsumeToken(scopedKey, capacity, refillTokens, refillDuration)
                .flatMap(allowed -> {
                    if (allowed) {
                        exchange.getResponse().getHeaders().add("X-RateLimit-Scope", streamPath ? "stream" : "common");
                        return chain.filter(exchange);
                    } else {
                        log.warn("Rate limit exceeded: key={}", scopedKey);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                });
    }

    private Mono<Boolean> checkAndConsumeToken(String key, long capacity, long refillTokens, Duration refillDuration) {
        return redisTemplate.opsForValue().get(key)
                .map(lastRefillTime -> {
                    Instant lastRefill = Instant.parse(lastRefillTime);
                    long intervals = Duration.between(lastRefill, Instant.now()).toMillis() / refillDuration.toMillis();
                    return intervals;
                })
                .defaultIfEmpty(0L)
                .flatMap(intervals -> {
                    if (intervals > 0) {
                        // Reset tokens after refill
                        return redisTemplate.opsForValue()
                                .set(key, Instant.now().toString(), refillDuration.multipliedBy(2))
                                .thenReturn(true);
                    } else {
                        // Check if we have tokens available
                        return redisTemplate.opsForValue()
                                .decrement(key + ":tokens")
                                .map(tokens -> tokens >= 0)
                                .defaultIfEmpty(false);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Redis error during rate limiting: {}", e.getMessage());
                    return Mono.just(true); // Allow on error
                });
    }
}
