package com.agrichain.identity.role;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * PUT /roles/{userId} — Administrator-only role assignment.
 *
 * The new role is persisted immediately so that any subsequent JWT validation
 * reflects the change within the token-store TTL, satisfying the
 * "within 1 minute" requirement (2.4).
 */
@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * PUT /roles/{userId}
     *
     * Assigns a new role to the specified user.
     * Only accessible to users with the Administrator role.
     *
     * @return 200 with updated user summary on success
     * @return 403 if the caller is not an Administrator (enforced by @PreAuthorize)
     * @return 404 if the userId does not exist
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('Administrator')")
    public ResponseEntity<RoleAssignmentResponse> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody RoleAssignmentRequest request) {
        RoleAssignmentResponse response = roleService.assignRole(userId, request.getRole());
        return ResponseEntity.ok(response);
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(Map.of("error", ex.getMessage()));
    }
}
