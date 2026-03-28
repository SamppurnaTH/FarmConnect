package com.agrichain.crop;

import com.agrichain.crop.dto.ListingRequest;
import com.agrichain.crop.entity.CropListing;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
     * Farmer creates a listing.
     */
    @PostMapping
    public ResponseEntity<UUID> createListing(@Valid @RequestBody ListingRequest request) {
        UUID id = cropListingService.createListing(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /listings
     * Search listings by location.
     */
    @GetMapping
    public ResponseEntity<List<CropListing>> searchListings(@RequestParam(required = false) String location) {
        return ResponseEntity.ok(cropListingService.searchListings(location));
    }

    /**
     * PUT /listings/{id}/status
     * Market Officer approval/rejection.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusRequest) {
        com.agrichain.common.enums.ListingStatus status = com.agrichain.common.enums.ListingStatus.valueOf(
                statusRequest.get("status"));
        String reason = statusRequest.get("rejectionReason");
        cropListingService.updateListingStatus(id, status, reason);
        return ResponseEntity.noContent().build();
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleStateError(IllegalStateException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleClientError(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }

    @GetMapping("/total-volume")
    public ResponseEntity<Long> getTotalVolume() {
        return ResponseEntity.ok(cropListingService.getTotalActiveVolume());
    }
}
