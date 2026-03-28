package com.agrichain.identity.notification;

/**
 * Contract for sending email notifications from the Identity Service.
 * Real SMTP integration is out of scope; use {@link StubEmailNotificationClient} in production
 * until a real implementation is wired in.
 */
public interface EmailNotificationClient {

    /**
     * Sends an account-lockout notification to the given email address.
     *
     * @param toEmail the recipient's email address (already decrypted by the caller)
     * @param username the username of the locked account, for context in the email body
     */
    void sendAccountLockedNotification(String toEmail, String username);
}
