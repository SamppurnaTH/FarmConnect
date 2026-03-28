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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CropListingService {

    private final CropListingRepository cropListingRepository;
    private final RestTemplate restTemplate;

    @Value("${services.farmer.url:http://localhost:8082}")
    private String farmerServiceUrl;

    public CropListingService(CropListingRepository cropListingRepository, RestTemplateBuilder restTemplateBuilder) {
        this.cropListingRepository = cropListingRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Requirement 7.1: Farmer creates CropListing.
     * Verifies farmer is Active before permitting.
     */
    @Transactional
    public UUID createListing(ListingRequest request) {
        // Coordination with Farmer Service
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

    private void verifyFarmerIsActive(UUID farmerId) {
        try {
            String status = restTemplate.getForObject(
                    farmerServiceUrl + "/farmers/" + farmerId + "/status", String.class);
            // Response is a JSON string like "Active" — strip quotes if present
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

    /**
     * Requirement 8.1: Search listings.
     */
    public List<CropListing> searchListings(String location) {
        if (location != null && !location.isBlank()) {
            return cropListingRepository.findByStatusAndLocationContainingIgnoreCase(ListingStatus.Active, location);
        }
        return cropListingRepository.findByStatus(ListingStatus.Active);
    }

    /**
     * Requirement 7.3: Market Officer approval/rejection.
     */
    @Transactional
    public void updateListingStatus(UUID id, ListingStatus status, String reason) {
        CropListing listing = cropListingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + id));
        
        listing.setStatus(status);
        listing.setRejectionReason(reason);
        cropListingRepository.save(listing);
    }

    public long getTotalActiveVolume() {
        return cropListingRepository.findAll().stream()
                .filter(l -> l.getStatus() == ListingStatus.Active)
                .mapToLong(l -> l.getQuantity().longValue())
                .sum();
    }
}
