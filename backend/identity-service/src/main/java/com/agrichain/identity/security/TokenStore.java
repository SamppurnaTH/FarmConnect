package com.agrichain.identity.security;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token store that tracks active token IDs (JTIs).
 *
 * Tokens are added on login and removed on logout or expiry,
 * enabling immediate invalidation without waiting for JWT expiry.
 *
 * For production, replace with a Redis-backed implementation.
 */
@Component
public class TokenStore {

    private final ConcurrentHashMap<UUID, String> activeTokens = new ConcurrentHashMap<>();

    /** Register a token ID as active, associated with the given username. */
    public void store(UUID tokenId, String username) {
        activeTokens.put(tokenId, username);
    }

    /** Returns true if the token ID is currently active (not invalidated). */
    public boolean isActive(UUID tokenId) {
        return activeTokens.containsKey(tokenId);
    }

    /** Invalidate a token by removing it from the store. */
    public void invalidate(UUID tokenId) {
        activeTokens.remove(tokenId);
    }
}
