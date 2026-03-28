package com.agrichain.notification;

import com.agrichain.common.enums.NotificationStatus;
import com.agrichain.notification.dto.NotificationRequest;
import com.agrichain.notification.entity.Notification;
import com.agrichain.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private static final int MAX_RETRIES = 3;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Requirement 16.1: Create and deliver notification.
     */
    @Transactional
    public UUID sendNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setChannel(request.getChannel());
        notification.setMessage(request.getContent());
        notification.setStatus(NotificationStatus.Pending);
        
        notification = notificationRepository.save(notification);
        
        deliverWithRetry(notification);
        
        return notification.getId();
    }

    /**
     * Requirement 16.3: Retry logic.
     * In a real system, this would be async or a background job.
     */
    public void deliverWithRetry(Notification notification) {
        boolean success = simulateDelivery(notification);
        
        if (success) {
            notification.setStatus(NotificationStatus.Delivered);
            notification.setDeliveredAt(Instant.now());
        } else {
            int currentRetries = notification.getRetryCount();
            if (currentRetries < MAX_RETRIES) {
                notification.setRetryCount(currentRetries + 1);
                // In production: enqueue for later
                deliverWithRetry(notification); // Recursive for simulation/testing
            } else {
                notification.setStatus(NotificationStatus.Failed);
            }
        }
        notificationRepository.save(notification);
    }

    private boolean simulateDelivery(Notification notification) {
        // Mocking external delivery (Twilio, SendGrid, etc.)
        // For testing, we might want to inject a failure chance
        return true; 
    }

    public Page<Notification> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public void markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setReadAt(Instant.now());
        notificationRepository.save(notification);
    }
}
