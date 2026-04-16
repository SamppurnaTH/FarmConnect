package com.agrichain.farmer;

/**
 * Thrown when a farmer record cannot be found.
 * Maps to HTTP 404 in the controller exception handler.
 */
public class FarmerNotFoundException extends RuntimeException {
    public FarmerNotFoundException(String message) {
        super(message);
    }
}
