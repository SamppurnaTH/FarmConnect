package com.agrichain.notification.dto;

import com.agrichain.common.enums.NotificationChannel;
import jakarta.validation.constraints.*;
import java.util.UUID;

/**
 * DTO for creating a new notification.
 */
public class NotificationRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private NotificationChannel channel;

    @NotBlank
    @Size(max = 1000)
    private String content;

    // Getters and setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
