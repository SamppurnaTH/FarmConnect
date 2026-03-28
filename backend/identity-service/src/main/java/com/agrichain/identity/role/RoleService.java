package com.agrichain.identity.role;

import com.agrichain.common.enums.UserRole;
import com.agrichain.identity.entity.User;
import com.agrichain.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles Administrator-only role assignment.
 *
 * The new role is persisted immediately so that any subsequent JWT validation
 * (which re-reads the user's role from the database) reflects the change within
 * the token-store TTL — satisfying the "within 1 minute" requirement (2.4).
 */
@Service
public class RoleService {

    private final UserRepository userRepository;

    public RoleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Assigns {@code newRole} to the user identified by {@code userId}.
     *
     * @param userId  target user's UUID
     * @param newRole role to assign
     * @return updated user summary
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Transactional
    public RoleAssignmentResponse assignRole(UUID userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setRole(newRole);
        User saved = userRepository.save(user);

        return new RoleAssignmentResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getRole(),
                saved.getStatus(),
                saved.getUpdatedAt()
        );
    }
}
