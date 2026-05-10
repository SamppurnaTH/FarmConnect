package com.agrichain.identity.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed token store that tracks active JWT token IDs (JTIs).
 *
 * <p>Replaces the previous in-memory ConcurrentHashMap implementation to support:</p>
 * <ul>
 *   <li>Multi-instance deployments (shared session state)</li>
 *   <li>Graceful restarts (logout state persists)</li>
 *   <li>Automatic TTL expiry aligned with JWT lifetime</li>
 * </ul>
 *
 * <p>TOKEN TTL:</p>
 * The JWT expiry is set in the identity-service config (default: 30 min).
 * We use JWT-expiry-2-minutes as the Redis TTL to allow a small overlap window
 * where a technically-expired JWT is still accepted by downstream services
 * but will be rejected by the token store on refresh attempt.
 *
 * <p>KEY FORMAT:</p>
 * {@code token:{jti}} → username (with TTL)
 *
 * <p>MIGRATION NOTE:</p>
 * On first deployment, existing logged-in users will need to re-login since
 * the in-memory tokens from previous restarts are lost. This is acceptable
 * because JWTs are short-lived by design.
 */
@Component
public class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);
    private static final String KEY_PREFIX = "token:";

    private final StringRedisTemplate redisTemplate;

    public TokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Register a token ID as active, associated with the given username.
     *
     * @param tokenId  the UUID from the JWT JTI claim
     * @param username the authenticated username
     * @param ttlMinutes how long the token remains valid (should align with JWT expiry minus buffer)
     */
    public void store(UUID tokenId, String username, long ttlMinutes) {
        String key = KEY_PREFIX + tokenId;
        try {
            redisTemplate.opsForValue().set(key, username, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to store token {} in Redis: {}", tokenId, e.getMessage());
            throw new RuntimeException("Token store unavailable", e);
        }
    }

    /** Returns true if the token ID is currently active (not invalidated). */
    public boolean isActive(UUID tokenId) {
        String key = KEY_PREFIX + tokenId;
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Failed to check token {} in Redis: {}", tokenId, e.getMessage());
            return false; // Fail-closed: reject token if Redis is unavailable
        }
    }

    /** Invalidate a token by removing it from the store (logout). */
    public void invalidate(UUID tokenId) {
        String key = KEY_PREFIX + tokenId;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to invalidate token {} in Redis: {}", tokenId, e.getMessage());
        }
    }

    /**
     * Remove all tokens for a given user (e.g., on password change or forced logout).
     * This is a best-effort operation since we only store username, not a reverse index.
     */
    public void invalidateAllForUser(String username) {
        // Note: Redis SCAN is used to avoid blocking KEYS in production.
        // This is acceptable for the relatively small token set per user.
        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                byte[] pattern = (KEY_PREFIX + "*").getBytes();
                connection.scan(new org.springframework.data.redis.core.ScanOptions.ScanOptionsBuilder()
                        .match(new String(pattern))
                        .count(100)
                        .build(),
                    (cursor) -> {
                        byte[] key = cursor.getKey();
                        String storedUsername = new String(redisTemplate.opsForValue().get(key));
                        if (username.equals(storedUsername)) {
                            redisTemplate.delete(new String(key));
                        }
                    });
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to bulk-invalidate tokens for user {}: {}", username, e.getMessage());
        }
    }
}
