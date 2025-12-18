package com.mouli.consumer.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {
    private static final String PREFIX = "processed:";
    private static final Duration CLAIM_EXPIRY = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Try to claim the message for processing.
     * Returns true if we successfully claimed (not previously processed/claimed).
     */
    public boolean claimProcessing(String messageId) {
        String key = PREFIX + messageId;
        Boolean ok = redis.opsForValue().setIfAbsent(key, "PROCESSING", CLAIM_EXPIRY);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * Mark final processed state (overwrite any PROCESSING)
     */
    public void markProcessed(String messageId) {
        String key = PREFIX + messageId;
        redis.opsForValue().set(key, "PROCESSED");
        // optionally make permanent (no expiry) or set a long expiry
    }

    public boolean isProcessed(String messageId) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + messageId));
    }
}
