package com.agrichain.identity.role;

import com.agrichain.common.enums.UserRole;
import com.agrichain.identity.entity.User;
import com.agrichain.identity.entity.UserStatus;
import com.agrichain.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private UserRepository userRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(userRepository);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private User farmerUser(UUID id) {
        User u = new User();
        u.setId(id);
        u.setUsername("farmer1");
        u.setRole(UserRole.Farmer);
        u.setStatus(UserStatus.Active);
        u.setEmail("farmer@example.com");
        u.setPasswordHash("hashed");
        return u;
    }

    // ── Administrator can assign any role ─────────────────────────────────────

    @Test
    void assignRole_existingUser_updatesRoleAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        User user = farmerUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleAssignmentResponse response = roleService.assignRole(userId, UserRole.Trader);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo("farmer1");
        assertThat(response.getRole()).isEqualTo(UserRole.Trader);
        assertThat(response.getStatus()).isEqualTo(UserStatus.Active);
    }

    @Test
    void assignRole_persistsNewRoleToDatabase() {
        UUID userId = UUID.randomUUID();
        User user = farmerUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        roleService.assignRole(userId, UserRole.Market_Officer);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.Market_Officer);
    }

    @Test
    void assignRole_canAssignAdministratorRole() {
        UUID userId = UUID.randomUUID();
        User user = farmerUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleAssignmentResponse response = roleService.assignRole(userId, UserRole.Administrator);

        assertThat(response.getRole()).isEqualTo(UserRole.Administrator);
    }

    @Test
    void assignRole_canAssignAllRoles() {
        for (UserRole targetRole : UserRole.values()) {
            UUID userId = UUID.randomUUID();
            User user = farmerUser(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            RoleAssignmentResponse response = roleService.assignRole(userId, targetRole);

            assertThat(response.getRole()).isEqualTo(targetRole);
        }
    }

    // ── Unknown userId → 404 ──────────────────────────────────────────────────

    @Test
    void assignRole_unknownUserId_throwsUserNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.assignRole(unknownId, UserRole.Trader))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        verify(userRepository, never()).save(any());
    }
}
