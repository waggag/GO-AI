package com.goaiplatform.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private long capacity = 100;
    private long refillTokens = 100;
    private long refillDurationSeconds = 1;
    private long streamCapacity = 20;
    private long streamRefillTokens = 20;
    private long streamRefillDurationSeconds = 1;

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public void setRefillTokens(long refillTokens) {
        this.refillTokens = refillTokens;
    }

    public long getRefillDurationSeconds() {
        return refillDurationSeconds;
    }

    public void setRefillDurationSeconds(long refillDurationSeconds) {
        this.refillDurationSeconds = refillDurationSeconds;
    }

    public long getStreamCapacity() {
        return streamCapacity;
    }

    public void setStreamCapacity(long streamCapacity) {
        this.streamCapacity = streamCapacity;
    }

    public long getStreamRefillTokens() {
        return streamRefillTokens;
    }

    public void setStreamRefillTokens(long streamRefillTokens) {
        this.streamRefillTokens = streamRefillTokens;
    }

    public long getStreamRefillDurationSeconds() {
        return streamRefillDurationSeconds;
    }

    public void setStreamRefillDurationSeconds(long streamRefillDurationSeconds) {
        this.streamRefillDurationSeconds = streamRefillDurationSeconds;
    }
}
