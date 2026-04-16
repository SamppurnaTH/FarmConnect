package com.agrichain.farmer.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Local-disk file storage for KYC documents.
 *
 * Files are stored under:
 *   ${agrichain.storage.base-dir}/{farmerId}/{uuid}_{originalFilename}
 *
 * The base directory is configurable via application.yml so it can be
 * swapped for an S3 path or mounted volume in production without code changes.
 *
 * Security:
 * - Only PDF, JPG, JPEG, PNG are accepted (checked by MIME type, not just extension)
 * - Maximum file size: 10 MB
 * - Stored filename includes a UUID prefix to prevent path traversal and collisions
 */
@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final Path baseDir;

    public FileStorageService(@Value("${agrichain.storage.base-dir:./uploads/documents}") String baseDirPath) {
        this.baseDir = Paths.get(baseDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create document storage directory: " + baseDirPath, e);
        }
    }

    /**
     * Stores the uploaded file under a farmer-specific subdirectory.
     *
     * @param farmerId the farmer's profile UUID (used as subdirectory)
     * @param file     the uploaded multipart file
     * @return the relative storage path (e.g. "farmerId/uuid_filename.pdf")
     * @throws FileStorageException if the file is invalid or cannot be stored
     */
    public String store(UUID farmerId, MultipartFile file) {
        validateFile(file);

        String originalFilename = sanitiseFilename(file.getOriginalFilename());
        String storedFilename   = UUID.randomUUID() + "_" + originalFilename;

        Path farmerDir = baseDir.resolve(farmerId.toString());
        try {
            Files.createDirectories(farmerDir);
        } catch (IOException e) {
            throw new FileStorageException("Could not create farmer directory", e);
        }

        Path targetPath = farmerDir.resolve(storedFilename);

        // Prevent path traversal — ensure target is inside farmerDir
        if (!targetPath.normalize().startsWith(farmerDir.normalize())) {
            throw new FileStorageException("Invalid filename — path traversal detected");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + e.getMessage(), e);
        }

        // Return relative path: farmerId/storedFilename
        return farmerId + "/" + storedFilename;
    }

    /**
     * Resolves a stored relative path to an absolute Path for serving.
     *
     * @param relativePath the path returned by {@link #store}
     * @return absolute Path to the file
     * @throws FileStorageException if the path is invalid or outside the base directory
     */
    public Path resolve(String relativePath) {
        Path resolved = baseDir.resolve(relativePath).normalize();

        // Security: ensure the resolved path is still inside baseDir
        if (!resolved.startsWith(baseDir)) {
            throw new FileStorageException("Invalid storage path — path traversal detected");
        }

        if (!Files.exists(resolved)) {
            throw new FileStorageException("File not found: " + relativePath);
        }

        return resolved;
    }

    /**
     * Deletes a stored file. Silently ignores missing files.
     */
    public void delete(String relativePath) {
        try {
            Path resolved = baseDir.resolve(relativePath).normalize();
            if (resolved.startsWith(baseDir)) {
                Files.deleteIfExists(resolved);
            }
        } catch (IOException e) {
            System.err.println("[storage] Failed to delete file: " + relativePath + " — " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileStorageException(
                    "File size exceeds the 10 MB limit (received " +
                    (file.getSize() / (1024 * 1024)) + " MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException(
                    "Invalid file type '" + contentType + "'. Only PDF, JPG, and PNG are accepted.");
        }
    }

    private String sanitiseFilename(String original) {
        if (original == null || original.isBlank()) return "document";
        // Strip path separators and keep only safe characters
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
