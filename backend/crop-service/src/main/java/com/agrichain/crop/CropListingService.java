package com.agrichain.crop;

import com.agrichain.common.enums.ListingStatus;
import com.agrichain.crop.dto.ListingRequest;
import com.agrichain.crop.entity.CropListing;
import com.agrichain.crop.repository.CropListingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CropListingService {

    private final CropListingRepository cropListingRepository;
    private final RestTemplate restTemplate;

    @Value("${services.farmer.url:http://localhost:8082}")
    private String farmerServiceUrl;

    @Value("${services.notification.url:http://localhost:8088}")
    private String notificationServiceUrl;

    public CropListingService(CropListingRepository cropListingRepository,
                              RestTemplateBuilder restTemplateBuilder) {
        this.cropListingRepository = cropListingRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Requirement 7.1: Farmer creates CropListing.
     * Verifies farmer is Active before permitting.
     */
    @Transactional
    public UUID createListing(ListingRequest request) {
        verifyFarmerIsActive(request.getFarmerId());

        CropListing listing = new CropListing();
        listing.setFarmerId(request.getFarmerId());
        listing.setCropType(request.getCropType());
        listing.setQuantity(request.getQuantity());
        listing.setPricePerUnit(request.getPricePerUnit());
        listing.setLocation(request.getLocation());
        listing.setStatus(ListingStatus.Pending_Approval);

        return cropListingRepository.save(listing).getId();
    }

    public java.util.Optional<CropListing> getListing(UUID id) {
        return cropListingRepository.findById(id);
    }

    /**
     * Requirement 8.1: Search/browse listings.
     *
     * - pendingOnly=true: returns all Pending_Approval listings (Market Officer queue)
     * - farmerId param: returns ALL listings for that farmer (all statuses) — for the farmer's own view.
     * - location param: returns Active listings matching location — for trader browse.
     * - no params: returns all Active listings.
     */
    public List<CropListing> searchListings(String location, UUID farmerId, boolean pendingOnly) {
        if (pendingOnly) {
            return cropListingRepository.findByStatus(ListingStatus.Pending_Approval);
        }
        if (farmerId != null) {
            // Farmer viewing their own listings — show all statuses
            return cropListingRepository.findByFarmerId(farmerId);
        }
        if (location != null && !location.isBlank()) {
            return cropListingRepository.findByStatusAndLocationContainingIgnoreCase(ListingStatus.Active, location);
        }
        return cropListingRepository.findByStatus(ListingStatus.Active);
    }

    /**
     * Requirement 7.3: Market Officer approval/rejection.
     * Notifies the farmer of the outcome.
     */
    @Transactional
    public void updateListingStatus(UUID id, ListingStatus status, String reason) {
        CropListing listing = cropListingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + id));
        listing.setStatus(status);
        listing.setRejectionReason(reason);
        cropListingRepository.save(listing);
        notifyListingStatusChange(listing, reason);
    }

    /**
     * Uses a SUM aggregate query — does NOT load all rows into memory.
     */
    public long getTotalActiveVolume() {
        BigDecimal sum = cropListingRepository.sumActiveQuantity();
        return sum != null ? sum.longValue() : 0L;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void notifyListingStatusChange(CropListing listing, String reason) {
        String message = switch (listing.getStatus()) {
            case Active -> "Your crop listing for '" + listing.getCropType()
                    + "' has been approved and is now visible to traders.";
            case Rejected -> "Your crop listing for '" + listing.getCropType()
                    + "' was rejected."
                    + (reason != null && !reason.isBlank() ? " Reason: " + reason : "");
            default -> "Your listing status has been updated to " + listing.getStatus().name();
        };
        sendNotification(listing.getFarmerId(), message);
    }

    private void sendNotification(UUID userId, String message) {
        Map<String, Object> request = Map.of(
                "userId",  userId,
                "channel", "In_App",
                "content", message
        );
        try {
            restTemplate.postForObject(notificationServiceUrl + "/notifications", request, Void.class);
        } catch (Exception e) {
            System.err.println("[notification] Failed to send notification: " + e.getMessage());
        }
    }

    private void verifyFarmerIsActive(UUID farmerId) {
        try {
            String status = restTemplate.getForObject(
                    farmerServiceUrl + "/farmers/" + farmerId + "/status", String.class);
            if (status != null) status = status.replace("\"", "").trim();
            if (!"Active".equalsIgnoreCase(status)) {
                throw new IllegalStateException("Only active and verified farmers can list crops.");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify farmer status: " + e.getMessage(), e);
        }
    }
}
