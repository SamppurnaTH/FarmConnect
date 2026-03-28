package com.agrichain.identity.auth;

import com.agrichain.identity.entity.User;
import com.agrichain.identity.entity.UserStatus;
import com.agrichain.identity.notification.EmailNotificationClient;
import com.agrichain.identity.repository.UserRepository;
import com.agrichain.identity.security.JwtService;
import com.agrichain.identity.security.TokenStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Handles login credential validation and token issuance.
 */
@Service
public class AuthService {

    static final String AUTH_FAILED_MESSAGE = "Authentication failed";
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final long LOCKOUT_WINDOW_MINUTES = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final EmailNotificationClient emailNotificationClient;
    private final Clock clock;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenStore tokenStore,
                       EmailNotificationClient emailNotificationClient,
                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.emailNotificationClient = emailNotificationClient;
        this.clock = clock;
    }

    public String login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(AuthenticationFailedException::new);

        if (user.getStatus() == UserStatus.Locked) {
            throw new AuthenticationFailedException();
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            recordFailedAttempt(user);
            throw new AuthenticationFailedException();
        }

        if (user.getStatus() == UserStatus.Inactive) {
            throw new AuthenticationFailedException();
        }

        user.setFailedAttempts(0);
        user.setLockedAt(null);
        userRepository.save(user);

        UUID tokenId = UUID.randomUUID();
        String token = jwtService.issue(user.getUsername(), tokenId);
        tokenStore.store(tokenId, user.getUsername());
        return token;
    }

    private void recordFailedAttempt(User user) {
        Instant now = clock.instant();
        if (user.getLockedAt() != null) {
            Instant windowStart = now.minus(LOCKOUT_WINDOW_MINUTES, ChronoUnit.MINUTES);
            if (user.getLockedAt().isBefore(windowStart)) {
                user.setFailedAttempts(0);
                user.setLockedAt(null);
            }
        }
        if (user.getFailedAttempts() == 0) {
            user.setLockedAt(now);
        }
        int newCount = user.getFailedAttempts() + 1;
        user.setFailedAttempts(newCount);
        if (newCount >= MAX_FAILED_ATTEMPTS) {
            user.setStatus(UserStatus.Locked);
            user.setLockedAt(now);
            userRepository.save(user);
            emailNotificationClient.sendAccountLockedNotification(user.getEmail(), user.getUsername());
        } else {
            userRepository.save(user);
        }
    }

    public void logout(String bearerToken) {
        UUID tokenId = parseActiveTokenId(bearerToken);
        tokenStore.invalidate(tokenId);
    }

    public String refresh(String bearerToken) {
        UUID oldTokenId = parseActiveTokenId(bearerToken);
        Claims claims;
        try {
            claims = jwtService.parse(bearerToken);
        } catch (JwtException e) {
            throw new InvalidTokenException();
        }
        String subject = claims.getSubject();
        UUID newTokenId = UUID.randomUUID();
        String newToken = jwtService.issue(subject, newTokenId);
        tokenStore.invalidate(oldTokenId);
        tokenStore.store(newTokenId, subject);
        return newToken;
    }

    private UUID parseActiveTokenId(String bearerToken) {
        Claims claims;
        try {
            claims = jwtService.parse(bearerToken);
        } catch (JwtException e) {
            throw new InvalidTokenException();
        }
        UUID tokenId = UUID.fromString(claims.getId());
        if (!tokenStore.isActive(tokenId)) {
            throw new InvalidTokenException();
        }
        return tokenId;
    }

    /**
     * Creates a new user with the given credentials and role.
     */
    public User createUser(String username, String rawPassword, String email, com.agrichain.common.enums.UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(UserStatus.Active);
        return userRepository.save(user);
    }
}
