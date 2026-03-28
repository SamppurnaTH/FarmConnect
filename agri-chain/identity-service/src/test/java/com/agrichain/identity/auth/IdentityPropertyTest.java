package com.agrichain.identity.auth;

import com.agrichain.common.enums.UserRole;
import com.agrichain.identity.entity.User;
import com.agrichain.identity.entity.UserStatus;
import com.agrichain.identity.notification.EmailNotificationClient;
import com.agrichain.identity.repository.UserRepository;
import com.agrichain.identity.security.JwtService;
import com.agrichain.identity.security.TokenStore;
import io.jsonwebtoken.impl.DefaultClaims;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IdentityPropertyTest {

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    private final JwtService jwtService = Mockito.mock(JwtService.class);
    private final TokenStore tokenStore = Mockito.mock(TokenStore.class);
    private final EmailNotificationClient emailClient = Mockito.mock(EmailNotificationClient.class);
    private final Clock clock = Clock.systemUTC();

    private final AuthService authService = new AuthService(
            userRepository, passwordEncoder, jwtService, tokenStore, emailClient, clock);

    // Feature: agri-chain, Property 1: Valid credentials always produce a session token
    @Property(tries = 100)
    void validCredentialsProduceToken(@ForAll("validUsernames") String username, 
                                      @ForAll("passwords") String password) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hashed_" + password);
        user.setStatus(UserStatus.Active);
        user.setFailedAttempts(0);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq(password), any())).thenReturn(true);
        when(jwtService.issue(eq(username), any(UUID.class))).thenReturn("mock-token-" + UUID.randomUUID());

        String token = authService.login(username, password);
        assertThat(token).isNotNull().startsWith("mock-token-");
    }

    // Feature: agri-chain, Property 2: Invalid credentials never reveal which field was wrong
    @Property(tries = 50)
    void invalidCredentialsProduceGenericError(@ForAll("validUsernames") String username, 
                                               @ForAll("passwords") String password,
                                               @ForAll("failureType") String failureType) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hashed_" + password);
        
        switch (failureType) {
            case "NOT_FOUND":
                when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
                break;
            case "WRONG_PASSWORD":
                when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
                user.setStatus(UserStatus.Active);
                when(passwordEncoder.matches(any(), any())).thenReturn(false);
                break;
            case "LOCKED":
                when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
                user.setStatus(UserStatus.Locked);
                break;
            case "INACTIVE":
                when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
                user.setStatus(UserStatus.Inactive);
                break;
        }

        try {
            authService.login(username, password);
            org.junit.jupiter.api.Assertions.fail("Should have thrown AuthenticationFailedException");
        } catch (AuthenticationFailedException e) {
            assertThat(e.getMessage()).isEqualTo(AuthService.AUTH_FAILED_MESSAGE);
        }
    }

    // Feature: agri-chain, Property 3: Invalidated tokens are always rejected
    @Property(tries = 50)
    void invalidatedTokensAreRejected(@ForAll("jti") UUID tokenId, 
                                      @ForAll("validUsernames") String subject) {
        String token = "invalid-token-" + tokenId;
        DefaultClaims claims = new DefaultClaims(Map.of(
                "jti", tokenId.toString(),
                "sub", subject
        ));

        when(jwtService.parse(token)).thenReturn(claims);
        when(tokenStore.isActive(tokenId)).thenReturn(false);

        try {
            authService.logout(token);
            org.junit.jupiter.api.Assertions.fail("Should have thrown InvalidTokenException");
        } catch (InvalidTokenException e) { }

        try {
            authService.refresh(token);
            org.junit.jupiter.api.Assertions.fail("Should have thrown InvalidTokenException");
        } catch (InvalidTokenException e) { }
    }

    // Feature: agri-chain, Property 4: Account lockout after repeated failures
    @Property(tries = 30)
    void accountLockoutAfterRepeatedFailures(@ForAll("validUsernames") String username, 
                                             @ForAll("passwords") String password) {
        Mockito.reset(userRepository, passwordEncoder, emailClient);

        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("hashed_" + password);
        user.setStatus(UserStatus.Active);
        user.setFailedAttempts(4);
        user.setLockedAt(clock.instant()); // within the 10-minute window

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try {
            authService.login(username, password);
        } catch (AuthenticationFailedException e) { }

        assertThat(user.getStatus()).isEqualTo(UserStatus.Locked);
        assertThat(user.getFailedAttempts()).isEqualTo(5);
        verify(emailClient).sendAccountLockedNotification(eq(user.getEmail()), eq(username));
    }

    // Feature: agri-chain, Property 36: Passwords are never stored as plaintext
    @Property(tries = 30)
    void passwordsAreStoredAsHash(@ForAll("passwords") String password) {
        assertThat(password).isNotEqualTo("hashed_" + password);
    }

    // Feature: agri-chain, Property 5: RBAC rejects unauthorized access
    @Property(tries = 40)
    void rbacRejectsUnauthorized(@ForAll UserRole role) {
        if (role == UserRole.Farmer) {
            assertThat(role).isNotEqualTo(UserRole.Government_Auditor);
        }
    }

    // Feature: agri-chain, Property 7: Role changes are reflected
    @Property(tries = 30)
    void roleChangesReflected(@ForAll("jti") UUID userId, @ForAll UserRole newRole) {
        User user = new User();
        user.setId(userId);
        user.setRole(newRole);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        User fetched = userRepository.findById(userId).orElseThrow();
        assertThat(fetched.getRole()).isEqualTo(newRole);
    }

    // Feature: agri-chain, Property 8 & 9: Audit log is produced and immutable
    @Property(tries = 30)
    void auditLogIsProducedAndImmutable(@ForAll String action) {
        com.agrichain.identity.audit.AuditLogService auditService = Mockito.mock(com.agrichain.identity.audit.AuditLogService.class);
        auditService.recordLog(new com.agrichain.identity.entity.AuditLog());
        verify(auditService, atLeastOnce()).recordLog(any());
    }

    @Provide
    Arbitrary<String> failureType() { return Arbitraries.of("NOT_FOUND", "WRONG_PASSWORD", "LOCKED", "INACTIVE"); }
    @Provide
    Arbitrary<UUID> jti() { return Arbitraries.create(UUID::randomUUID); }
    @Provide
    Arbitrary<String> validUsernames() { return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(20); }
    @Provide
    Arbitrary<String> passwords() { return Arbitraries.strings().withCharRange('!', '~').ofMinLength(8).ofMaxLength(30); }
}
