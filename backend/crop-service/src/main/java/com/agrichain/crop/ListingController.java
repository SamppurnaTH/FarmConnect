package com.agrichain.crop;

import com.agrichain.common.enums.ListingStatus;
import com.agrichain.crop.dto.ListingRequest;
import com.agrichain.crop.entity.CropListing;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private final CropListingService cropListingService;

    public ListingController(CropListingService cropListingService) {
        this.cropListingService = cropListingService;
    }

    /**
     * POST /listings
     * Farmer creates a listing. Farmer must be Active (verified by service layer).
     */
    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<UUID> createListing(@Valid @RequestBody ListingRequest request) {
        UUID id = cropListingService.createListing(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /listings
     * - ?farmerId= → all listings for that farmer (all statuses, farmer's own view)
     * - ?location= → active listings matching location (trader browse)
     * - no params  → all active listings
     * Public endpoint — no auth required for browse.
     */
    @GetMapping
    public ResponseEntity<List<CropListing>> searchListings(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) UUID farmerId) {
        return ResponseEntity.ok(cropListingService.searchListings(location, farmerId, false));
    }

    /**
     * GET /listings/pending
     * Returns all listings with Pending_Approval status.
     * Market Officer only.
     * Declared before /{id} to prevent Spring treating "pending" as a path variable.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MARKET_OFFICER','ADMINISTRATOR')")
    public ResponseEntity<List<CropListing>> getPendingListings() {
        return ResponseEntity.ok(cropListingService.searchListings(null, null, true));
    }

    /**
     * GET /listings/total-volume
     * Internal — called by reporting-service. No auth required.
     * Declared before /{id} to prevent Spring treating "total-volume" as a path variable.
     */
    @GetMapping("/total-volume")
    public ResponseEntity<Long> getTotalVolume() {
        return ResponseEntity.ok(cropListingService.getTotalActiveVolume());
    }

    /**
     * GET /listings/{id}
     * Single listing — public.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CropListing> getListing(@PathVariable UUID id) {
        return cropListingService.getListing(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /listings/{id}/status
     * Market Officer approves or rejects a listing.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('MARKET_OFFICER')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        ListingStatus status = parseEnum(ListingStatus.class, body.get("status"), "status");
        String reason = body.get("rejectionReason");
        cropListingService.updateListingStatus(id, status, reason);
        return ResponseEntity.noContent().build();
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleStateError(IllegalStateException ex) {
        return ResponseEntity.status(422).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleClientError(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Field '" + field + "' is required.");
        }
        try {
            return Enum.valueOf(cls, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value for '" + field + "': " + value);
        }
    }
}
