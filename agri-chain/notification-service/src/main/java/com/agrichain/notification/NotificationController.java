package com.agrichain.notification;

import com.agrichain.notification.dto.NotificationRequest;
import com.agrichain.notification.entity.Notification;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Internal endpoint for services to send alerts.
     */
    @PostMapping
    public ResponseEntity<UUID> sendNotification(@Valid @RequestBody NotificationRequest request) {
        UUID id = notificationService.sendNotification(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /notifications/me
     * Paginated history for the current user.
     */
    @GetMapping("/me")
    public ResponseEntity<Page<Notification>> getMyNotifications(
            @RequestHeader("X-User-ID") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        Page<Notification> history = notificationService.getUserNotifications(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(history);
    }

    /**
     * PUT /notifications/{id}/read
     * Mark a notification as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    // ── Exception handler ─────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }
}
