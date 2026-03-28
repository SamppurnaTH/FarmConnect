package com.agrichain.identity.role;

import com.agrichain.common.enums.UserRole;
import com.agrichain.identity.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public class RoleAssignmentResponse {

    private UUID id;
    private String username;
    private UserRole role;
    private UserStatus status;
    private Instant updatedAt;

    public RoleAssignmentResponse(UUID id, String username, UserRole role,
                                  UserStatus status, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
}
