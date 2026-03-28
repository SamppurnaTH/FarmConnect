package com.agrichain.identity.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of {@link EmailNotificationClient}.
 * Logs the notification instead of sending a real email.
 * Replace with a real SMTP / SES implementation when ready.
 */
@Component
public class StubEmailNotificationClient implements EmailNotificationClient {

    private static final Logger log = LoggerFactory.getLogger(StubEmailNotificationClient.class);

    @Override
    public void sendAccountLockedNotification(String toEmail, String username) {
        log.warn("[STUB EMAIL] Account locked notification → to={} username={}", toEmail, username);
    }
}
