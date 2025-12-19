package com.mouli.consumer.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
public class IdempotencyService {
    private static final String PREFIX = "processed:";
    private static final Duration CLAIM_EXPIRY = Duration.ofMinutes(5);
    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('del', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> releaseScript;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
        this.releaseScript = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
    }

    /**
     * Try to claim the message for processing.
     * Returns a claim token (non-null) if we successfully claimed; null otherwise.
     */
    public String claimProcessing(String messageId) {
        String key = PREFIX + messageId;
        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(key, token, CLAIM_EXPIRY);
        return Boolean.TRUE.equals(ok) ? token : null;
    }

    /**
     * Mark final processed state (overwrite any PROCESSING token).
     * This makes the key present (value "PROCESSED") so duplicates are ignored.
     */
    public void markProcessed(String messageId) {
        String key = PREFIX + messageId;
        redis.opsForValue().set(key, "PROCESSED");
        // optionally remove expiry by re-setting persist depending on Redis client; leaving as-is is fine
    }

    /**
     * Release a claim only if the token matches (safe delete).
     * Returns true if the key was deleted.
     */
    public boolean releaseClaim(String messageId, String token) {
        if (token == null) return false;
        String key = PREFIX + messageId;
        Long res = redis.execute(releaseScript, Collections.singletonList(key), token);
        return res != null && res > 0;
    }

    /**
     * Check whether the message is already processed (exists in state store).
     */
    public boolean isProcessed(String messageId) {
        String key = PREFIX + messageId;
        return Boolean.TRUE.equals(redis.hasKey(key) && "PROCESSED".equals(redis.opsForValue().get(key)));
    }
}
