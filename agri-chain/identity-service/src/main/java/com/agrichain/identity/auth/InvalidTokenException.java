package com.agrichain.identity.auth;

/**
 * Thrown when a token is missing, expired, or has been invalidated (e.g. after logout).
 * Maps to HTTP 401.
 */
public class InvalidTokenException extends RuntimeException {

    static final String INVALID_TOKEN_MESSAGE = "Invalid or expired token";

    public InvalidTokenException() {
        super(INVALID_TOKEN_MESSAGE);
    }
}
