package com.agrichain.identity.auth;

/**
 * Thrown when login fails for any reason (unknown user, wrong password, locked/inactive account).
 * A single exception type ensures the caller cannot distinguish which field caused the failure.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super(AuthService.AUTH_FAILED_MESSAGE);
    }
}
