package com.agrichain.notification;

import com.agrichain.notification.dto.NotificationRequest;
import com.agrichain.notification.entity.Notification;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * POST /notifications
     * Internal — called by other services to send alerts. No user JWT.
     */
    @PostMapping
    public ResponseEntity<UUID> sendNotification(@Valid @RequestBody NotificationRequest request) {
        UUID id = notificationService.sendNotification(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /notifications/me
     * Paginated notification history for the currently authenticated user.
     * userId is extracted from the JWT — never trusted from a header.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Notification>> getMyNotifications(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        UUID userId = extractUserId(auth);
        Page<Notification> history = notificationService.getUserNotifications(
                userId, PageRequest.of(page, size));
        return ResponseEntity.ok(history);
    }

    /**
     * PUT /notifications/{id}/read
     * Mark a notification as read. Any authenticated user can mark their own.
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts userId from JWT details set by JwtAuthFilter.
     * Never trusts the X-User-ID header.
     */
    private UUID extractUserId(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof String s && !s.isBlank()) {
            return UUID.fromString(s);
        }
        throw new IllegalStateException("Unable to extract userId from token");
    }
}
