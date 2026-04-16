package com.agrichain.farmer;

import com.agrichain.common.enums.FarmerStatus;
import com.agrichain.common.enums.VerificationStatus;
import com.agrichain.farmer.dto.*;
import com.agrichain.farmer.entity.DocumentType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.agrichain.farmer.storage.FileStorageException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/farmers")
public class FarmerController {

    private final FarmerService farmerService;

    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    // ── Registration (public) ─────────────────────────────────────────────────

    /**
     * POST /farmers
     * Public self-registration. No JWT required.
     */
    @PostMapping
    public ResponseEntity<UUID> register(@Valid @RequestBody FarmerRegistrationRequest request) {
        UUID farmerId = farmerService.registerFarmer(request);
        return ResponseEntity.status(201).body(farmerId);
    }

    // ── Profile reads ─────────────────────────────────────────────────────────

    /**
     * GET /farmers/me
     * Returns the farmer profile for the currently authenticated user.
     * The JWT carries userId (identity ID); we look up the farmer by that userId.
     * This is the correct endpoint for the frontend to call after login.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<FarmerProfileResponse> getMyProfile(Authentication auth) {
        UUID userId = extractUserId(auth);
        return farmerService.getFarmerByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /farmers/{id}
     * Retrieve a farmer profile by farmer ID.
     * Accessible by Market Officers and Administrators.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MARKET_OFFICER','ADMINISTRATOR','PROGRAM_MANAGER','COMPLIANCE_OFFICER','GOVERNMENT_AUDITOR')")
    public ResponseEntity<FarmerProfileResponse> getFarmer(@PathVariable UUID id) {
        return farmerService.getFarmerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /farmers
     * List all farmers, optionally filtered by status.
     * Accessible by Market Officers and Administrators.
     *
     * Without pagination params → returns full list (backward-compatible).
     * With ?page=&size= → returns paginated FarmerPageResponse.
     * With ?search= → filters by name (case-insensitive, post-decryption).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MARKET_OFFICER','ADMINISTRATOR')")
    public ResponseEntity<?> listFarmers(
            @RequestParam(required = false) FarmerStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        // If pagination params provided, return paginated response
        if (page != null || size != null) {
            int p = page != null ? Math.max(0, page) : 0;
            int s = size != null ? Math.max(1, size) : 20;
            return ResponseEntity.ok(farmerService.listFarmersPaged(status, search, p, s));
        }

        // Legacy: return full list (used by internal service calls)
        return ResponseEntity.ok(farmerService.listFarmers(status));
    }

    /**
     * GET /farmers/count
     * Returns farmer count, optionally filtered by status.
     * Internal endpoint — also called by reporting-service (no JWT in service-to-service calls).
     * Permitted without auth in SecurityConfig.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getFarmerCount(@RequestParam(required = false) FarmerStatus status) {
        return ResponseEntity.ok(farmerService.countFarmers(status));
    }

    /**
     * GET /farmers/report?start=&end=
     * Internal — called by reporting-service to fetch farmers registered in a date range.
     * No auth required (service-to-service). Permitted without auth in SecurityConfig.
     */
    @GetMapping("/report")
    public ResponseEntity<List<FarmerProfileResponse>> getFarmersForReport(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        return ResponseEntity.ok(farmerService.getFarmersByDateRange(start, end));
    }

    /**
     * GET /farmers/{id}/status
     * Returns the raw status string for a farmer.
     * Internal endpoint — called by crop-service and subsidy-service.
     * Permitted without auth in SecurityConfig.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<FarmerStatus> getFarmerStatus(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(farmerService.getFarmerStatus(id));
        } catch (FarmerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Profile updates ───────────────────────────────────────────────────────

    /**
     * PUT /farmers/{id}
     * Update own profile. Farmer can only update their own record.
     * userId is extracted from the JWT — not trusted from a header.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<FarmerProfileResponse> updateFarmer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFarmerRequest request,
            Authentication auth) {
        UUID currentUserId = extractUserId(auth);
        return ResponseEntity.ok(farmerService.updateFarmer(id, request, currentUserId));
    }

    /**
     * PUT /farmers/{id}/status
     * Activate or deactivate a farmer. Market Officer / Administrator only.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MARKET_OFFICER','ADMINISTRATOR')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        FarmerStatus newStatus = parseEnum(FarmerStatus.class, body.get("status"), "status");
        farmerService.updateStatus(id, newStatus);
        return ResponseEntity.noContent().build();
    }

    // ── Documents ─────────────────────────────────────────────────────────────

    /**
     * GET /farmers/{id}/documents
     * Returns all documents for a farmer.
     * Farmer can view their own; Market Officers can view any.
     */
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasAnyRole('FARMER','MARKET_OFFICER','ADMINISTRATOR')")
    public ResponseEntity<List<FarmerDocumentResponse>> getDocuments(@PathVariable UUID id) {
        return ResponseEntity.ok(farmerService.getDocuments(id));
    }

    /**
     * POST /farmers/{id}/documents/upload
     * Upload a KYC document as multipart/form-data.
     * Accepts: PDF, JPG, PNG — max 10 MB.
     * Farmer only — a farmer can only upload to their own profile.
     *
     * Form fields:
     *   - file         : the binary file
     *   - documentType : one of National_ID | Land_Title | Tax_Certificate
     */
    @PostMapping(value = "/{id}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<UUID> uploadDocument(
            @PathVariable UUID id,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        // Ensure the farmer is uploading to their own profile
        UUID requestingUserId = extractUserId(auth);
        farmerService.getFarmerByUserId(requestingUserId)
                .filter(p -> p.getId().equals(id))
                .orElseThrow(() -> new SecurityException("You can only upload documents to your own profile."));

        DocumentType type = parseEnum(DocumentType.class, documentType, "documentType");
        UUID docId = farmerService.uploadDocument(id, type, file);
        return ResponseEntity.status(201).body(docId);
    }

    /**
     * GET /farmers/{id}/documents/{docId}/file
     * Download a KYC document file.
     * Farmer can download their own; Market Officers can download any.
     */
    @GetMapping("/{id}/documents/{docId}/file")
    @PreAuthorize("hasAnyRole('FARMER','MARKET_OFFICER','ADMINISTRATOR')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID id,
            @PathVariable UUID docId) {
        try {
            java.nio.file.Path filePath = farmerService.getDocumentFilePath(id, docId);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type from file extension
            String contentType = "application/octet-stream";
            String filename = filePath.getFileName().toString().toLowerCase();
            if (filename.endsWith(".pdf"))  contentType = "application/pdf";
            else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (filename.endsWith(".png")) contentType = "image/png";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("[error] File download failed: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * PUT /farmers/{id}/documents/{docId}/verify
     * Verify or reject a document. Market Officer only.
     */
    @PutMapping("/{id}/documents/{docId}/verify")
    @PreAuthorize("hasRole('MARKET_OFFICER')")
    public ResponseEntity<Void> verifyDocument(
            @PathVariable UUID id,
            @PathVariable UUID docId,
            @Valid @RequestBody DocumentVerifyRequest request,
            Authentication auth) {
        VerificationStatus status = parseEnum(VerificationStatus.class, request.getStatus(), "status");
        UUID officerId = extractUserId(auth);
        farmerService.verifyDocument(id, docId, status, officerId, request.getRejectionReason());
        return ResponseEntity.noContent().build();
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, String>> handleStorageError(FileStorageException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(SecurityException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(FarmerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(FarmerNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(422).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleServerError(RuntimeException ex) {
        // Log the real cause internally; return a generic message to the client
        System.err.println("[error] Unhandled exception: " + ex.getMessage());
        return ResponseEntity.status(500).body(Map.of("error", "An internal error occurred. Please try again."));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Extracts the userId from the JWT details set by JwtAuthFilter.
     * Never trusts the X-User-ID header.
     */
    private UUID extractUserId(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof String s && !s.isBlank()) {
            return UUID.fromString(s);
        }
        throw new IllegalStateException("Unable to extract userId from token");
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is required.");
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value for '" + fieldName + "': " + value);
        }
    }
}
