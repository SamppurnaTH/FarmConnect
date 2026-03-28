package com.agrichain.identity.role;

import com.agrichain.common.enums.UserRole;
import com.agrichain.identity.audit.AuditLogService;
import com.agrichain.identity.config.SecurityConfig;
import com.agrichain.identity.entity.UserStatus;
import com.agrichain.identity.security.JwtService;
import com.agrichain.identity.security.TokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
@Import(SecurityConfig.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoleService roleService;

    // Prevent Spring from trying to instantiate these beans with the real secret
    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenStore tokenStore;

    // AuditLogController is also loaded in WebMvcTest context; mock its dependency
    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private com.agrichain.identity.notification.EmailNotificationClient emailNotificationClient;

    // AuthController is also loaded; mock its service dependency
    @MockBean
    private com.agrichain.identity.auth.AuthService authService;

    // JwtAuthenticationFilter requires UserRepository; mock it for WebMvcTest
    @MockBean
    private com.agrichain.identity.repository.UserRepository userRepository;

    // ── Administrator can assign any role → 200 ───────────────────────────────

    @Test
    @WithMockUser(roles = "Administrator")
    void assignRole_asAdministrator_returns200WithUpdatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        RoleAssignmentResponse response = new RoleAssignmentResponse(
                userId, "farmer1", UserRole.Trader, UserStatus.Active, Instant.now());

        when(roleService.assignRole(userId, UserRole.Trader)).thenReturn(response);

        Map<String, String> body = Map.of("role", "Trader");

        mockMvc.perform(put("/roles/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("farmer1"))
                .andExpect(jsonPath("$.role").value("Trader"))
                .andExpect(jsonPath("$.status").value("Active"));
    }

    @Test
    @WithMockUser(roles = "Administrator")
    void assignRole_asAdministrator_canAssignAdministratorRole() throws Exception {
        UUID userId = UUID.randomUUID();
        RoleAssignmentResponse response = new RoleAssignmentResponse(
                userId, "user1", UserRole.Administrator, UserStatus.Active, Instant.now());

        when(roleService.assignRole(userId, UserRole.Administrator)).thenReturn(response);

        Map<String, String> body = Map.of("role", "Administrator");

        mockMvc.perform(put("/roles/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Administrator"));
    }

    // ── Non-Administrator → 403 ───────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "Farmer")
    void assignRole_asFarmer_returns403() throws Exception {
        UUID userId = UUID.randomUUID();
        Map<String, String> body = Map.of("role", "Trader");

        mockMvc.perform(put("/roles/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roleService);
    }

    @Test
    @WithMockUser(roles = "Trader")
    void assignRole_asTrader_returns403() throws Exception {
        UUID userId = UUID.randomUUID();
        Map<String, String> body = Map.of("role", "Farmer");

        mockMvc.perform(put("/roles/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roleService);
    }

    @Test
    @WithMockUser(roles = "Market_Officer")
    void assignRole_asMarketOfficer_returns403() throws Exception {
        UUID userId = UUID.randomUUID();
        Map<String, String> body = Map.of("role", "Farmer");

        mockMvc.perform(put("/roles/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roleService);
    }

    // ── Unknown userId → 404 ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "Administrator")
    void assignRole_unknownUserId_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(roleService.assignRole(unknownId, UserRole.Trader))
                .thenThrow(new UserNotFoundException(unknownId));

        Map<String, String> body = Map.of("role", "Trader");

        mockMvc.perform(put("/roles/{userId}", unknownId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: " + unknownId));
    }

    // ── Missing/invalid role in request body → 400 ───────────────────────────

    @Test
    @WithMockUser(roles = "Administrator")
    void assignRole_missingRoleField_returns400() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/roles/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roleService);
    }

    // ── Unauthenticated request → 403 ────────────────────────────────────────

    @Test
    void assignRole_unauthenticated_returns403() throws Exception {
        UUID userId = UUID.randomUUID();
        Map<String, String> body = Map.of("role", "Trader");

        mockMvc.perform(put("/roles/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roleService);
    }
}
