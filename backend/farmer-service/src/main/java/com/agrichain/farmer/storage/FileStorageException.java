package com.agrichain.farmer.storage;

/**
 * Thrown when a file storage operation fails due to validation or I/O errors.
 * Maps to HTTP 400 (validation) or 500 (I/O) in the controller.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
