package com.agrichain.identity.auth;

import com.agrichain.common.enums.UserRole;
import java.time.Instant;
import java.util.UUID;

public class LoginResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final UserRole role;
    private final UUID userId;
    private final Instant expiresAt;

    public LoginResponse(String token, UserRole role, UUID userId, Instant expiresAt) {
        this.token = token;
        this.role = role;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public UserRole getRole() { return role; }
    public UUID getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
}
