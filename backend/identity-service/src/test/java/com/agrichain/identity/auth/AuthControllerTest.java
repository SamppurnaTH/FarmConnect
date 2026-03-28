package com.agrichain.identity.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agrichain.identity.audit.AuditLogService;
import com.agrichain.identity.config.SecurityConfig;
import com.agrichain.identity.security.JwtService;
import com.agrichain.identity.security.TokenStore;
import com.agrichain.identity.notification.EmailNotificationClient;
import io.jsonwebtoken.JwtException;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Prevent Spring from trying to instantiate these beans with the real secret
    @MockBean
    private JwtService jwtService;

    @MockBean
    private TokenStore tokenStore;

    @MockBean
    private EmailNotificationClient emailNotificationClient;

    // AuditLogController is also loaded in WebMvcTest context; mock its dependency
    @MockBean
    private AuditLogService auditLogService;

    // JwtAuthenticationFilter requires UserRepository; mock it for WebMvcTest
    @MockBean
    private com.agrichain.identity.repository.UserRepository userRepository;

    @org.junit.jupiter.api.BeforeEach
    void stubJwtFilter() {
        // JwtAuthenticationFilter calls jwtService.parse() for any Bearer token.
        // Throw JwtException so the filter skips auth and lets the controller handle it.
        org.mockito.Mockito.when(jwtService.parse(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new JwtException("stub"));
    }

    // ── Valid credentials → 200 with token ───────────────────────────────────

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        when(authService.login("farmer1", "secret123")).thenReturn("signed.jwt.token");

        LoginRequest req = new LoginRequest();
        req.setUsername("farmer1");
        req.setPassword("secret123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // ── Wrong password → 401 generic message ─────────────────────────────────

    @Test
    void login_wrongPassword_returns401GenericMessage() throws Exception {
        when(authService.login("farmer1", "wrong")).thenThrow(new AuthenticationFailedException());

        LoginRequest req = new LoginRequest();
        req.setUsername("farmer1");
        req.setPassword("wrong");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(AuthService.AUTH_FAILED_MESSAGE));
    }

    // ── Unknown username → 401 same generic message ───────────────────────────

    @Test
    void login_unknownUsername_returns401GenericMessage() throws Exception {
        when(authService.login("nobody", "pass")).thenThrow(new AuthenticationFailedException());

        LoginRequest req = new LoginRequest();
        req.setUsername("nobody");
        req.setPassword("pass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(AuthService.AUTH_FAILED_MESSAGE));
    }

    // ── Locked account → 401 same generic message ────────────────────────────

    @Test
    void login_lockedAccount_returns401GenericMessage() throws Exception {
        when(authService.login("locked", "secret123")).thenThrow(new AuthenticationFailedException());

        LoginRequest req = new LoginRequest();
        req.setUsername("locked");
        req.setPassword("secret123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(AuthService.AUTH_FAILED_MESSAGE));
    }

    // ── Missing fields → 400 (bean validation) ────────────────────────────────

    @Test
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── logout: valid token → 204 ─────────────────────────────────────────────

    @Test
    void logout_validToken_returns204() throws Exception {
        doNothing().when(authService).logout("valid.jwt.token");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer valid.jwt.token"))
                .andExpect(status().isNoContent());

        verify(authService).logout("valid.jwt.token");
    }

    // ── logout: invalidated token → 401 ──────────────────────────────────────

    @Test
    void logout_invalidatedToken_returns401() throws Exception {
        doThrow(new InvalidTokenException()).when(authService).logout("old.jwt.token");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer old.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(InvalidTokenException.INVALID_TOKEN_MESSAGE));
    }

    // ── logout: missing Authorization header → 400 (required header missing) ──

    @Test
    void logout_missingAuthHeader_returns400() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isBadRequest());
    }

    // ── refresh: valid token → 200 with new token ─────────────────────────────

    @Test
    void refresh_validToken_returns200WithNewToken() throws Exception {
        when(authService.refresh("old.jwt.token")).thenReturn("new.jwt.token");

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer old.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // ── refresh: invalid/expired token → 401 ─────────────────────────────────

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        doThrow(new InvalidTokenException()).when(authService).refresh("expired.jwt.token");

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer expired.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value(InvalidTokenException.INVALID_TOKEN_MESSAGE));
    }

    // ── using invalidated token after logout → 401 ────────────────────────────

    @Test
    void refresh_afterLogout_returns401() throws Exception {
        // Simulate: token was invalidated by logout, so refresh also rejects it
        doThrow(new InvalidTokenException()).when(authService).refresh("logged.out.token");

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer logged.out.token"))
                .andExpect(status().isUnauthorized());
    }
}
