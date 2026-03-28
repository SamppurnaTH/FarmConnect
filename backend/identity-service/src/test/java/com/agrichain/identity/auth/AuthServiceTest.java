package com.agrichain.identity.auth;

import com.agrichain.identity.entity.User;
import com.agrichain.identity.entity.UserStatus;
import com.agrichain.identity.notification.EmailNotificationClient;
import com.agrichain.identity.repository.UserRepository;
import com.agrichain.identity.security.JwtService;
import com.agrichain.identity.security.TokenStore;
import com.agrichain.common.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private EmailNotificationClient emailNotificationClient;

    private PasswordEncoder passwordEncoder;
    private Clock fixedClock;
    private AuthService authService;

    private static final String RAW_PASSWORD = "secret123";
    private String encodedPassword;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        encodedPassword = passwordEncoder.encode(RAW_PASSWORD);
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneOffset.UTC);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, tokenStore,
                emailNotificationClient, fixedClock);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private User activeUser() {
        User u = new User();
        u.setUsername("farmer1");
        u.setPasswordHash(encodedPassword);
        u.setStatus(UserStatus.Active);
        u.setRole(UserRole.Farmer);
        u.setEmail("farmer@example.com");
        return u;
    }

    // ── Valid credentials ─────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsToken() {
        User user = activeUser();
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));
        when(jwtService.issue(eq("farmer1"), any(UUID.class))).thenReturn("signed.jwt.token");

        String token = authService.login("farmer1", RAW_PASSWORD);

        assertThat(token).isEqualTo("signed.jwt.token");
        verify(tokenStore).store(any(UUID.class), eq("farmer1"));
    }

    @Test
    void login_successfulLogin_resetsFailedAttempts() {
        User user = activeUser();
        user.setFailedAttempts(3);
        user.setLockedAt(Instant.parse("2024-01-01T11:55:00Z"));
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));
        when(jwtService.issue(eq("farmer1"), any(UUID.class))).thenReturn("token");

        authService.login("farmer1", RAW_PASSWORD);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFailedAttempts()).isZero();
        assertThat(captor.getValue().getLockedAt()).isNull();
    }

    // ── Unknown username ──────────────────────────────────────────────────────

    @Test
    void login_unknownUsername_throws401() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody", RAW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(AuthService.AUTH_FAILED_MESSAGE);

        verifyNoInteractions(jwtService, tokenStore);
    }

    // ── Wrong password ────────────────────────────────────────────────────────

    @Test
    void login_wrongPassword_throws401() {
        User user = activeUser();
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("farmer1", "wrongpassword"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(AuthService.AUTH_FAILED_MESSAGE);

        verifyNoInteractions(jwtService, tokenStore);
    }

    // ── Locked account ────────────────────────────────────────────────────────

    @Test
    void login_lockedAccount_throws401() {
        User user = activeUser();
        user.setStatus(UserStatus.Locked);
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("farmer1", RAW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(AuthService.AUTH_FAILED_MESSAGE);

        verifyNoInteractions(jwtService, tokenStore);
    }

    // ── Inactive account ──────────────────────────────────────────────────────

    @Test
    void login_inactiveAccount_throws401() {
        User user = activeUser();
        user.setStatus(UserStatus.Inactive);
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("farmer1", RAW_PASSWORD))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(AuthService.AUTH_FAILED_MESSAGE);

        verifyNoInteractions(jwtService, tokenStore);
    }

    // ── Generic error message (no field-level detail) ─────────────────────────

    @Test
    void login_allFailureCasesReturnSameMessage() {
        // Unknown username
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        String msgUnknown = catchThrowableOfType(
                () -> authService.login("unknown", RAW_PASSWORD),
                AuthenticationFailedException.class).getMessage();

        // Wrong password
        User user = activeUser();
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));
        String msgWrongPw = catchThrowableOfType(
                () -> authService.login("farmer1", "bad"),
                AuthenticationFailedException.class).getMessage();

        // Locked
        User locked = activeUser();
        locked.setStatus(UserStatus.Locked);
        when(userRepository.findByUsername("locked")).thenReturn(Optional.of(locked));
        String msgLocked = catchThrowableOfType(
                () -> authService.login("locked", RAW_PASSWORD),
                AuthenticationFailedException.class).getMessage();

        assertThat(msgUnknown).isEqualTo(msgWrongPw).isEqualTo(msgLocked);
    }

    // ── Account lockout: 5th failure triggers lock ────────────────────────────

    @Test
    void login_fifthConsecutiveFailure_locksAccountAndSendsEmail() {
        User user = activeUser();
        // Simulate 4 prior failures within the window
        user.setFailedAttempts(4);
        user.setLockedAt(Instant.parse("2024-01-01T11:55:00Z")); // 5 min ago, within window
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("farmer1", "wrongpassword"))
                .isInstanceOf(AuthenticationFailedException.class);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.Locked);
        assertThat(saved.getFailedAttempts()).isEqualTo(5);
        verify(emailNotificationClient).sendAccountLockedNotification("farmer@example.com", "farmer1");
    }

    // ── 6th attempt on locked account still returns generic 401 ──────────────

    @Test
    void login_sixthAttemptOnLockedAccount_returns401WithoutEmailResend() {
        User user = activeUser();
        user.setStatus(UserStatus.Locked);
        user.setFailedAttempts(5);
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("farmer1", "wrongpassword"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(AuthService.AUTH_FAILED_MESSAGE);

        // No save, no email — locked check short-circuits before any counter logic
        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailNotificationClient);
    }

    // ── Failure counter resets after 10-minute window ─────────────────────────

    @Test
    void login_failureOutsideWindow_resetsCounterBeforeIncrementing() {
        User user = activeUser();
        // First failure was 11 minutes ago — outside the 10-minute window
        user.setFailedAttempts(4);
        user.setLockedAt(Instant.parse("2024-01-01T11:49:00Z")); // 11 min before fixedClock
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("farmer1", "wrongpassword"))
                .isInstanceOf(AuthenticationFailedException.class);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        // Counter was reset to 0 then incremented to 1 — account must NOT be locked
        assertThat(saved.getFailedAttempts()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.Active);
        verifyNoInteractions(emailNotificationClient);
    }

    // ── Helpers for logout/refresh tests ─────────────────────────────────────

    /** Build a Claims object with the given JTI and subject. */
    private Claims claimsFor(UUID tokenId, String subject) {
        DefaultClaims claims = new DefaultClaims(Map.of(
                "jti", tokenId.toString(),
                "sub", subject
        ));
        return claims;
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_validToken_invalidatesTokenInStore() {
        UUID tokenId = UUID.randomUUID();
        Claims claims = claimsFor(tokenId, "farmer1");

        when(jwtService.parse("valid.token")).thenReturn(claims);
        when(tokenStore.isActive(tokenId)).thenReturn(true);

        authService.logout("valid.token");

        verify(tokenStore).invalidate(tokenId);
    }

    @Test
    void logout_invalidatedToken_throwsInvalidTokenException() {
        UUID tokenId = UUID.randomUUID();
        Claims claims = claimsFor(tokenId, "farmer1");

        when(jwtService.parse("old.token")).thenReturn(claims);
        when(tokenStore.isActive(tokenId)).thenReturn(false);

        assertThatThrownBy(() -> authService.logout("old.token"))
                .isInstanceOf(InvalidTokenException.class);

        verify(tokenStore, never()).invalidate(any());
    }

    @Test
    void logout_malformedToken_throwsInvalidTokenException() {
        when(jwtService.parse("bad.token")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));

        assertThatThrownBy(() -> authService.logout("bad.token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_issuesNewTokenAndInvalidatesOld() {
        UUID oldId = UUID.randomUUID();
        Claims claims = claimsFor(oldId, "farmer1");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(UserRole.Farmer);

        when(jwtService.parse("old.token")).thenReturn(claims);
        when(tokenStore.isActive(oldId)).thenReturn(true);
        when(jwtService.issue(eq("farmer1"), any(UUID.class))).thenReturn("new.token");
        when(jwtService.getExpiryMinutes()).thenReturn(30L);
        when(userRepository.findByUsername("farmer1")).thenReturn(Optional.of(user));

        AuthService.LoginResult result = authService.refresh("old.token");

        assertThat(result.token()).isEqualTo("new.token");
        verify(tokenStore).invalidate(oldId);
        verify(tokenStore).store(any(UUID.class), eq("farmer1"));
    }

    @Test
    void refresh_invalidatedToken_throwsInvalidTokenException() {
        UUID tokenId = UUID.randomUUID();
        Claims claims = claimsFor(tokenId, "farmer1");

        when(jwtService.parse("old.token")).thenReturn(claims);
        when(tokenStore.isActive(tokenId)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("old.token"))
                .isInstanceOf(InvalidTokenException.class);

        verify(jwtService, never()).issue(any(), any());
        verify(tokenStore, never()).invalidate(any());
    }

    @Test
    void refresh_expiredToken_throwsInvalidTokenException() {
        when(jwtService.parse("expired.token"))
                .thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "expired"));

        assertThatThrownBy(() -> authService.refresh("expired.token"))
                .isInstanceOf(InvalidTokenException.class);
    }
}
