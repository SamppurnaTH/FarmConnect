package com.agrichain.farmer;

import com.agrichain.common.enums.FarmerStatus;
import com.agrichain.common.enums.UserRole;
import com.agrichain.common.enums.VerificationStatus;
import com.agrichain.farmer.dto.FarmerRegistrationRequest;
import com.agrichain.farmer.dto.FarmerProfileResponse;
import com.agrichain.farmer.dto.FarmerDocumentResponse;
import com.agrichain.farmer.dto.FarmerPageResponse;
import com.agrichain.farmer.dto.UpdateFarmerRequest;
import com.agrichain.farmer.entity.Farmer;
import com.agrichain.farmer.entity.FarmerDocument;
import com.agrichain.farmer.entity.DocumentType;
import com.agrichain.farmer.repository.FarmerRepository;
import com.agrichain.farmer.repository.FarmerDocumentRepository;
import com.agrichain.farmer.storage.FileStorageService;
import com.agrichain.farmer.storage.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final FarmerDocumentRepository farmerDocumentRepository;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate;
    // Singleton ObjectMapper — not instantiated per-call
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${services.identity.url:http://localhost:8081}")
    private String identityServiceUrl;

    @Value("${services.notification.url:http://localhost:8088}")
    private String notificationServiceUrl;

    public FarmerService(FarmerRepository farmerRepository,
                         FarmerDocumentRepository farmerDocumentRepository,
                         FileStorageService fileStorageService,
                         RestTemplateBuilder restTemplateBuilder) {
        this.farmerRepository         = farmerRepository;
        this.farmerDocumentRepository = farmerDocumentRepository;
        this.fileStorageService       = fileStorageService;
        this.restTemplate             = restTemplateBuilder.build();
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @Transactional
    public UUID registerFarmer(FarmerRegistrationRequest request) {
        if (farmerRepository.existsByContactInfoAndStatusIn(request.getContactInfo(),
                List.of(FarmerStatus.Active, FarmerStatus.Pending_Verification))) {
            throw new IllegalArgumentException("Farmer with this contact information already exists.");
        }

        UUID userId = createUserInIdentityService(request);

        Farmer farmer = new Farmer();
        farmer.setUserId(userId);
        farmer.setName(request.getName());
        farmer.setDateOfBirth(request.getDateOfBirth().toString());
        farmer.setGender(request.getGender());
        farmer.setAddress(request.getAddress());
        farmer.setContactInfo(request.getContactInfo());
        farmer.setLandDetails(request.getLandDetails());
        farmer.setStatus(FarmerStatus.Pending_Verification);

        return farmerRepository.save(farmer).getId();
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    public Optional<FarmerProfileResponse> getFarmerById(UUID farmerId) {
        return farmerRepository.findById(farmerId).map(FarmerProfileResponse::from);
    }

    /**
     * Lookup by identity userId — used by the frontend after login,
     * since the JWT carries userId (identity ID), not the farmer profile ID.
     */
    public Optional<FarmerProfileResponse> getFarmerByUserId(UUID userId) {
        return farmerRepository.findByUserId(userId).map(FarmerProfileResponse::from);
    }

    public List<FarmerProfileResponse> listFarmers(FarmerStatus status) {
        List<Farmer> farmers = (status == null)
                ? farmerRepository.findAll()
                : farmerRepository.findByStatus(status);
        return farmers.stream().map(FarmerProfileResponse::from).collect(Collectors.toList());
    }

    /**
     * Paginated farmer list with optional name search.
     *
     * Because the name column is AES-encrypted, DB-level LIKE is not possible.
     * We fetch a page by status, then apply the name filter in-memory on the
     * decrypted values. For large datasets, consider a search index or
     * deterministic encryption for the name field.
     *
     * @param status  filter by farmer status (required)
     * @param search  optional partial name match (case-insensitive, applied after decryption)
     * @param page    0-based page number
     * @param size    page size (max 100)
     */
    public FarmerPageResponse listFarmersPaged(FarmerStatus status, String search, int page, int size) {
        // Clamp page size to prevent abuse
        int clampedSize = Math.min(size, 100);

        if (search != null && !search.isBlank()) {
            // Name search: load all by status, filter in-memory, then manually paginate
            List<Farmer> all = (status == null)
                    ? farmerRepository.findAll()
                    : farmerRepository.findByStatus(status);

            String lowerSearch = search.toLowerCase();
            List<FarmerProfileResponse> filtered = all.stream()
                    .map(FarmerProfileResponse::from)
                    .filter(f -> f.getName() != null && f.getName().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());

            long total = filtered.size();
            int fromIndex = page * clampedSize;
            int toIndex   = Math.min(fromIndex + clampedSize, filtered.size());
            List<FarmerProfileResponse> pageContent = fromIndex >= filtered.size()
                    ? List.of()
                    : filtered.subList(fromIndex, toIndex);

            int totalPages = (int) Math.ceil((double) total / clampedSize);
            return new FarmerPageResponse(pageContent, page, clampedSize, total, totalPages);
        }

        // No search — use DB-level pagination
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, clampedSize,
                        org.springframework.data.domain.Sort.by("createdAt").descending());

        org.springframework.data.domain.Page<Farmer> dbPage = (status == null)
                ? farmerRepository.findAll(pageable)
                : farmerRepository.findByStatus(status, pageable);

        List<FarmerProfileResponse> content = dbPage.getContent().stream()
                .map(FarmerProfileResponse::from)
                .collect(Collectors.toList());

        return new FarmerPageResponse(content, page, clampedSize,
                dbPage.getTotalElements(), dbPage.getTotalPages());
    }

    /**
     * Uses a COUNT query — does NOT load all rows into memory.
     */
    public long countFarmers(FarmerStatus status) {
        if (status == null) return farmerRepository.count();
        return farmerRepository.countByStatus(status);
    }

    /**
     * Returns farmers registered within a date range.
     * Used by reporting-service for scoped report generation.
     */
    public List<FarmerProfileResponse> getFarmersByDateRange(java.time.LocalDate start, java.time.LocalDate end) {
        java.time.Instant from = start.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant to   = end.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        return farmerRepository.findByCreatedAtBetween(from, to)
                .stream().map(FarmerProfileResponse::from).collect(Collectors.toList());
    }

    public FarmerStatus getFarmerStatus(UUID farmerId) {
        return farmerRepository.findById(farmerId)
                .map(Farmer::getStatus)
                .orElseThrow(() -> new FarmerNotFoundException("Farmer not found: " + farmerId));
    }

    // ── Updates ───────────────────────────────────────────────────────────────

    @Transactional
    public FarmerProfileResponse updateFarmer(UUID farmerId, UpdateFarmerRequest request, UUID currentUserId) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new FarmerNotFoundException("Farmer not found: " + farmerId));

        String previousValue = serialize(farmer);

        if (request.getName() != null)        farmer.setName(request.getName());
        if (request.getAddress() != null)     farmer.setAddress(request.getAddress());
        if (request.getContactInfo() != null) farmer.setContactInfo(request.getContactInfo());
        if (request.getLandDetails() != null) farmer.setLandDetails(request.getLandDetails());

        Farmer saved = farmerRepository.save(farmer);
        recordAuditLog(currentUserId, "UPDATE", "FARMER", saved.getId(), previousValue, serialize(saved));

        return FarmerProfileResponse.from(saved);
    }

    @Transactional
    public void updateStatus(UUID farmerId, FarmerStatus newStatus) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new FarmerNotFoundException("Farmer not found: " + farmerId));

        FarmerStatus previousStatus = farmer.getStatus();
        farmer.setStatus(newStatus);
        farmerRepository.save(farmer);

        // Audit log the status change
        recordAuditLog(farmer.getUserId(), "STATUS_CHANGE", "FARMER", farmerId,
                previousStatus.name(), newStatus.name());

        notifyStatusChange(farmer);
    }

    // ── Documents ─────────────────────────────────────────────────────────────

    public List<FarmerDocumentResponse> getDocuments(UUID farmerId) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new FarmerNotFoundException("Farmer not found: " + farmerId);
        }
        return farmerDocumentRepository.findByFarmerId(farmerId)
                .stream()
                .map(FarmerDocumentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public UUID uploadDocument(UUID farmerId, DocumentType type, MultipartFile file) {
        if (!farmerRepository.existsById(farmerId)) {
            throw new FarmerNotFoundException("Farmer not found: " + farmerId);
        }

        // Store the actual file bytes on disk; get back a relative path
        String storagePath = fileStorageService.store(farmerId, file);

        // If a document of this type already exists, delete the old file first
        farmerDocumentRepository.findByFarmerIdAndDocumentType(farmerId, type)
                .ifPresent(existing -> {
                    fileStorageService.delete(existing.getStoragePath());
                    farmerDocumentRepository.delete(existing);
                });

        FarmerDocument doc = new FarmerDocument();
        doc.setFarmerId(farmerId);
        doc.setDocumentType(type);
        doc.setStoragePath(storagePath);
        doc.setVerificationStatus(VerificationStatus.Pending);

        return farmerDocumentRepository.save(doc).getId();
    }

    /**
     * Resolves the stored file path for a document.
     * Used by the controller to serve the file as a download.
     * Access control (farmer owns doc, or officer) is enforced in the controller.
     */
    public java.nio.file.Path getDocumentFilePath(UUID farmerId, UUID docId) {
        FarmerDocument doc = farmerDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        if (!doc.getFarmerId().equals(farmerId)) {
            throw new IllegalArgumentException("Document does not belong to farmer: " + farmerId);
        }

        return fileStorageService.resolve(doc.getStoragePath());
    }

    @Transactional
    public void verifyDocument(UUID farmerId, UUID docId, VerificationStatus status,
                               UUID officerId, String reason) {
        FarmerDocument doc = farmerDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        if (!doc.getFarmerId().equals(farmerId)) {
            throw new IllegalArgumentException("Document does not belong to farmer: " + farmerId);
        }

        doc.setVerificationStatus(status);
        doc.setReviewedBy(officerId);
        doc.setReviewedAt(Instant.now());
        doc.setRejectionReason(reason);

        farmerDocumentRepository.save(doc);
        notifyDocumentStatusChange(doc);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private UUID createUserInIdentityService(FarmerRegistrationRequest request) {
        Map<String, Object> userRequest = Map.of(
                "username", request.getUsername(),
                "password", request.getPassword(),
                "email",    request.getEmail(),
                "role",     UserRole.FARMER
        );
        try {
            return restTemplate.postForObject(identityServiceUrl + "/auth/register", userRequest, UUID.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new IllegalArgumentException("Username or email already registered.");
            }
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        }
    }

    private void notifyDocumentStatusChange(FarmerDocument doc) {
        // Look up the farmer's userId to address the notification correctly
        farmerRepository.findById(doc.getFarmerId()).ifPresent(farmer -> {
            String message = switch (doc.getVerificationStatus()) {
                case Verified -> "Your " + doc.getDocumentType().name().replace("_", " ")
                        + " document has been verified successfully.";
                case Rejected -> "Your " + doc.getDocumentType().name().replace("_", " ")
                        + " document was rejected."
                        + (doc.getRejectionReason() != null ? " Reason: " + doc.getRejectionReason() : "");
                default -> "Your document status has been updated to " + doc.getVerificationStatus().name();
            };
            sendNotification(farmer.getUserId(), message);
        });
    }

    private void notifyStatusChange(Farmer farmer) {
        String message = switch (farmer.getStatus()) {
            case Active -> "Your farmer account has been verified and activated. You can now create crop listings.";
            case Inactive -> "Your farmer account has been deactivated. Please contact support for assistance.";
            default -> "Your account status has been updated to " + farmer.getStatus().name();
        };
        sendNotification(farmer.getUserId(), message);
    }

    /**
     * Sends an In_App notification to the given user via notification-service.
     * Failures are logged but do not roll back the calling transaction.
     */
    private void sendNotification(UUID userId, String message) {
        Map<String, Object> request = Map.of(
                "userId",  userId,
                "channel", "In_App",
                "content", message
        );
        try {
            restTemplate.postForObject(notificationServiceUrl + "/notifications", request, Void.class);
        } catch (Exception e) {
            System.err.println("[notification] Failed to send notification to user " + userId + ": " + e.getMessage());
        }
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void recordAuditLog(UUID userId, String action, String resourceType,
                                UUID resourceId, String prev, String next) {
        Map<String, Object> log = Map.of(
                "userId",        userId,
                "actionType",    action,
                "resourceType",  resourceType,
                "resourceId",    resourceId,
                "previousValue", prev,
                "newValue",      next
        );
        try {
            restTemplate.postForObject(identityServiceUrl + "/audit-log", log, Void.class);
        } catch (Exception e) {
            System.err.println("[audit] Failed to record audit log: " + e.getMessage());
        }
    }
}
