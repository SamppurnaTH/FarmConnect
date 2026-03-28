package com.agrichain.identity.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /auth/login
     *
     * Validates credentials and returns a short-lived JWT on success.
     * Returns 401 with a generic message on any failure — no field-level detail.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    /**
     * POST /auth/register
     *
     * Creates a new user account. Typically called by other domain services 
     * (like Farmer Service) during registration, but can be used for direct registration.
     */
    @PostMapping("/register")
    public ResponseEntity<java.util.UUID> register(@Valid @RequestBody UserRegistrationRequest request) {
        com.agrichain.identity.entity.User user = authService.createUser(
                request.getUsername(), 
                request.getPassword(), 
                request.getEmail(), 
                request.getRole()
        );
        return ResponseEntity.status(201).body(user.getId());
    }

    /**
     * POST /auth/logout
     *
     * Extracts the JTI from the Authorization Bearer token and removes it from the token store.
     * Returns 204 No Content on success.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearer(authHeader);
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /auth/refresh
     *
     * Validates the current token, issues a new JWT with a new JTI, and invalidates the old one.
     * Returns 200 with the new token, or 401 if the token is invalid/expired/invalidated.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = extractBearer(authHeader);
        String newToken = authService.refresh(token);
        return ResponseEntity.ok(new LoginResponse(newToken));
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, String>> handleAuthFailed(AuthenticationFailedException ex) {
        return ResponseEntity.status(401)
                .body(Map.of("error", AuthService.AUTH_FAILED_MESSAGE));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(401)
                .body(Map.of("error", InvalidTokenException.INVALID_TOKEN_MESSAGE));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractBearer(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new InvalidTokenException();
    }
}
