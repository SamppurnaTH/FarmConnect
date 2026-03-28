package com.agrichain.identity.role;

import com.agrichain.common.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public class RoleAssignmentRequest {

    @NotNull(message = "role must not be null")
    private UserRole role;

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}
